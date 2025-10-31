package uranium.nz.bot.ui;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.*;

public class UI {

    public static class Session {
        private final long userId;
        private final long channelId;
        private long messageId;
        private long lastAccessed;
        private Member selectedMember;
        private final Deque<UIStates> history = new ArrayDeque<>();

        public Session(long userId, long channelId, long messageId, UIStates initialState) {
            this.userId = userId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.lastAccessed = System.currentTimeMillis();
            this.history.push(initialState);
        }

        public UIStates getCurrentState() {return history.peek();}

        public void changeState(UIStates newState) {
            this.history.push(newState);
            touch();
        }

        public UIStates getPreviousState() {
            if (history.size() > 1) {
                history.pop();
            }
            touch();
            return history.peek();
        }

        public void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }

        public boolean isExpired(long timeoutMillis) {
            return (System.currentTimeMillis() - lastAccessed) > timeoutMillis;
        }

        public long getUserId() { return userId; }
        public long getChannelId() { return channelId; }
        public long getMessageId() { return messageId; }
        public void setMessageId(long messageId) { this.messageId = messageId; }
        public Member getSelectedMember() { return selectedMember; }
        public void setSelectedMember(Member selectedMember) { this.selectedMember = selectedMember; }
    }

    public static class UIMemory {


        private static final ConcurrentMap<Long, Session> sessions = new ConcurrentHashMap<>();
        private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private static final long SESSION_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

        public static void start(JDA jda) {
            scheduler.scheduleAtFixedRate(() -> cleanup(jda), 1, 1, TimeUnit.MINUTES);
            System.out.println("UI memory started");
        }

        public static void stop() {scheduler.shutdown();}
        public static void putSession(long userId, Session session) {sessions.put(userId, session);}
        public static Session getSession(long userId) {return sessions.get(userId);}
        public static void removeSession(long userId) {sessions.remove(userId);}

        private static void cleanup(JDA jda) {
            sessions.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired(SESSION_TIMEOUT)) {
                    MessageChannel channel = jda.getChannelById(MessageChannel.class, entry.getValue().getChannelId());
                    if (channel != null) {
                        channel.deleteMessageById(entry.getValue().getMessageId()).queue(null, (e) -> System.out.println("Failed to delete expired UI message"));
                    }
                    return true;
                }
                return false;
            });
        }
    }
}
