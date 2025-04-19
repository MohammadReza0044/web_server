import java.io.*;
import java.util.*;

public class IsolatedFormSaver {
    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            Map<String, String> data = new LinkedHashMap<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    data.put(parts[0], parts[1]);
                }
            }

            File dir = new File("submissions");
            if (!dir.exists()) dir.mkdir();

            File file = new File(dir, "submission_" + System.currentTimeMillis() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue());
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            System.err.println("IsolatedFormSaver error: " + e.getMessage());
        }
    }
}
