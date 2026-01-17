package uranium.nz.bot.database;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import uranium.nz.bot.Bot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

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
                updateUserOnJoin(discordId);
                Bot.getJda().retrieveUserById(discordId).queue(user ->
                        System.out.printf("Added main: %s %s %d%n", username, user.getName(), discordId));
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
            if (affectedRows > 0) {
                Bot.getJda().retrieveUserById(discordId).queue(user ->
                        System.out.printf("Added twin: %s %s %d%n", username, user.getName(), discordId));
            } else {
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
            if (affectedRows > 0) {
                updateUserOnJoin(discordId);
            }
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
        Optional<WhitelistedUser> userOpt = DatabaseManager.getWhitelistedUser(discordId);
        String deleteSql = "DELETE FROM whitelist WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, discordId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                removeRoleFromUser(discordId, whitelistRoleId);
                userOpt.ifPresent(whitelistedUser -> Bot.getJda().retrieveUserById(discordId).queue(user ->
                        System.out.printf("Removed main: %s %s %d%n", whitelistedUser.minecraftName(), user.getName(), discordId)));
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error removing main account: " + e.getMessage());
            return false;
        }
    }

    public boolean removeTwin(long discordId) {
        Optional<WhitelistedUser> userOpt = DatabaseManager.getWhitelistedUser(discordId);
        String sql = "UPDATE whitelist SET twin_name = NULL WHERE discord_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                userOpt.ifPresent(whitelistedUser -> Bot.getJda().retrieveUserById(discordId).queue(user ->
                        System.out.printf("Removed twin: %s %s %d%n", whitelistedUser.twinName(), user.getName(), discordId)));
            }
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
                    updateUserOnJoin(discordId);
                } else {
                }
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating on_server status: " + e.getMessage());
            return false;
        }
    }

    public void updateUserOnJoin(long discordId) {
        Optional<WhitelistedUser> userOpt = DatabaseManager.getWhitelistedUser(discordId);
        userOpt.ifPresent(user -> {
            Guild guild = Bot.guild;
            if (guild == null) return;
            guild.retrieveMemberById(discordId).queue(member -> {
                try {
                    member.modifyNickname(user.minecraftName()).queue();
                } catch (Exception e) {
                    System.err.println("Could not modify nickname for user " + discordId + ": " + e.getMessage());
                }
                Role role = guild.getRoleById(whitelistRoleId);
                if (role != null) {
                    guild.addRoleToMember(member, role).queue();
                } else {
                    System.err.println("Whitelist role not found!");
                }
            }, error -> System.err.println("Could not find member with ID " + discordId + " to update on join."));
        });
    }

    public void removeRoleFromUser(long discordId, long roleId) {
        Guild guild = Bot.guild;
        if (guild == null) {
            System.err.println("Guild is null, cannot remove role.");
            return;
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            System.err.println("Role with ID " + roleId + " not found, cannot remove role.");
            return;
        }

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue();
            }
        }, error -> {
            System.err.printf("Could not find member with ID %d to remove role. They may have left the server.\n", discordId);
        });
    }
}
