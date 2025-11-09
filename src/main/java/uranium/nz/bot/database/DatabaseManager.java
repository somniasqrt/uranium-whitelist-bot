package uranium.nz.bot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void init() {
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").ignoreIfMissing().load();
        String host = dotenv.get("DB_HOST", "localhost");
        String dbName = dotenv.get("DB_NAME");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");

        if (dbName == null || dbName.isBlank() || user == null || user.isBlank() || password == null || password.isBlank() || "localhost".equals(host)) {
            System.out.println("Main .env has invalid DB config, trying .env.test");
            dotenv = Dotenv.configure().directory("./").filename(".env.test").ignoreIfMissing().load();
            host = dotenv.get("DB_HOST", "localhost");
            dbName = dotenv.get("DB_NAME");
            user = dotenv.get("DB_USER");
            password = dotenv.get("DB_PASSWORD");
        }

        if (dbName == null || dbName.isBlank() || user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Database configuration is missing or blank in both .env and .env.test (DB_NAME, DB_USER, DB_PASSWORD)");
        }

        HikariConfig config = new HikariConfig();
        String port = dotenv.get("DB_PORT", "5432");
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s?sslmode=prefer", host, port, dbName));
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        try (Connection connection = getConnection()) {
            if (!connection.isValid(2)) {
                throw new SQLException("Database connection is not valid.");
            }
            System.out.println("Database connection pool initialized and connection is valid.");
        } catch (SQLException e) {
            System.err.println("Failed to establish database connection!");
            e.printStackTrace();
            throw new RuntimeException("Failed to establish database connection.", e);
        }

        initSchema();
    }

    private static void initSchema() {
        String sql = """
            CREATE TABLE IF NOT EXISTS whitelist (
                id SERIAL PRIMARY KEY,
                discord_id BIGINT NOT NULL UNIQUE, -- The user's Discord ID, one entry per user
                minecraft_name VARCHAR(100) NOT NULL UNIQUE, -- The user`s main in-game name
                twin_name VARCHAR(100) UNIQUE, -- The user's optional twin name, can be null
                added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- The time when user was added
                on_server BOOLEAN NOT NULL DEFAULT true, -- Is the user currently in the Discord server?
                paid BOOLEAN NOT NULL DEFAULT false, -- Paid(true) or trial(false)
                expires_on TIMESTAMP WITH TIME ZONE -- Null if paid=true, otherwise the expiration date for a trial
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database schema initialized or already exists.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema!");
            e.printStackTrace();
        }
    }

    public static Optional<WhitelistedUser> getWhitelistedUser(long discordId) {
        String sql = "SELECT * FROM whitelist WHERE discord_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new WhitelistedUser(
                            rs.getInt("id"),
                            rs.getLong("discord_id"),
                            rs.getString("minecraft_name"),
                            rs.getString("twin_name"),
                            rs.getTimestamp("added_at"),
                            rs.getBoolean("on_server"),
                            rs.getBoolean("paid"),
                            rs.getTimestamp("expires_on")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<WhitelistedUser> getWhitelistedUser(String minecraftName) {
        String sql = "SELECT * FROM whitelist WHERE minecraft_name = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, minecraftName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new WhitelistedUser(
                            rs.getInt("id"),
                            rs.getLong("discord_id"),
                            rs.getString("minecraft_name"),
                            rs.getString("twin_name"),
                            rs.getTimestamp("added_at"),
                            rs.getBoolean("on_server"),
                            rs.getBoolean("paid"),
                            rs.getTimestamp("expires_on")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<WhitelistedUser> findUserByQuery(String query) {
        // Attempt to parse as a long for Discord ID
        try {
            long discordId = Long.parseLong(query.replaceAll("[^0-9]", "")); // Sanitize for mentions like <@12345>
            Optional<WhitelistedUser> user = getWhitelistedUser(discordId);
            if (user.isPresent()) {
                return user;
            }
        } catch (NumberFormatException ignored) {
            // Not a numeric ID, proceed to check names
        }

        // Check minecraft_name and twin_name
        String sql = "SELECT * FROM whitelist WHERE minecraft_name = ? OR twin_name = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query);
            stmt.setString(2, query);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new WhitelistedUser(
                            rs.getInt("id"),
                            rs.getLong("discord_id"),
                            rs.getString("minecraft_name"),
                            rs.getString("twin_name"),
                            rs.getTimestamp("added_at"),
                            rs.getBoolean("on_server"),
                            rs.getBoolean("paid"),
                            rs.getTimestamp("expires_on")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during user search by query: " + query);
            e.printStackTrace();
        }

        // If we reach here, no user was found
        return Optional.empty();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }
}