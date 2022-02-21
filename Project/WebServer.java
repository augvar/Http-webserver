import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * .
 */
public class WebServer {
  /**
   * .
   */
  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(8888)) {
      while (true) {
        try (Socket client = serverSocket.accept()) {
          clientHandler(client);
        }
      }
    } catch (Exception e) {
      System.out.println("Could not open port 8888!");
    }
  }

  private static void clientHandler(Socket client) {
    try {
      BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
      
      String line;
      while (!(line = input.readLine()).isEmpty()) {
        String[] a = line.split("\r\n")[0].split(" ");
        if (a[0].equals("GET") || a[0].equals("POST")) {
        
          String topDirectory = "./public";

          Path path = Paths.get(topDirectory, a[1]);

          if (path.startsWith(topDirectory)) {
            if (Files.exists(path)) {
              String status = "200 OK";
              if (path.toString().equals("./public/a/b/b.html")) {
                path = Paths.get("./public/a/b/c.html");
                status = "302 Found";
              }
              String fileType = Files.probeContentType(path);
              responseHandler(Files.readAllBytes(path), client, fileType, status);
            } else {
              byte[] notFound = "<h1><b>Page not found, idiot!</b></h1>".getBytes();
              responseHandler(notFound, client, "text/html", "404 Not Found");
            }               
          }
                 
        }
        System.out.println(line);
      }      
      
    } catch (IOException e) {
      e.printStackTrace();
      status500(client);
    }
  }

  /**
   * .
   */
  public static Path checkPath(Path path) {
    if (Files.isDirectory(path)) {
      path = Paths.get(path.toString(), "/index.html");
    }
    return path;
  }

  /**
   * .
   */
  public static void responseHandler(byte[] respone, Socket client, String type, String status) {
    try {
      OutputStream output = client.getOutputStream();
      
      output.write(("HTTP/1.1 " + status + "\r\n").getBytes()); 
      output.write(("Content-Type: " + type + "\r\n").getBytes());
      output.write(("Content-Length: " + respone.length + "\r\n").getBytes());
      output.write("\r\n".getBytes());
      output.write(respone);
      output.write("\r\n\r\n".getBytes());
      output.flush();
      client.close();

    } catch (Exception e) {
      System.out.println("Nonono respone.");
      status500(client);
    }
  }
  
  /**
   * .
   */
  public static void status500(Socket client) {
    String status = "500 Internal Server Error";
    byte[] respone = "<h1><b>Ohh nooo, you suck!</b></h1>".getBytes();
    responseHandler(respone, client, "text/html", status);
  }
}