import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
  
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      
      System.out.println("accepted new connection");
      clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());

      serverSocket.close();
      clientSocket.close();
    
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
