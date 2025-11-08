package uranium.nz.bot.ui;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UI {

    @Getter
    public static class Session {
        private final long userId;
        private final long channelId;
        @Setter
        private long messageId;
        private final Deque<UIStates> history = new LinkedList<>();
        @Setter
        private Member selectedMember;
        private long lastInteraction;

        public Session(long userId, long channelId, long messageId, UIStates initialState) {
            this.userId = userId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.history.push(initialState);
            touch();
        }

        public UIStates getCurrentState() {
            return history.peek();
        }

        public void changeState(UIStates newState) {
            if (history.isEmpty() || history.peek() != newState) {
                history.push(newState);
            }
            touch();
        }

        public void goBack() {
            if (history.size() > 1) {
                history.pop();
            }
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
