import java.io.*;
import java.net.*;

public class Main {   
    private static String directory = ".";
    public static void main(String[] args) {
        if (args.length == 2 && "--directory".equals(args[0])) {
            directory = args[1];
        }
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new Connection");
                new Thread(new HandleClient(clientSocket, directory)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }     
    }
}

class HandleClient implements Runnable {
    private final Socket clientSocket;
    private final String directory;

    public HandleClient(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            // Read the request line
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                System.out.println("Invalid request received.");
                return;
            }

            // Read headers dynamically
            String userAgent = "Unknown";  // Default value
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring(12).trim();  // Extract user-agent value
                }
            }

            System.out.println("Request: " + requestLine);
            System.out.println("User-Agent: " + userAgent);

            // Parse request
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                System.out.println("Malformed request.");
                return;
            }

            String path = parts[1];
            String response;

            if ("/".equals(path)) {
                response = "HTTP/1.1 200 OK\r\n" +
                           "Content-Length: 0\r\n" +
                           "Connection: close\r\n\r\n";
            } else if (path.startsWith("/user-agent")) {
                response = String.format(
                    "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\nConnection: close\r\n\r\n%s",
                    userAgent.length(), userAgent);
            } else if (path.startsWith("/echo/")) {
                String echoMessage = path.substring(6);
                response = "HTTP/1.1 200 OK\r\n" +
                           "Content-Type: text/plain\r\n" +
                           "Content-Length: " + echoMessage.length() + "\r\n" +
                           "Connection: close\r\n\r\n" +
                           echoMessage;
            } else if (path.startsWith("/files/")) {
                String filename = path.substring(7);  // Extract filename
                File file = new File(directory, filename);

                if (file.exists() && file.isFile()) {
                    byte[] fileData = readFile(file);
                    response = "HTTP/1.1 200 OK\r\n" +
                               "Content-Type: application/octet-stream\r\n" +
                               "Content-Length: " + fileData.length + "\r\n" +
                               "Connection: close\r\n\r\n";
                    outputStream.write(response.getBytes());
                    outputStream.write(fileData);  // Send file content
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                    outputStream.write(response.getBytes());
                }
            } else {
                response = "HTTP/1.1 404 Not Found\r\n" +
                           "Content-Length: 0\r\n" +
                           "Connection: close\r\n\r\n";
            }

            // Send the response
            outputStream.write(response.getBytes());
            outputStream.flush();

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
