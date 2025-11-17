package uranium.nz.bot.database;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import uranium.nz.bot.Bot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WhitelistManager {

    private final long whitelistRoleId;

    public WhitelistManager(long whitelistRoleId) {
        this.whitelistRoleId = whitelistRoleId;
    }

    public boolean addMain(long discordId, String username) {
        String sql = "INSERT INTO whitelist (discord_id, minecraft_name) VALUES (?, ?) ON CONFLICT (discord_id) DO UPDATE SET minecraft_name = EXCLUDED.minecraft_name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            pstmt.setString(2, username);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                addRoleToUser(discordId, whitelistRoleId);
            }
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error adding main account - " + e.getMessage());
            return false;
        }
    }

    public boolean addTwin(long discordId, String username) {
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

    public boolean updateMainName(long discordId, String newUsername) {
        String sql = "UPDATE whitelist SET minecraft_name = ? WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setLong(2, discordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating main name: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTwinName(long discordId, String newUsername) {
        String sql = "UPDATE whitelist SET twin_name = ? WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setLong(2, discordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating twin name: " + e.getMessage());
            return false;
        }
    }

    public boolean removeMain(long discordId) {
        String deleteSql = "DELETE FROM whitelist WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, discordId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                removeRoleFromUser(discordId, whitelistRoleId);
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error removing main account: " + e.getMessage());
            return false;
        }
    }

    public boolean removeTwin(long discordId) {
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

    public boolean hasMain(long discordId) {
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

    public boolean hasTwin(long discordId) {
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

    public boolean isUserWhitelisted(long discordId) {
        return hasMain(discordId) || hasTwin(discordId);
    }

    public boolean isUsernameTaken(String username) {
        String sql = "SELECT 1 FROM whitelist WHERE minecraft_name = ? OR twin_name = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking if username is taken: " + e.getMessage());
            return true;
        }
    }

    public boolean setOnServerStatus(long discordId, boolean onServer) {
        String sql = "UPDATE whitelist SET on_server = ? WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, onServer);
            pstmt.setLong(2, discordId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                if (onServer) {
                    if (isUserWhitelisted(discordId)) {
                        addRoleToUser(discordId, whitelistRoleId);
                    }
                } else {
                    removeRoleFromUser(discordId, whitelistRoleId);
                }
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating on_server status: " + e.getMessage());
            return false;
        }
    }

    public void addRoleToUser(long discordId, long roleId) {
        Guild guild = Bot.guild;
        if (guild == null) return;
        Role role = guild.getRoleById(roleId);
        if (role == null) return;
        Member member = guild.getMemberById(discordId);
        if (member == null) return;
        guild.addRoleToMember(member, role).queue();
    }

    public void removeRoleFromUser(long discordId, long roleId) {
        Guild guild = Bot.guild;
        if (guild == null) return;
        Role role = guild.getRoleById(roleId);
        if (role == null) return;
        Member member = guild.getMemberById(discordId);
        if (member == null) return;
        guild.removeRoleFromMember(member, role).queue();
    }
}
