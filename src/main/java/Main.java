import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }

    BufferedReader clientIn = null;
    OutputStream clientOut = null;
    String input = null;
    try {
      clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      clientOut = clientSocket.getOutputStream();

      input = clientIn.readLine();
      String get[] = input.split(" ", 0);

      if(get[1].equals("/")) {
        clientOut.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      } else {
        clientOut.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }
      
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
    }
  }
}
