package uranium.nz.bot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WhitelistManager {

    public static boolean addMain(long discordId, String username) {
        String sql = "INSERT INTO whitelist (discord_id, minecraft_name) VALUES (?, ?) ON CONFLICT (discord_id) DO UPDATE SET minecraft_name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding main account - " + e.getMessage());
            return false;
        }
    }

    public static boolean addTwin(long discordId, String username) {
        String sql = "UPDATE whitelist SET twin_name = ? WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setLong(2, discordId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("Error adding twin account: user with discordId " + discordId + " not found. Please add a main account first.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding twin account - " + e.getMessage());
            return false;
        }
    }

    public static boolean removeMain(long discordId) {
        String deleteSql = "DELETE FROM whitelist WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, discordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error removing main account: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeTwin(long discordId) {
        String sql = "UPDATE whitelist SET twin_name = NULL WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error removing twin account: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasMain(long discordId) {
        String sql = "SELECT 1 FROM whitelist WHERE discord_id = ? AND minecraft_name IS NOT NULL LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking main account: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasTwin(long discordId) {
        String sql = "SELECT 1 FROM whitelist WHERE discord_id = ? AND twin_name IS NOT NULL LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking twin account: " + e.getMessage());
            return false;
        }
    }

    public static boolean isUserWhitelisted(long discordId) {
        return hasMain(discordId)
    }
}
