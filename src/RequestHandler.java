import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        if (requestLine == null || requestLine.isEmpty()) {
            Logger.log(Logger.Level.WARN, "Empty request received from " + socket.getInetAddress());
            return;
        }

        Logger.log(Logger.Level.INFO, "Request from " + socket.getInetAddress() + " - " + requestLine);

        String[] tokens = requestLine.split(" ");

        if (tokens.length < 2) {
            Logger.log(Logger.Level.WARN, "Malformed HTTP request from " + socket.getInetAddress() + ": " + requestLine);
            sendResponse(out, 400, "Bad Request", "text/html", "<h1>400 Bad Request</h1><p>Malformed request.</p>");
            return;
        }

        String method = tokens[0];
        String path = tokens[1];

        Logger.log(Logger.Level.DEBUG, "Parsed method: " + method + ", path: " + path);

        switch (method) {
            case "GET" -> handleGet(path, out);
            case "POST" -> handlePost(path, in, out);
            default -> {
                Logger.log(Logger.Level.WARN, "Unsupported HTTP method from " + socket.getInetAddress() + ": " + method);
                sendResponse(out, 501, "Not Implemented", "text/html", "<h1>501 Not Implemented</h1><p>Unsupported method.</p>");
            }
        }

    } catch (IOException e) {
        Logger.log(Logger.Level.ERROR, "IOException from " + socket.getInetAddress() + ": " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
        Logger.log(Logger.Level.ERROR, "Unexpected error from " + socket.getInetAddress() + ": " + e.getMessage());
        e.printStackTrace();
    }
}

