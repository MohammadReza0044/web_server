import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;


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
            Logger.log(Logger.Level.INFO, "Request from " + socket.getInetAddress() + " - " + requestLine);


            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String path = tokens[1];

            if (method.equals("GET")) {
                handleGet(path, out);
            } else if (method.equals("POST")) {
                handlePost(path, in, out); 
            } else {
                Logger.log(Logger.Level.WARN, "Unsupported HTTP method from " + socket.getInetAddress() + ": " + method);
                sendResponse(out, 501, "Not Implemented", "text/html", "Method Not Implemented");
            }
            

        } catch (IOException e) {
            Logger.log(Logger.Level.ERROR, "IOException occurred: " + e.getMessage());
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
    
    private void handlePost(String path, BufferedReader in, OutputStream out) throws IOException {
    // Step 1: Read headers
    String line;
    int contentLength = 0;
    while ((line = in.readLine()) != null && !line.isEmpty()) {
        if (line.startsWith("Content-Length:")) {
            contentLength = Integer.parseInt(line.substring(15).trim());
        }
    }

    // Step 2: Read body
    char[] bodyChars = new char[contentLength];
    in.read(bodyChars, 0, contentLength);
    String requestBody = new String(bodyChars);

    Map<String, String> rawParams = FormHandler.parseFormData(requestBody);
    Map<String, String> cleanParams = new LinkedHashMap<>();

    System.out.println("🔍 Validating form data...");
    for (Map.Entry<String, String> entry : rawParams.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        System.out.println("👉 Field: " + key + ", Length: " + value.length());

        if (!FormHandler.isValidFormData(key, value)) {
            System.out.println("❌ Rejected input: " + key + "=" + value);
            sendResponse(out, 400, "Bad Request", "text/html", "<h1>400 Bad Request</h1><p>Invalid input detected.</p>");
            return;
        }
        cleanParams.put(FormHandler.sanitize(key), FormHandler.sanitize(value));
    }


    FormHandler.saveToFile(cleanParams);


    // Step 3: Prepare and send response
    StringBuilder responseHtml = new StringBuilder("<html><body><h1>Form Submission Received</h1><ul>");
    for (Map.Entry<String, String> entry : cleanParams.entrySet()) {
        responseHtml.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
    }
    responseHtml.append("</ul></body></html>");

    sendResponse(out, 200, "OK", "text/html", responseHtml.toString());
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
