package dev.wand.stacker.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Persists live-stats embed locations (channelId:messageId) to {@code live_stats.txt}
 * so they can be resumed after a bot restart.
 */
public class LiveStatsStore {

    private static final Path STORE_PATH = Paths.get("live_stats.txt");

    public static synchronized void add(String channelId, String messageId) throws IOException {
        Set<String> entries = readRaw();
        entries.add(channelId + ":" + messageId);
        writeAll(entries);
    }

    public static synchronized void remove(String channelId, String messageId) throws IOException {
        Set<String> entries = readRaw();
        entries.remove(channelId + ":" + messageId);
        writeAll(entries);
    }

    /**
     * Read all stored entries.
     *
     * @return A set of {@code [channelId, messageId]} pairs
     */
    public static synchronized Set<String[]> readAll() throws IOException {
        Set<String[]> result = new HashSet<>();
        for (String raw : readRaw()) {
            String[] parts = raw.split(":", 2);
            if (parts.length == 2) {
                result.add(parts);
            }
        }
        return result;
    }

    private static Set<String> readRaw() throws IOException {
        try {
            Set<String> entries = new HashSet<>();
            for (String line : Files.readAllLines(STORE_PATH)) {
                if (!line.isBlank()) {
                    entries.add(line.trim());
                }
            }
            return entries;
        } catch (NoSuchFileException e) {
            return new HashSet<>();
        }
    }

    private static void writeAll(Set<String> entries) throws IOException {
        Files.write(STORE_PATH, entries, CREATE, TRUNCATE_EXISTING);
    }

    private LiveStatsStore() {}
}
