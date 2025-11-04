package uranium.nz.bot.ui;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UI {

    @Getter
    public static class Session {
        private final long userId;
        private final long channelId;
        @Setter
        private long messageId;
        private UIStates currentState;
        private UIStates previousState;
        @Setter
        private Member selectedMember;
        private long lastInteraction;

        public Session(long userId, long channelId, long messageId, UIStates initialState) {
            this.userId = userId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.currentState = initialState;
            this.previousState = initialState;
            touch();
        }

        public void changeState(UIStates newState) {
            this.previousState = this.currentState;
            this.currentState = newState;
            touch();
        }

        public void touch() {
            this.lastInteraction = System.currentTimeMillis();
        }
    }

    public static class UIMemory {
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
}
