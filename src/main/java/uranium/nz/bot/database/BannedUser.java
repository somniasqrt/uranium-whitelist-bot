package uranium.nz.bot.database;

import java.time.Instant;

public record BannedUser(long discordId, String reason, Instant bannedUntil) {}
