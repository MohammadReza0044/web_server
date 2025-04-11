import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
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

            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String path = tokens[1];

            if (method.equals("GET")) {
                handleGet(path, out);
            } else if (method.equals("POST")) {
                handlePost(path, in, out); 
            } else {
                sendResponse(out, 501, "Not Implemented", "text/html", "Method Not Implemented");
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

    // Step 3: Parse form data
    Map<String, String> rawParams = parseFormData(requestBody);
    Map<String, String> cleanParams = new LinkedHashMap<>();

    // Step 4: Validate and sanitize
    for (Map.Entry<String, String> entry : rawParams.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        if (!isValidFormData(key, value)) {
            sendResponse(out, 400, "Bad Request", "text/html", "<h1>400 Bad Request</h1><p>Invalid input detected.</p>");
            return;
        }

        // Sanitize only after validation passes
        cleanParams.put(sanitize(key), sanitize(value));
    }

    // Step 5: Save to file
    saveSubmissionToFile(cleanParams);

    // Step 6: Prepare and send response
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

    private String sanitize(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
    
    private boolean isValidFormData(String key, String value) {
        return key != null && value != null &&
               !key.trim().isEmpty() && !value.trim().isEmpty() &&
               key.length() < 100 && value.length() < 100 &&
               !key.contains("<") && !value.contains("<") &&
               !key.contains(">") && !value.contains(">");
    }
    
    
    private Map<String, String> parseFormData(String data) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = data.split("&");
    
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2); // limit = 2 in case value contains '='
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
    
        return result;
    }
    

    private void saveSubmissionToFile(Map<String, String> data) {
        // Ensure the submissions directory exists
        File dir = new File("submissions");
        if (!dir.exists()) {
            dir.mkdir();
        }
    
        // Create a unique filename (e.g., based on timestamp)
        String filename = "submissions/submission_" + System.currentTimeMillis() + ".txt";
        File file = new File(filename);
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error saving submission: " + e.getMessage());
        }
    }
    
    
}
