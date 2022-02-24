import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * A server that handles POST and GET requests.
 */
public class WebServer {
  /**
   * The main method that takes port and top directory.
   */
  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]))) {
      while (true) {
        try (Socket client = serverSocket.accept()) {
          System.out.println("Connected to client! Port: " + args[0] 
              + " Top directory: " + args[1]);
          clientHandler(client, args[1]);
        }
      }
    } catch (NumberFormatException e) {
      System.out.println("The port number needs to consist of only integers..."); 
    } catch (IOException e) {
      System.out.println("Could not open port 8888!");
    } catch (IllegalArgumentException e) {
      System.out.println("The passed arguments need to be strings!");
    }
  }

  /**
   * Handles the client input.
   */
  private static void clientHandler(Socket client, String directory) {
    try {
      BufferedReader input = new BufferedReader(
          new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
      
      // Parse for and handle the different requests.
      StringBuilder requestBuilder = new StringBuilder();
      String line;
      while (!(line = input.readLine()).isEmpty()) {
        requestBuilder.append(line + "\r\n");
        System.out.println(line);
      }
      String topDirectory = "./" + directory;
      String request = requestBuilder.toString();

      String[] a = request.split("\r\n")[0].split(" ");
      
      if (request.startsWith("GET")) {
        Path path = checkForDirectory(Paths.get(topDirectory, a[1]));

        handleGet(path, client, topDirectory);    

      } else if (request.startsWith("POST")) {
        handlePost(topDirectory, client, input);
        
      } else {
        status500(client);
      }      
    } catch (IOException e) {
      System.out.println("Something went wrong when reading request.");
    }
  }

  /**
   * Handles post requests.
   */
  public static void handlePost(String topDirectory, Socket client, BufferedReader input) {
    try {

      StringBuilder bodyBuilder = new StringBuilder();
      while (input.ready()) {
        bodyBuilder.append((char) input.read());
      }
      String body = bodyBuilder.toString();

      String[] b = body.split("\r\n\r\n");
      String fileName = b[0].split(";")[2].split("\"")[1];
      String type = fileName.split("\\.")[1];
      if (!b[1].isEmpty()) {
        imageHandler(b[1], fileName, topDirectory, type, client);
      } else {
        status500(client);
      }

      
    } catch (IOException e) {
      System.out.println("Something went wrong with the POST request.");
    } catch (IndexOutOfBoundsException e) {
      System.out.println("Can't read the request content.");
    }
  }

  /**
   * Handles the formating and storing of an image locally.
   */
  private static void imageHandler(String body, String fileName,
      String topDirectory, String type, Socket client) {
    byte[] bytes;
    try {
      bytes = body.getBytes(StandardCharsets.ISO_8859_1);
      ByteArrayInputStream bodyBytes = new ByteArrayInputStream(bytes);
  
      BufferedImage img = ImageIO.read(bodyBytes);
      if (img != null) {
        ImageIO.write(img, type, new File(topDirectory + "/img/" + fileName));
      } else {
        status500(client);
      }
      System.out.println("Image saved locally!");
    } catch (UnsupportedEncodingException e) {
      System.out.println("Something went wrong with the encoding.");
    } catch (IOException e) {
      System.out.println("Something went wrong when converting to an image.");
    }
    
  }

  /**
   * Handles the GET request from a client.
   */
  private static void handleGet(Path path, Socket client, String topDirectory) {
    if (Files.exists(path)) {
      String status = "200 OK";
      System.out.println("Requested file found!");
      if (path.equals(Path.of(topDirectory + "/a/b/b.html"))) {
        path = Paths.get(topDirectory + "/a/b/c.html");
        status = "302 Found";
        System.out.println("Requested file not found.");
      }
      try {
        String fileType = Files.probeContentType(path);
        responseHandler(Files.readAllBytes(path), client, fileType, status);
      } catch (IOException e) {
        System.out.println("Something went wrong with the path.");
      }
    } else {
      byte[] notFound = "<h1><b>Page not found, idiot!</b></h1>".getBytes();
      responseHandler(notFound, client, "text/html", "404 Not Found");
    }
  }

  /**
   * Check if a given path points to a directory. If true,
   * then redirect to the index file.
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

      System.out.println("Response succesfully sent!");

    } catch (Exception e) {
      System.out.println("Response failed.");
    }
  }
  
  /**
   * Status code: 500 response.
   */
  public static void status500(Socket client) {
    String status = "500 Internal Server Error";
    byte[] respone = "<h1><b>Ohh nooo, you suck!</b></h1>".getBytes();
    responseHandler(respone, client, "text/html", status);
    System.out.println("500 error triggerd.");
  }
}