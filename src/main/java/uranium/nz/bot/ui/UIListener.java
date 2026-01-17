package uranium.nz.bot.ui;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;
import uranium.nz.bot.Bot;
import uranium.nz.bot.database.BanManager;
import uranium.nz.bot.database.DatabaseManager;
import uranium.nz.bot.database.WhitelistManager;
import uranium.nz.bot.database.WhitelistedUser;
import uranium.nz.bot.util.TimeUtil;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UIListener extends ListenerAdapter {

    private final WhitelistManager whitelistManager;
    private final UIMessages uiMessages;
    private final ConcurrentHashMap<String, PendingBan> pendingBans = new ConcurrentHashMap<>();

    private record PendingBan(WhitelistedUser user, String reason, String time, Instant banUntil) {
    }

    public UIListener(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
        this.uiMessages = new UIMessages(whitelistManager);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("У вас немає дозволу на використання цієї команди.").setEphemeral(true).queue();
            return;
        }
        switch (event.getName()) {
            case "whitelist" -> handleWhitelistSlashCommand(event);
            case "ban" -> handleBanCommand(event);
        }
    }

    private void handleWhitelistSlashCommand(SlashCommandInteractionEvent event) {
        OptionMapping nameOption = event.getOption("name");
        OptionMapping findOption = event.getOption("find");
        OptionMapping removeOption = event.getOption("remove");

        if (removeOption != null) {
            handleWhitelistRemoveCommand(event, removeOption);
        } else if (findOption != null) {
            handleWhitelistFindCommand(event, findOption);
        } else if (nameOption != null) {
            handleWhitelistNameCommand(event, nameOption);
        } else {
            handleWhitelistPanelCommand(event);
        }
    }

    private void handleBanCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        Bot.executor.submit(() -> {
            String nameOrId = event.getOption("name_or_id").getAsString();
            String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "Причину не вказано";
            String time = event.getOption("time") != null ? event.getOption("time").getAsString() : null;

            var userData = DatabaseManager.findUserByQuery(nameOrId);
            if (userData.isEmpty()) {
                event.getHook().sendMessage("❌ Користувача `" + nameOrId + "` не знайдено").queue();
                return;
            }

            WhitelistedUser user = userData.get();
            Instant banUntil = TimeUtil.parseBanDuration(time);
            String banId = UUID.randomUUID().toString();
            pendingBans.put(banId, new PendingBan(user, reason, time, banUntil));

            event.getHook().sendMessage(UIMessages.showBanConfirmation(user, reason, time, banId)).queue();
        });
    }

    private void handleWhitelistPanelCommand(SlashCommandInteractionEvent event) {
        UI.Session existingSession = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (existingSession != null) {
            var channel = Bot.getJda().getTextChannelById(existingSession.getChannelId());
            if (channel != null) {
                channel.retrieveMessageById(existingSession.getMessageId()).queue(
                        message -> event.reply("У вас вже є активна сесія: " + message.getJumpUrl()).setEphemeral(true).queue(),
                        error -> {
                            UI.UIMemory.removeSession(event.getUser().getIdLong());
                            createNewPanel(event);
                        }
                );
            } else {
                UI.UIMemory.removeSession(event.getUser().getIdLong());
                createNewPanel(event);
            }
        } else {
            createNewPanel(event);
        }
    }

    private void createNewPanel(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        UI.Session newSession = new UI.Session(event.getUser().getIdLong(), event.getChannel().getIdLong(), 0L, UIStates.ROOT);
        event.getHook().sendMessage(UIMessages.root()).queue(message -> {
            newSession.setMessageId(message.getIdLong());
            UI.UIMemory.putSession(event.getUser().getIdLong(), newSession);
        });
    }

    private void handleWhitelistRemoveCommand(SlashCommandInteractionEvent event, OptionMapping queryOption) {
        event.deferReply(true).queue();
        Bot.executor.submit(() -> {
            String query = queryOption.getAsString();
            var userData = DatabaseManager.findUserByQuery(query);

            if (userData.isEmpty()) {
                event.getHook().sendMessage("❌ Користувач за запитом `" + query + "` не знайдений у вайтлисті.").queue();
                return;
            }

            WhitelistedUser user = userData.get();
            boolean removed = whitelistManager.removeMain(user.discordId());

            String finalMessage = removed ? String.format("✅ Користувача з ID `%d` було успішно видалено.", user.discordId())
                    : String.format("❌ Не вдалося видалити користувача з ID `%d`.", user.discordId());
            event.getHook().sendMessage(finalMessage).queue();
        });
    }

    private void handleWhitelistFindCommand(SlashCommandInteractionEvent event, OptionMapping queryOption) {
        event.deferReply(true).queue();
        Bot.executor.submit(() -> {
            String query = queryOption.getAsString();
            var userData = DatabaseManager.findUserByQuery(query);

            if (userData.isEmpty()) {
                event.getHook().sendMessage("❌ Користувач за запитом `" + query + "` не знайдений.").queue();
                return;
            }

            WhitelistedUser user = userData.get();
            Bot.getJda().retrieveUserById(user.discordId()).queue(
                    discordUser -> event.getHook().sendMessage(UIMessages.showFindUserResult(discordUser, userData, false)).queue(),
                    error -> event.getHook().sendMessage("⚠️ Користувач з ID `" + user.discordId() + "` не знайдений на сервері, але є у вайтлисті.").queue()
            );
        });
    }

    private void handleWhitelistNameCommand(SlashCommandInteractionEvent event, OptionMapping usernameOption) {
        event.deferReply(true).queue();
        Bot.executor.submit(() -> {
            UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
            if (session == null) {
                event.getHook().sendMessage("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").queue();
                return;
            }

            UIStates currentState = session.getCurrentState();
            if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME &&
                    currentState != UIStates.AWAITING_MAIN_NAME_CHANGE && currentState != UIStates.AWAITING_TWIN_NAME_CHANGE
            ) {
                event.getHook().sendMessage("UI зараз не в режимі додавання користувача. Будь ласка, спочатку виберіть дію в панелі.").queue();
                return;
            }

            String username = usernameOption.getAsString();
            if (whitelistManager.isUsernameTaken(username)) {
                event.getHook().sendMessage(String.format("❌ Нік `%s` вже зайнятий.", username)).queue();
                return;
            }

            Member targetMember = session.getSelectedMember();
            boolean success;
            String replyMessage;
            if (currentState == UIStates.AWAITING_MAIN_NAME_CHANGE) {
                success = whitelistManager.updateMainName(targetMember.getIdLong(), username);
                replyMessage = success ? String.format("Основний нік для %s змінено на `%s`.", targetMember.getAsMention(), username)
                        : "Не вдалося змінити основний нік.";
            } else if (currentState == UIStates.AWAITING_TWIN_NAME_CHANGE) {
                success = whitelistManager.updateTwinName(targetMember.getIdLong(), username);
                replyMessage = success ? String.format("Нік твінка для %s змінено на `%s`.", targetMember.getAsMention(), username)
                        : "Не вдалося змінити нік твінка.";
            } else if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
                success = whitelistManager.addMain(targetMember.getIdLong(), username);
                replyMessage = success ? String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                        : "Користувач вже існує або це помилка бази данних.";
            } else {
                success = whitelistManager.addTwin(targetMember.getIdLong(), username);
                replyMessage = success ? String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                        : "Твінк акаунт вже існує або основний аккаунт не був вказаний.";
            }

            session.goBack();
            String finalMessage = success ? "✅ " + replyMessage : "❌ " + replyMessage;
            event.getHook().sendMessage(finalMessage).queue();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        e.deferEdit().queue();
        Bot.executor.submit(() -> {
            String componentId = e.getComponentId();
            if (componentId.startsWith("wl:")) {
                handleWhitelistButton(e, componentId);
            } else if (componentId.startsWith("ban:")) {
                handleBanButton(e, componentId);
            }
        });
    }

    private void handleBanButton(ButtonInteractionEvent e, String componentId) {
        String[] parts = componentId.split(":");
        String action = parts[1];

        if ("confirm".equals(action)) {
            String banId = parts[2];
            PendingBan ban = pendingBans.remove(banId);
            if (ban != null) {
                boolean success = BanManager.banUser(ban.user().discordId(), ban.user().minecraftName(), ban.user().twinName(), ban.reason(), ban.banUntil());
                if (success) {
                    e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.showBanSuccess(ban.user(), ban.reason(), ban.time()))).queue();
                } else {
                    e.getHook().editOriginal("Failed to ban user.").setComponents().queue();
                }
            } else {
                e.getHook().editOriginal("Ban confirmation expired.").setComponents().queue();
            }
        }
    }

    private void handleWhitelistButton(ButtonInteractionEvent e, String componentId) {
        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId()) {
            e.getHook().sendMessage("Це не ваша сесія, або вона застаріла.").setEphemeral(true).queue();
            return;
        }
        session.touch();

        if ("wl:close".equals(componentId)) {
            UI.UIMemory.removeSession(e.getUser().getIdLong());
            e.getHook().deleteOriginal().queue();
            return;
        }

        switch (componentId) {
            case "wl:add" -> {
                session.changeState(UIStates.ADD_USER);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.addUser())).queue();
            }
            case "wl:prev" -> {
                session.goBack();
                e.getHook().editOriginal(MessageEditData.fromCreateData(getMessageForState(session.getCurrentState(), session.getSelectedMember()))).queue();
            }
            case "wl:remove" -> {
                session.changeState(UIStates.REMOVE_USER);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.removeUser())).queue();
            }
            case "wl:find" -> {
                session.changeState(UIStates.FIND_USER);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.findUser())).queue();
            }
            case "wl:change" -> {
                session.changeState(UIStates.CHANGE_USER);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.changeUser())).queue();
            }
            case "wl:add_main" -> {
                session.changeState(UIStates.AWAITING_MAIN_USERNAME);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.promptForMainUsername(session.getSelectedMember()))).queue();
            }
            case "wl:add_twin" -> {
                session.changeState(UIStates.AWAITING_TWIN_USERNAME);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.promptForTwinUsername(session.getSelectedMember()))).queue();
            }
            case "wl:change_main" -> {
                session.changeState(UIStates.AWAITING_MAIN_NAME_CHANGE);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.promptForNewUsername(session.getSelectedMember(), "main"))).queue();
            }
            case "wl:change_twin" -> {
                session.changeState(UIStates.AWAITING_TWIN_NAME_CHANGE);
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.promptForNewUsername(session.getSelectedMember(), "twin"))).queue();
            }
            case "wl:remove_main" -> {
                Member memberToRemove = session.getSelectedMember();
                boolean removed = whitelistManager.removeMain(memberToRemove.getIdLong());
                String reply = removed ? String.format("Користувача %s було повність видалено.", memberToRemove.getAsMention())
                        : String.format("Не вдалося видалити дані для %s.", memberToRemove.getAsMention());
                session.goBack();
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.showSuccessAndGoBack(reply, "wl:prev", "wl:close"))).queue();
            }
            case "wl:remove_twin" -> {
                Member memberToRemove = session.getSelectedMember();
                boolean removed = whitelistManager.removeTwin(memberToRemove.getIdLong());
                String reply = removed ? String.format("Твінк акаунт для %s було видалено.", memberToRemove.getAsMention())
                        : String.format("Не вдалося видалити твінк акаунт для %s.", memberToRemove.getAsMention());
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.showSuccessAndGoBack(reply, "wl:prev", "wl:close"))).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent e) {
        e.deferEdit().queue();
        Bot.executor.submit(() -> {
            if (!"wl:user".equals(e.getComponentId())) return;
            UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
            if (session == null || e.getUser().getIdLong() != session.getUserId()) {
                e.getHook().sendMessage("Це не ваша сесія, або вона застаріла.").setEphemeral(true).queue();
                return;
            }
            session.touch();

            IMentionable selectedEntity = e.getValues().stream().findFirst().orElse(null);
            if (!(selectedEntity instanceof Member selectedMember)) {
                e.getHook().sendMessage("Користувача не вибрано.").setEphemeral(true).queue();
                return;
            }

            UIStates currentState = session.getCurrentState();
            if (currentState == UIStates.ADD_USER) {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_ADD_TYPE);
                e.getHook().editOriginal(MessageEditData.fromCreateData(uiMessages.showAddUserOptions(selectedMember))).queue();
            } else if (currentState == UIStates.REMOVE_USER) {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_REMOVE_TYPE);
                e.getHook().editOriginal(MessageEditData.fromCreateData(uiMessages.showRemoveUserOptions(selectedMember))).queue();
            } else if (currentState == UIStates.CHANGE_USER) {
                if (!whitelistManager.isUserWhitelisted(selectedMember.getIdLong())) {
                    e.getHook().sendMessage("❌ Користувач " + selectedMember.getAsMention() + " не знайдений у вайтлисті.").setEphemeral(true).queue();
                    return;
                }
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_CHANGE_TYPE);
                e.getHook().editOriginal(MessageEditData.fromCreateData(uiMessages.showChangeUserOptions(selectedMember))).queue();
            } else if (currentState == UIStates.FIND_USER) {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.SHOWING_FIND_RESULT);
                var userData = DatabaseManager.getWhitelistedUser(selectedMember.getIdLong());
                e.getHook().editOriginal(MessageEditData.fromCreateData(UIMessages.showFindUserResult(selectedMember.getUser(), userData))).queue();
            }
        });
    }

    private MessageCreateData getMessageForState(UIStates state, Member member) {
        return switch (state) {
            case ROOT -> UIMessages.root();
            case ADD_USER -> UIMessages.addUser();
            case REMOVE_USER -> UIMessages.removeUser();
            case FIND_USER -> UIMessages.findUser();
            case CHANGE_USER -> UIMessages.changeUser();
            case AWAITING_ADD_TYPE -> uiMessages.showAddUserOptions(member);
            case AWAITING_MAIN_USERNAME -> UIMessages.promptForMainUsername(member);
            case AWAITING_TWIN_USERNAME -> UIMessages.promptForTwinUsername(member);
            case AWAITING_REMOVE_TYPE -> uiMessages.showRemoveUserOptions(member);
            case AWAITING_CHANGE_TYPE -> uiMessages.showChangeUserOptions(member);
            case AWAITING_MAIN_NAME_CHANGE, AWAITING_TWIN_NAME_CHANGE -> uiMessages.showChangeUserOptions(member);
            case SHOWING_FIND_RESULT -> UIMessages.findUser();
            default -> UIMessages.root();
        };
    }
}
