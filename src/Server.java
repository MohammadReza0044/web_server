import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        int port = 8080;
    
        try {
            File wwwDir = new File("www");
            if (!wwwDir.exists()) {
                boolean created = wwwDir.mkdir();
                if (created) {
                    Logger.log(Logger.Level.INFO, "'www/' directory created.");
                } else {
                    Logger.log(Logger.Level.WARN, "Failed to create 'www/' directory.");
                }
            }
    
            // Set permissions on www/
            try {
                wwwDir.setReadable(true, true);
                wwwDir.setWritable(false, false); // No write permissions for anyone
                Logger.log(Logger.Level.INFO, "'www/' directory permissions updated (read-only).");
            } catch (SecurityException e) {
                Logger.log(Logger.Level.WARN, "Failed to set permissions on 'www/' directory: " + e.getMessage());
            }
    
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                Logger.log(Logger.Level.INFO, "Server started on port " + port);
    
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Logger.log(Logger.Level.DEBUG, "Accepted connection from " + clientSocket.getInetAddress());
                        new Thread(new RequestHandler(clientSocket)).start();
                    } catch (IOException e) {
                        Logger.log(Logger.Level.WARN, "Failed to accept connection: " + e.getMessage());
                    }
                }
    
            } catch (IOException e) {
                Logger.log(Logger.Level.ERROR, "Could not start server on port " + port + ": " + e.getMessage());
            }
    
        } catch (Exception e) {
            Logger.log(Logger.Level.ERROR, "Unexpected error in main(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
