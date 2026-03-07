package dev.wand.stacker.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.wand.stacker.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the PostgreSQL connection pool and schema initialization.
 *
 * <p>Call {@link #initialize()} once at bot startup (before any repositories are used).
 * Each repository obtains a connection via {@link #getConnection()} and closes it after use.
 * Call {@link #close()} on graceful shutdown to release all pooled connections.</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code DB_NAME} — database name</li>
 *   <li>{@code DB_USER} — database username</li>
 *   <li>{@code DB_PASSWORD} — database password</li>
 * </ul>
 * Optional:
 * <ul>
 *   <li>{@code DB_HOST} — hostname (default: {@code localhost})</li>
 *   <li>{@code DB_PORT} — port (default: {@code 5432})</li>
 * </ul>
 */
public final class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private static HikariDataSource dataSource;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    private Database() {
    }

    /**
     * Initialize the connection pool and create any missing tables.
     * Must be called once before any repository is used.
     */
    public static void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.warn("Database already initialized; ignoring duplicate call");
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.getDbUrl());
        config.setUsername(Config.getDbUser());
        config.setPassword(Config.getDbPassword());
        config.setDriverClassName("org.postgresql.Driver");

        // Pool sizing — a small Discord bot doesn't need many connections
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);
        config.setPoolName("StackerPool");

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool created ({})", Config.getDbUrl());

        createTables();
    }

    /**
     * Obtain a connection from the pool.
     * The caller is responsible for closing the connection (use try-with-resources).
     *
     * @return an active {@link Connection}
     * @throws SQLException if a connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException("Database has not been initialized. Call Database.initialize() first.");
        }
        return dataSource.getConnection();
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    /**
     * Close the connection pool on graceful shutdown.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    /**
     * Create all required tables if they do not already exist.
     * Add new {@code CREATE TABLE IF NOT EXISTS} statements here when new tables are needed.
     */
    private static void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Live stats embed locations
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS live_stats_embeds (
                        channel_id  VARCHAR(32) NOT NULL,
                        message_id  VARCHAR(32) NOT NULL,
                        PRIMARY KEY (channel_id, message_id)
                    )
                    """);

            // Pending tester user IDs
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS pending_testers (
                        user_id VARCHAR(32) PRIMARY KEY
                    )
                    """);

            logger.info("Database tables verified / created");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database tables", e);
        }
    }
}
