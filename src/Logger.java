import java.io.*;
import java.time.LocalDateTime;

public class Logger {

    private static final File logDir = new File("logs");

    static {
        if (!logDir.exists()) logDir.mkdir();
    }

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static void log(Level level, String message) {
        String filename = "server.log"; // We'll log all levels in a single file for now

        File file = new File(logDir, filename);
        String logEntry = "[" + LocalDateTime.now() + "] [" + level + "] " + message;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            System.err.println("Logging failed: " + e.getMessage());
        }

        // Optional: Also print to console (color-coded for extra flair)
        printToConsole(level, logEntry);
    }

    private static void printToConsole(Level level, String message) {
        String color;
        switch (level) {
            case DEBUG -> color = "\u001B[36m"; // Cyan
            case INFO -> color = "\u001B[32m";  // Green
            case WARN -> color = "\u001B[33m";  // Yellow
            case ERROR -> color = "\u001B[31m"; // Red
            default -> color = "\u001B[0m";     // Reset
        }
        System.out.println(color + message + "\u001B[0m"); // Reset color after log
    }
}
