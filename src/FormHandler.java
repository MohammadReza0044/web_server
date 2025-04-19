import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FormHandler {

    public static Map<String, String> parseFormData(String data) {
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

    public static boolean isValidFormData(String key, String value) {
        key = key.trim();
        value = value.trim();
    
        // Reject if too long
        if (key.length() > 100 || value.length() > 100) {
            Logger.log(Logger.Level.WARN, "Unusual input detected: " + value);
            return false;
        }
    
        // Reject if empty
        if (key.isEmpty() || value.isEmpty()) {
            Logger.log(Logger.Level.WARN, "Unusual input detected: " + value);
            return false;
        }
    
        // Reject path traversal
        if (value.contains("..") || key.contains("..")) {
            Logger.log(Logger.Level.WARN, "Unusual input detected: " + value);
            return false;
        }
    
        // Reject if it looks like command injection or scripting
        String[] dangerousPatterns = {"<script>", "</script>", "javascript:", "$(", "`", ";", "|", "&", "&&", "||", "$"};
        for (String pattern : dangerousPatterns) {
            if (key.contains(pattern) || value.contains(pattern)) {
                Logger.log(Logger.Level.WARN, "Unusual input detected: " + value);
                return false;
            }
        }
    
        return true;
    }
    
    
    

    public static String sanitize(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    public static void saveToFile(Map<String, String> data) {
    try {
        // Start a new process to run IsolatedFormSaver
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", "out", "IsolatedFormSaver");
        Process process = pb.start();

        // Send form data to the subprocess through stdin
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {

            for (Map.Entry<String, String> entry : data.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            writer.flush();
        }

        // Wait for the process to finish
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            Logger.log(Logger.Level.ERROR, "IsolatedFormSaver exited with code " + exitCode);
        } else {
            Logger.log(Logger.Level.INFO, "Form data saved via IsolatedFormSaver.");
        }

    } catch (IOException | InterruptedException e) {
        Logger.log(Logger.Level.ERROR, "ProcessBuilder exception: " + e.getMessage());
    }
}

}
