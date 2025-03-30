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
            String userAgent = "Unknown"; // Default value
            boolean supportsGzip = false;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring(12).trim(); // Extract user-agent value
                } else if (line.startsWith("Accept-Encoding:")) {
                    String acceptEncoding = line.substring(16).trim();
                    supportsGzip = acceptEncoding.contains("gzip");
                }
            }

            StringBuffer bodyBuffer = new StringBuffer();
            while (reader.ready()) {
                bodyBuffer.append((char) reader.read());
            }
            String body = bodyBuffer.toString();

            String[] parts = requestLine.split(" ", 3);
            String httpMethod = parts[0];
            String requestTarget = parts[1];

            String response;

            if ("POST".equals(httpMethod)) {
                if (requestTarget.startsWith("/files/")) {
                    File file = new File(directory + requestTarget.substring(7));
                    if (file.createNewFile()) {
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(body);
                        fileWriter.close();
                    }
                    outputStream.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
                } else {
                    outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                }
                outputStream.flush();
                outputStream.close();
                return;
            }

            if ("/".equals(requestTarget)) {
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        (supportsGzip ? "Content-Encoding: gzip\r\n" : "") +
                        "\r\n";
            } else if (requestTarget.startsWith("/user-agent")) {
                response = String.format(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\nConnection: close\r\n%s\r\n%s",
                        userAgent.length(),
                        supportsGzip ? "Content-Encoding: gzip" : "",
                        userAgent);
            } else if (requestTarget.startsWith("/echo/")) {
                String echoMessage = requestTarget.substring(6);
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + echoMessage.length() + "\r\n" +
                        "Connection: close\r\n" +
                        (supportsGzip ? "Content-Encoding: gzip\r\n" : "") +
                        "\r\n" +
                        echoMessage;
            } else if (requestTarget.startsWith("/files/")) {
                String filename = requestTarget.substring(7);
                File file = new File(directory, filename);

                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "Content-Length: " + fileContent.length + "\r\n" +
                            (supportsGzip ? "Content-Encoding: gzip\r\n" : "") +
                            "\r\n" +
                            new String(fileContent);
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }

            } else {
                response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        (supportsGzip ? "Content-Encoding: gzip\r\n" : "") +
                        "\r\n";
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
