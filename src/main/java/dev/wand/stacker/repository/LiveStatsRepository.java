package dev.wand.stacker.repository;

import dev.wand.stacker.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists live-stats embed locations (channelId / messageId) to the
 * {@code live_stats_embeds} PostgreSQL table so they survive bot restarts.
 *
 * <p>All methods obtain a connection from {@link Database#getConnection()} and
 * release it immediately after use via try-with-resources.</p>
 */
public final class LiveStatsRepository {

    private static final Logger logger = LoggerFactory.getLogger(LiveStatsRepository.class);

    private LiveStatsRepository() {
    }

    /**
     * Insert a live-stats embed location.
     * Silently ignores duplicate entries (ON CONFLICT DO NOTHING).
     *
     * @param channelId the Discord channel ID
     * @param messageId the Discord message ID
     * @throws SQLException if the database operation fails
     */
    public static void add(String channelId, String messageId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO live_stats_embeds (channel_id, message_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            ps.setString(1, channelId);
            ps.setString(2, messageId);
            ps.executeUpdate();
            logger.debug("LiveStatsRepository: added {}/{}", channelId, messageId);
        }
    }

    /**
     * Remove a live-stats embed location.
     *
     * @param channelId the Discord channel ID
     * @param messageId the Discord message ID
     * @throws SQLException if the database operation fails
     */
    public static void remove(String channelId, String messageId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM live_stats_embeds WHERE channel_id = ? AND message_id = ?")) {
            ps.setString(1, channelId);
            ps.setString(2, messageId);
            ps.executeUpdate();
            logger.debug("LiveStatsRepository: removed {}/{}", channelId, messageId);
        }
    }

    /**
     * Read all stored embed locations.
     *
     * @return a set of {@code [channelId, messageId]} pairs
     * @throws SQLException if the database operation fails
     */
    public static Set<String[]> readAll() throws SQLException {
        Set<String[]> results = new HashSet<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT channel_id, message_id FROM live_stats_embeds");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(new String[]{rs.getString("channel_id"), rs.getString("message_id")});
            }
        }
        return results;
    }
}
