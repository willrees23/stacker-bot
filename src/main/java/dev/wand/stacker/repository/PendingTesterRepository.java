package dev.wand.stacker.repository;

import dev.wand.stacker.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists pending-tester Discord user IDs to the {@code pending_testers}
 * PostgreSQL table so role assignments survive bot restarts.
 *
 * <p>All methods obtain a connection from {@link Database#getConnection()} and
 * release it immediately after use via try-with-resources.</p>
 */
public final class PendingTesterRepository {

    private static final Logger logger = LoggerFactory.getLogger(PendingTesterRepository.class);

    private PendingTesterRepository() {
    }

    /**
     * Add a user to the pending-testers list.
     * Silently ignores duplicates (ON CONFLICT DO NOTHING).
     *
     * @param userId the Discord user ID to add
     * @throws SQLException if the database operation fails
     */
    public static void add(String userId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO pending_testers (user_id) VALUES (?) ON CONFLICT DO NOTHING")) {
            ps.setString(1, userId);
            ps.executeUpdate();
            logger.debug("PendingTesterRepository: added user {}", userId);
        }
    }

    /**
     * Remove a user from the pending-testers list.
     *
     * @param userId the Discord user ID to remove
     * @throws SQLException if the database operation fails
     */
    public static void remove(String userId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM pending_testers WHERE user_id = ?")) {
            ps.setString(1, userId);
            ps.executeUpdate();
            logger.debug("PendingTesterRepository: removed user {}", userId);
        }
    }

    /**
     * Check whether a user is in the pending-testers list.
     *
     * @param userId the Discord user ID to look up
     * @return {@code true} if the user is pending
     * @throws SQLException if the database operation fails
     */
    public static boolean contains(String userId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM pending_testers WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
