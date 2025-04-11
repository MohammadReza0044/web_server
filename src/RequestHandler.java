import java.io.*;
import java.net.Socket;
import java.nio.file.Files;


public class RequestHandler implements Runnable {
    private final Socket socket;
    private final static String WEB_ROOT = "www";

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Request: " + requestLine);

            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String path = tokens[1];

            if (method.equals("GET")) {
                handleGet(path, out);
            } else {
                sendResponse(out, 501, "Not Implemented", "text/html", "<h1>501 - Method Not Implemented</h1>");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        return "application/octet-stream"; // default binary
    }
    

    private void handleGet(String path, OutputStream out) throws IOException {
        if (path.equals("/")) {
            path = "/index.html";  // default file
        }
    
        File file = new File(WEB_ROOT + path);
        
        if (file.exists() && !file.isDirectory()) {
            String contentType = getContentType(path);
            byte[] content = Files.readAllBytes(file.toPath());
    
            String header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
    
            out.write(header.getBytes());
            out.write(content);
        } else {
            String body = "<h1>404 Not Found</h1>";
            String response = "HTTP/1.1 404 Not Found\r\n" +
                              "Content-Type: text/html\r\n" +
                              "Content-Length: " + body.length() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n" +
                              body;
            out.write(response.getBytes());
        }
    
        out.flush();
    }
    
    
    private void sendResponse(OutputStream out, int statusCode, String statusText, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
    
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                          "Content-Type: " + contentType + "\r\n" +
                          "Content-Length: " + bodyBytes.length + "\r\n" +
                          "Connection: close\r\n" +
                          "\r\n";
    
        out.write(response.getBytes("UTF-8"));
        out.write(bodyBytes);
        out.flush();
    }
    
}
