package uranium.nz.bot.ui;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;
import uranium.nz.bot.Bot;
import uranium.nz.bot.database.WhitelistManager;
import uranium.nz.bot.database.WhitelistedUser;
import uranium.nz.bot.database.DatabaseManager;

public class UIListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("whitelist")) return;

        OptionMapping nameOption = event.getOption("name");
        OptionMapping findOption = event.getOption("find");
        OptionMapping removeOption = event.getOption("remove");

        if (removeOption != null) {
            handleWhitelistRemoveCommand(event, removeOption);
        } else if (findOption != null) {
            handleWhitelistFindCommand(event, findOption);
        } else if (nameOption != null) {
            handleWhitelistAddCommand(event, nameOption);
        } else {
            handleWhitelistPanelCommand(event);
        }
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
        String query = queryOption.getAsString();
        event.deferReply(true).queue();

        var userData = DatabaseManager.findUserByQuery(query);

        if (userData.isEmpty()) {
            event.getHook().sendMessage("❌ Користувач за запитом `" + query + "` не знайдений у вайтлисті.").setEphemeral(true).queue();
            return;
        }

        WhitelistedUser user = userData.get();
        boolean removed = WhitelistManager.removeMain(user.discordId());

        String finalMessage = removed ? String.format("✅ Користувача з ID `%d` було успішно видалено.", user.discordId())
                : String.format("❌ Не вдалося видалити користувача з ID `%d`.", user.discordId());
        event.getHook().sendMessage(finalMessage).queue();
    }

    private void handleWhitelistFindCommand(SlashCommandInteractionEvent event, OptionMapping queryOption) {
        String query = queryOption.getAsString();
        event.deferReply(true).queue();

        var userData = DatabaseManager.findUserByQuery(query);

        if (userData.isEmpty()) {
            event.getHook().sendMessage("❌ Користувач за запитом `" + query + "` не знайдений.").setEphemeral(true).queue();
            return;
        }

        WhitelistedUser user = userData.get();
        Bot.getJda().retrieveUserById(user.discordId()).queue(
                discordUser -> event.getHook().sendMessage(UIMessages.showFindUserResult(discordUser, userData, false)).setEphemeral(true).queue(),
                error -> event.getHook().sendMessage("⚠️ Користувач з ID `" + user.discordId() + "` не знайдений на сервері, але є у вайтлисті.").setEphemeral(true).queue()
        );
    }

    private void handleWhitelistAddCommand(SlashCommandInteractionEvent event, OptionMapping usernameOption) {
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.reply("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").setEphemeral(true).queue();
            return;
        }

        UIStates currentState = session.getCurrentState();
        if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME &&
            currentState != UIStates.AWAITING_MAIN_NAME_CHANGE && currentState != UIStates.AWAITING_TWIN_NAME_CHANGE
        ) {
            event.reply("UI зараз не в режимі додавання користувача. Будь ласка, спочатку виберіть дію в панелі.").setEphemeral(true).queue();
            return;
        }

        String username = usernameOption.getAsString();
        if (WhitelistManager.isUsernameTaken(username)) {
            event.reply(String.format("❌ Нік `%s` вже зайнятий.", username)).setEphemeral(true).queue();
            return;
        }

        Member targetMember = session.getSelectedMember();

        boolean success;
        String replyMessage;
        if (currentState == UIStates.AWAITING_MAIN_NAME_CHANGE) {
            success = WhitelistManager.updateMainName(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Основний нік для %s змінено на `%s`.", targetMember.getAsMention(), username)
                    : "Не вдалося змінити основний нік.";
        } else if (currentState == UIStates.AWAITING_TWIN_NAME_CHANGE) {
            success = WhitelistManager.updateTwinName(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Нік твінка для %s змінено на `%s`.", targetMember.getAsMention(), username)
                    : "Не вдалося змінити нік твінка.";
        } else if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
            success = WhitelistManager.addMain(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Користувач вже існує або це помилка бази данних.";
        } else {
            success = WhitelistManager.addTwin(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Твінк акаунт вже існує або основний аккаунт не був вказаний.";
        }

        session.goBack();
        String finalMessage = success ? "✅ " + replyMessage : "❌ " + replyMessage;
        event.reply(finalMessage).setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        String componentId = e.getComponentId();
        if (!componentId.startsWith("wl:")) return;

        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId()) {
            e.reply("Це не ваша сесія, або вона застаріла.").setEphemeral(true).queue();
            return;
        }
        session.touch();

        switch (componentId) {
            case "wl:add" -> {
                session.changeState(UIStates.ADD_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.addUser())).queue();
            }
            case "wl:prev" -> {
                session.goBack();
                e.editMessage(MessageEditData.fromCreateData(getMessageForState(session.getCurrentState(), session.getSelectedMember()))).queue();
            }
            case "wl:close" -> {
                UI.UIMemory.removeSession(e.getUser().getIdLong());
                e.deferEdit().queue(v -> e.getHook().deleteOriginal().queue());
            }
            case "wl:remove" -> {
                session.changeState(UIStates.REMOVE_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.removeUser())).queue();
            }
            case "wl:find" -> {
                session.changeState(UIStates.FIND_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.findUser())).queue();
            }
            case "wl:change" -> {
                session.changeState(UIStates.CHANGE_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.changeUser())).queue();
            }
            case "wl:add_main" -> {
                session.changeState(UIStates.AWAITING_MAIN_USERNAME);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.promptForMainUsername(session.getSelectedMember()))).queue();
            }
            case "wl:add_twin" -> {
                session.changeState(UIStates.AWAITING_TWIN_USERNAME);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.promptForTwinUsername(session.getSelectedMember()))).queue();
            }
            case "wl:change_main" -> {
                session.changeState(UIStates.AWAITING_MAIN_NAME_CHANGE);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.promptForNewUsername(session.getSelectedMember(), "main"))).queue();
            }
            case "wl:change_twin" -> {
                session.changeState(UIStates.AWAITING_TWIN_NAME_CHANGE);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.promptForNewUsername(session.getSelectedMember(), "twin"))).queue();
            }
            case "wl:remove_main" -> {
                Member memberToRemove = session.getSelectedMember();
                boolean removed = WhitelistManager.removeMain(memberToRemove.getIdLong());
                String reply = removed ? String.format("Користувача %s було повність видалено.", memberToRemove.getAsMention())
                        : String.format("Не вдалося видалити дані для %s.", memberToRemove.getAsMention());
                session.goBack();
                e.editMessage(MessageEditData.fromCreateData(UIMessages.showSuccessAndGoBack(reply, "wl:prev", "wl:close"))).queue();
            }
            case "wl:remove_twin" -> {
                Member memberToRemove = session.getSelectedMember();
                boolean removed = WhitelistManager.removeTwin(memberToRemove.getIdLong());
                String reply = removed ? String.format("Твінк акаунт для %s було видалено.", memberToRemove.getAsMention())
                        : String.format("Не вдалося видалити твінк акаунт для %s.", memberToRemove.getAsMention());
                e.editMessage(MessageEditData.fromCreateData(UIMessages.showSuccessAndGoBack(reply, "wl:prev", "wl:close"))).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent e) {
        if (!"wl:user".equals(e.getComponentId())) return;
        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId()) {
            e.reply("Це не ваша сесія, або вона застаріла.").setEphemeral(true).queue();
            return;
        }
        session.touch();
        IMentionable selectedEntity = e.getValues().stream().findFirst().orElse(null);
        if (!(selectedEntity instanceof Member selectedMember)) {
            e.reply("Користувача не вибрано.").setEphemeral(true).queue();
            return;
        }
        UIStates currentState = session.getCurrentState();
        if (currentState == UIStates.ADD_USER) {
            session.setSelectedMember(selectedMember);
            session.changeState(UIStates.AWAITING_ADD_TYPE);
            e.editMessage(MessageEditData.fromCreateData(UIMessages.showAddUserOptions(selectedMember))).queue();
        } else if (currentState == UIStates.REMOVE_USER) {
            session.setSelectedMember(selectedMember);
            session.changeState(UIStates.AWAITING_REMOVE_TYPE);
            e.editMessage(MessageEditData.fromCreateData(UIMessages.showRemoveUserOptions(selectedMember))).queue();
        } else if (currentState == UIStates.CHANGE_USER) {
            if (!WhitelistManager.isUserWhitelisted(selectedMember.getIdLong())) {
                e.reply("❌ Користувач " + selectedMember.getAsMention() + " не знайдений у вайтлисті.").setEphemeral(true).queue();
                return;
            }
            session.setSelectedMember(selectedMember);
            session.changeState(UIStates.AWAITING_CHANGE_TYPE);
            e.editMessage(MessageEditData.fromCreateData(UIMessages.showChangeUserOptions(selectedMember))).queue();
        } else if (currentState == UIStates.FIND_USER) {
            session.setSelectedMember(selectedMember);
            session.changeState(UIStates.SHOWING_FIND_RESULT);
            var userData = DatabaseManager.getWhitelistedUser(selectedMember.getIdLong());
            e.editMessage(MessageEditData.fromCreateData(UIMessages.showFindUserResult(selectedMember.getUser(), userData))).queue();
        }
    }

    private MessageCreateData getMessageForState(UIStates state, Member member) {
        return switch (state) {
            case ROOT -> UIMessages.root();
            case ADD_USER -> UIMessages.addUser();
            case REMOVE_USER -> UIMessages.removeUser();
            case FIND_USER -> UIMessages.findUser();
            case CHANGE_USER -> UIMessages.changeUser();
            case AWAITING_ADD_TYPE -> UIMessages.showAddUserOptions(member);
            case AWAITING_MAIN_USERNAME -> UIMessages.promptForMainUsername(member);
            case AWAITING_TWIN_USERNAME -> UIMessages.promptForTwinUsername(member);
            case AWAITING_REMOVE_TYPE -> UIMessages.showRemoveUserOptions(member);
            case AWAITING_CHANGE_TYPE -> UIMessages.showChangeUserOptions(member);
            case AWAITING_MAIN_NAME_CHANGE, AWAITING_TWIN_NAME_CHANGE -> UIMessages.showChangeUserOptions(member);
            case SHOWING_FIND_RESULT -> UIMessages.findUser();
            default -> UIMessages.root();
        };
    }
}
