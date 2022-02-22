import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

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
      BufferedReader input = new BufferedReader(
          new InputStreamReader(client.getInputStream(), "Cp852"));
      
      // Parse for and handle the different requests.
      StringBuilder requestBuilder = new StringBuilder();
      String line;
      while (!(line = input.readLine()).isEmpty()) {
        requestBuilder.append(line + "\r\n");
        System.out.println(line);
      }
      String topDirectory = "./public";
      String request = requestBuilder.toString();

      String[] a = request.split("\r\n")[0].split(" ");

      if (a[0].equals("GET")) {
        Path path = checkForDirectory(Paths.get(topDirectory, a[1]));
        handleGet(path, client);    

      } else if (a[0].equals("POST")) {
        StringBuilder bodyBuilder = new StringBuilder();
        while (input.ready()) {
          bodyBuilder.append((char) input.read());
        }
        String body = bodyBuilder.toString();
        String[] b = body.split("\r\n\r\n");
        String c = b[0].split(";")[2].split("\"")[1];
        handlePost(b[1], topDirectory, c);
        System.out.println("POST request!");
      } else {
        status500(client);
      }      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * .
   */
  public static void handlePost(String body, String topDirectory, String fileName) {
    try {

      File targetFile = new File(topDirectory + "/img/" + fileName);
      String type = fileName.split("\\.")[1];

      byte[] bytes = body.getBytes("Cp852");
      ByteArrayInputStream botty = new ByteArrayInputStream(bytes);
    
      BufferedImage img = ImageIO.read(botty);
      ImageIO.write(img, type, targetFile);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * .
   */
  private static void handleGet(Path path, Socket client) {
    if (Files.exists(path)) {
      String status = "200 OK";
      if (path.toString().equals("./public/a/b/b.html")) {
        path = Paths.get("./public/a/b/c.html");
        status = "302 Found";
      }
      try {
        String fileType = Files.probeContentType(path);
        responseHandler(Files.readAllBytes(path), client, fileType, status);
      } catch (IOException e) {
        System.out.println(e);
      }
    } else {
      byte[] notFound = "<h1><b>Page not found, idiot!</b></h1>".getBytes();
      responseHandler(notFound, client, "text/html", "404 Not Found");
    }
  }

  /**
   * .
   */
  public static Path checkForDirectory(Path path) {
    if (Files.isDirectory(path)) {
      path = Paths.get(path.toString(), "/index.html");
    }
    return path;
  }

  /**
   * General response handler with set headers.
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
    }
  }
  
  /**
   * Status code: 500 response.
   */
  public static void status500(Socket client) {
    String status = "500 Internal Server Error";
    byte[] respone = "<h1><b>Ohh nooo, you suck!</b></h1>".getBytes();
    responseHandler(respone, client, "text/html", status);
  }
}