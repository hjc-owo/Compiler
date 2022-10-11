package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.StringJoiner;

public class IOUtils {
    /**
     * read from testfile.txt
     */
    public static String read(String filename) throws IOException {
        InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(filename)));
        Scanner scanner = new Scanner(in);
        StringJoiner stringJoiner = new StringJoiner("\n");
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        in.close();
        return stringJoiner.toString();
    }

    /**
     * output to output.txt
     */
    public static void write(String content) {
        File outputFile = new File("output.txt");
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            writer.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write(String content, String filename) {
        File outputFile = new File(filename);
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            writer.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void error(String message) {
        File outputFile = new File("error.txt");
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            writer.write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
    }
}
