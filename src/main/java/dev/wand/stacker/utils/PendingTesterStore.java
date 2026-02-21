package dev.wand.stacker.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class PendingTesterStore {

    private static final Path STORE_PATH = Paths.get("pending_testers.txt");

    public static synchronized void addPendingTester(String userId) throws IOException {
        Set<String> ids = readAll();
        ids.add(userId);
        writeAll(ids);
    }

    public static synchronized void removePendingTester(String userId) throws IOException {
        Set<String> ids = readAll();
        ids.remove(userId);
        writeAll(ids);
    }

    public static synchronized boolean isPendingTester(String userId) throws IOException {
        return readAll().contains(userId);
    }

    private static Set<String> readAll() throws IOException {
        try {
            Set<String> ids = new HashSet<>();
            for (String line : Files.readAllLines(STORE_PATH)) {
                if (!line.isBlank()) {
                    ids.add(line.trim());
                }
            }
            return ids;
        } catch (NoSuchFileException e) {
            return new HashSet<>();
        }
    }

    private static void writeAll(Set<String> ids) throws IOException {
        Files.write(STORE_PATH, ids, CREATE, TRUNCATE_EXISTING);
    }

    private PendingTesterStore() {
        // Utility class, prevent instantiation
    }
}
