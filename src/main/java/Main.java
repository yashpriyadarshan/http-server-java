import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class Main {   
    private static String directory;
    public static void main(String[] args) {

        if (args.length > 1 && args[0].equals("--directory")) {
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
    private String directory;

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
                String filename = path.substring(7);
                File file = new File(directory, filename);

                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                                fileContent.length + "\r\n\r\n" + new String(fileContent);
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
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
