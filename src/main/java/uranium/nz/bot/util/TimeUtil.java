package uranium.nz.bot.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");

    public static Instant parseBanDuration(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return null; // Permanent ban
        }

        Matcher matcher = DURATION_PATTERN.matcher(durationStr.toLowerCase());
        if (!matcher.matches()) {
            return null; 
        }

        long amount = Long.parseLong(matcher.group(1));
        char unitChar = matcher.group(2).charAt(0);

        Instant now = Instant.now();
        switch (unitChar) {
            case 's':
                return now.plus(amount, ChronoUnit.SECONDS);
            case 'm':
                return now.plus(amount, ChronoUnit.MINUTES);
            case 'h':
                return now.plus(amount, ChronoUnit.HOURS);
            case 'd':
                return now.plus(amount, ChronoUnit.DAYS);
            case 'w':
                return now.plus(amount * 7, ChronoUnit.DAYS);
            case 'y':
                return now.plus(amount * 365, ChronoUnit.DAYS);
            default:
                return null;
        }
    }
}
