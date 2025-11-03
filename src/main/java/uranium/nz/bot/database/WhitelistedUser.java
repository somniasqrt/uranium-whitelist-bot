package uranium.nz.bot.database;

import java.sql.Timestamp;

public record WhitelistedUser(
        int id,
        long discordId,
        String minecraftName,
        String twinName,
        Timestamp addedAt,
        boolean onServer,
        boolean paid,
        Timestamp expiresOn) {
}
