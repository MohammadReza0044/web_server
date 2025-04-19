import java.io.*;
import java.time.LocalDateTime;

public class Logger {

    private static final File logDir = new File("logs");

    static {
        if (!logDir.exists()) logDir.mkdir();
    }

    public static void log(String type, String message) {
        String filename = type.equals("suspicious") ? "suspicious.log" : "server.log";
        File file = new File("logs", filename);
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write("[" + LocalDateTime.now() + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
    
}
