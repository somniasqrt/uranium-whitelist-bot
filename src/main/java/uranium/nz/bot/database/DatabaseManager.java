package uranium.nz.bot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import uranium.nz.bot.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void init() {
        String host = Config.get("DB_HOST", "localhost");
        String dbName = Config.get("DB_NAME");
        String user = Config.get("DB_USER");
        String password = Config.get("DB_PASSWORD");

        if (dbName == null || dbName.equals("YOUR_DATABASE_NAME") || user == null || password == null) {
            throw new IllegalStateException("Database configuration is missing or incomplete in the .env file (DB_NAME, DB_USER, DB_PASSWORD)");
        }

        HikariConfig config = new HikariConfig();
        String port = Config.get("DB_PORT", "5432");
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
        String whitelistSql = """
            CREATE TABLE IF NOT EXISTS whitelist (
                id SERIAL PRIMARY KEY,
                discord_id BIGINT NOT NULL UNIQUE,
                minecraft_name VARCHAR(100) NOT NULL UNIQUE,
                twin_name VARCHAR(100) UNIQUE,
                added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                on_server BOOLEAN NOT NULL DEFAULT true,
                paid BOOLEAN NOT NULL DEFAULT false,
                expires_on TIMESTAMP WITH TIME ZONE
            );
        """;

        String bannedSql = """
            CREATE TABLE IF NOT EXISTS banned (
                id SERIAL PRIMARY KEY,
                discord_id BIGINT NOT NULL UNIQUE,
                minecraft_name VARCHAR(100) NOT NULL UNIQUE,
                twin_name VARCHAR(100) UNIQUE,
                reason TEXT,
                banned_until TIMESTAMP WITH TIME ZONE
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(whitelistSql);
            stmt.execute(bannedSql);
            System.out.println("Database schema initialized or already exists.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema!");
            e.printStackTrace();
        }
    }

    public static boolean isUserBanned(long discordId) {
        String sql = "SELECT 1 FROM banned WHERE discord_id = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
        try {
            long discordId = Long.parseLong(query.replaceAll("[^0-9]", ""));
            Optional<WhitelistedUser> user = getWhitelistedUser(discordId);
            if (user.isPresent()) {
                return user;
            }
        } catch (NumberFormatException ignored) {
        }

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
