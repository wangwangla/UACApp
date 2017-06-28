package UACApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TemplateSourceFilePreprocessor {

    private static final String NEW_LINE = System.getProperty("line.separator");

    public static String process(String fileName) {
        File inputFile = new File(fileName);
        if (! inputFile.isFile()) {
            return "";
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(NEW_LINE);
            }
            reader.close();
            // Replace the indicator for the dynamic generated area with %s,
            // So we can plug in our code with String.format().
            // An indicator is a comment line starts with a "// $" and ends with a "$".
            // e.g.: // $ insert statements here $
            return builder.toString().replaceAll("// \\$.*\\$", "%s");
        } catch (IOException e) {
            return "";
        }
    }
}
