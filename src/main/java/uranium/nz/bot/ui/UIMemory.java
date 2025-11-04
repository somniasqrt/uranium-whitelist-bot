package uranium.nz.bot.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UIMemory {
    private static final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public static Session getSession(long userId) {
        return sessions.get(userId);
    }

    public static void putSession(long userId, Session session) {
        sessions.put(userId, session);
    }

    public static void removeSession(long userId) {
        sessions.remove(userId);
    }
}
