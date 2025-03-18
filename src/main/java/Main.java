import java.io.*;
import java.net.*;

public class Main {   
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server is running...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new Connection");
                new Thread(new HandleClient(clientSocket)).start();  // 🚀 Run in a new thread!
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }     
    }
}

class HandleClient implements Runnable {
    private final Socket clientSocket;

    public HandleClient(Socket clientSocket) {
        this.clientSocket = clientSocket;
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

            // Read and discard remaining headers
            String header;
            header = reader.readLine();
            header = reader.readLine();
            System.out.println("Header: " + header);

            // Parse the request
            String[] parts = requestLine.split(" ");

            String path = parts[1];
            String response;

            if ("/".equals(path)) {
                response = "HTTP/1.1 200 OK\r\n" +
                           "Content-Length: 0\r\n" +
                           "Connection: close\r\n\r\n";
            } else if (path.startsWith("/user-agent")) {
                response = String.format(
                    "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n%s\r\n",
                            header.length(), header);
            } else if (path.startsWith("/echo/")) {
                String echoMessage = path.substring(6);
                response = "HTTP/1.1 200 OK\r\n" +
                           "Content-Type: text/plain\r\n" +
                           "Content-Length: " + echoMessage.length() + "\r\n" +
                           "Connection: close\r\n\r\n" +
                           echoMessage;
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
