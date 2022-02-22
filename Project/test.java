import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class test {
  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(8888)) {
      while (true) {
        try (Socket client = serverSocket.accept()) {
          System.out.println("Client connected!");
          clientHandler(client);
        }
      }
    } catch (Exception e) {
      System.out.println("Could not open port 8888!");
    }
  }

  private static void clientHandler(Socket client) throws Exception {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    InputStream inputStream = client.getInputStream();
    byte[] buffer = new byte[1024];
    for (int length; (length = inputStream.read(buffer)) != -1;) {
        result.write(buffer, 0, length);
    }
    System.out.println(result.toString("UTF-8"));
  }
}
