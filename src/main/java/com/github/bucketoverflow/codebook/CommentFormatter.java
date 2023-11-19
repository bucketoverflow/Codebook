package com.github.bucketoverflow.codebook;

import java.io.*;

public class CommentFormatter {

    public static void formatCommentsInFile(String filePath) {
        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(file +"_formatted"));

            String line;
            while ((line = reader.readLine()) != null) {
                String formattedLine = formatLine(line);
                writer.write(formattedLine);
                writer.newLine();
            }

            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatLine(String line) {
        int commentIndex = line.indexOf("/*");
        if (commentIndex != -1 && (commentIndex == 0 || line.charAt(commentIndex - 1) != '*')) {
            StringBuilder sb = new StringBuilder(line.substring(0, commentIndex));
            // Fill the space between the original text and the comment with spaces
            while (sb.length() < 80) {
                sb.append(' ');
            }
            sb.append(line.substring(commentIndex));
            return sb.toString();
        }
        return line;
    }
}
