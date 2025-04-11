import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
                sendResponse(out, 501, "Not Implemented", "Method Not Implemented");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGet(String path, OutputStream out) throws IOException {
        if (path.equals("/")) path = "/index.html";
    
        // Normalize path to prevent access outside www
        Path filePath = Paths.get(WEB_ROOT, path).normalize();
        File file = filePath.toFile();
    
        if (file.exists() && file.isFile()) {
            byte[] content = Files.readAllBytes(file.toPath());
            String header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "\r\n";
            out.write(header.getBytes());
            out.write(content);
        } else {
            sendResponse(out, 404, "Not Found", "<h1>404 Not Found</h1>");
        }
    
        out.flush();
    }
    
    

    private void sendResponse(OutputStream out, int statusCode, String statusText, String body) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                          "Content-Type: text/html\r\n" +
                          "Content-Length: " + body.length() + "\r\n" +
                          "\r\n" +
                          body;
        out.write(response.getBytes());
    }
}