private String getContentType(String path) {
    path = path.toLowerCase(); // Make it case-insensitive

    if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
    if (path.endsWith(".css")) return "text/css";
    if (path.endsWith(".js")) return "application/javascript";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
    if (path.endsWith(".gif")) return "image/gif";
    if (path.endsWith(".svg")) return "image/svg+xml";
    if (path.endsWith(".ico")) return "image/x-icon";
    if (path.endsWith(".pdf")) return "application/pdf";
    if (path.endsWith(".json")) return "application/json";
    if (path.endsWith(".txt")) return "text/plain";

    Logger.log(Logger.Level.WARN, "Unknown file type requested: " + path);
    return "application/octet-stream"; // default binary
    }

    private void handleGet(String path, OutputStream out) {
        try {
            if (path.equals("/")) {
                path = "/index.html"; // default file
            }
    
            // Prevent directory traversal
            if (path.contains("..")) {
                Logger.log(Logger.Level.WARN, "Blocked path traversal attempt: " + path);
                sendResponse(out, 403, "Forbidden", "text/html", "<h1>403 Forbidden</h1><p>Access denied.</p>");
                return;
            }
    
            File file = new File(WEB_ROOT, path); // Safer way to join paths
    
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                byte[] content = Files.readAllBytes(file.toPath());
    
                String header = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + contentType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n\r\n";
    
                out.write(header.getBytes());
                out.write(content);
    
                Logger.log(Logger.Level.INFO, "Served file: " + file.getPath() + " (" + contentType + ")");
            } else {
                String body = "<h1>404 Not Found</h1>";
                String response = "HTTP/1.1 404 Not Found\r\n" +
                                  "Content-Type: text/html\r\n" +
                                  "Content-Length: " + body.length() + "\r\n" +
                                  "Connection: close\r\n\r\n" +
                                  body;
    
                out.write(response.getBytes());
                Logger.log(Logger.Level.WARN, "File not found: " + file.getPath());
            }
    
            out.flush();
    
        } catch (IOException e) {
            Logger.log(Logger.Level.ERROR, "IOException in handleGet: " + e.getMessage());
            sendResponse(out, 500, "Internal Server Error", "text/html", "<h1>500 Internal Server Error</h1>");    
        }
    }
    
    
    private void handlePost(String path, BufferedReader in, OutputStream out) {
        try {
            Logger.log(Logger.Level.INFO, "Handling POST request to: " + path);
    
            // Step 1: Read headers
            String line;
            int contentLength = -1;
    
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (NumberFormatException e) {
                        Logger.log(Logger.Level.WARN, "Invalid Content-Length header.");
                        sendResponse(out, 411, "Length Required", "text/html", "<h1>411 Length Required</h1>");
                        return;
                    }
                }
            }
    
            if (contentLength < 0) {
                Logger.log(Logger.Level.WARN, "No Content-Length header found.");
                sendResponse(out, 411, "Length Required", "text/html", "<h1>411 Length Required</h1>");
                return;
            }
    
            // Step 2: Read body
            char[] bodyChars = new char[contentLength];
            int read = in.read(bodyChars, 0, contentLength);
    
            if (read < contentLength) {
                Logger.log(Logger.Level.WARN, "Incomplete body read: expected " + contentLength + ", got " + read);
                sendResponse(out, 400, "Bad Request", "text/html", "<h1>400 Bad Request</h1><p>Incomplete request body.</p>");
                return;
            }
    
            String requestBody = new String(bodyChars);
            Logger.log(Logger.Level.DEBUG, "Raw form data: " + requestBody);
    
            // Step 3: Parse and validate
            Map<String, String> rawParams = FormHandler.parseFormData(requestBody);
            Map<String, String> cleanParams = new LinkedHashMap<>();
    
            Logger.log(Logger.Level.DEBUG, "🔍 Validating form data...");
    
            for (Map.Entry<String, String> entry : rawParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
    
                Logger.log(Logger.Level.DEBUG, "👉 Field: " + key + ", Length: " + value.length());
    
                if (!FormHandler.isValidFormData(key, value)) {
                    Logger.log(Logger.Level.WARN, "❌ Rejected input: " + key + "=" + value);
                    sendResponse(out, 400, "Bad Request", "text/html", "<h1>400 Bad Request</h1><p>Invalid input detected.</p>");
                    return;
                }
    
                cleanParams.put(FormHandler.sanitize(key), FormHandler.sanitize(value));
            }
    
            // Step 4: Save via isolated process
            FormHandler.saveToFile(cleanParams);
            Logger.log(Logger.Level.INFO, "Form data saved successfully.");
    
            // Step 5: Prepare and send response
            StringBuilder responseHtml = new StringBuilder("<html><body><h1>Form Submission Received</h1><ul>");
            for (Map.Entry<String, String> entry : cleanParams.entrySet()) {
                responseHtml.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
            }
            responseHtml.append("</ul></body></html>");
    
            sendResponse(out, 200, "OK", "text/html", responseHtml.toString());
    
        } catch (IOException e) {
            Logger.log(Logger.Level.ERROR, "IOException in handlePost: " + e.getMessage());
            sendResponse(out, 500, "Internal Server Error", "text/html", "<h1>500 Internal Server Error</h1>");

        } catch (Exception e) {
            Logger.log(Logger.Level.ERROR, "Unexpected error in handlePost: " + e.getMessage());
            sendResponse(out, 500, "Internal Server Error", "text/html", "<h1>500 Internal Server Error</h1>");

        }
    }
    
    


    private void sendResponse(OutputStream out, int statusCode, String statusText, String contentType, String body) {
        try {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    
            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                              "Content-Type: " + contentType + "\r\n" +
                              "Content-Length: " + bodyBytes.length + "\r\n" +
                              "Connection: close\r\n\r\n";
    
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();
    
            Logger.log(Logger.Level.INFO, "Response sent: " + statusCode + " " + statusText + 
                       " (" + contentType + ", " + bodyBytes.length + " bytes)");
    
        } catch (IOException e) {
            Logger.log(Logger.Level.ERROR, "Failed to send HTTP response: " + e.getMessage());
        }
    }
    


      
}
