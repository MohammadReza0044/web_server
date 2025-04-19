import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        File dir = new File("submissions");
        if (!dir.exists()) {
            dir.mkdir();
            dir.setReadable(false, false);   // no one can read
            dir.setExecutable(false, false); // no one can list
            dir.setWritable(true, true);     // only server user can write
        }

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
