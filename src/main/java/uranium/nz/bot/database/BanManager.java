package uranium.nz.bot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class BanManager {

    public static boolean banUser(long discordId, String minecraftName, String twinName, String reason, Instant bannedUntil) {
        String sql = """
            INSERT INTO banned (discord_id, minecraft_name, twin_name, reason, banned_until)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (discord_id) DO UPDATE SET
                minecraft_name = EXCLUDED.minecraft_name,
                twin_name = EXCLUDED.twin_name,
                reason = EXCLUDED.reason,
                banned_until = EXCLUDED.banned_until;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, discordId);
            pstmt.setString(2, minecraftName);
            pstmt.setString(3, twinName);
            pstmt.setString(4, reason);
            if (bannedUntil != null) {
                pstmt.setTimestamp(5, Timestamp.from(bannedUntil));
            } else {
                pstmt.setNull(5, java.sql.Types.TIMESTAMP);
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error banning user: " + e.getMessage());
            return false;
        }
    }
}
