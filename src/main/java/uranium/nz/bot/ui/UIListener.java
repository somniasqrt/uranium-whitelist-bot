package uranium.nz.bot.ui;

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
import uranium.nz.bot.database.WhitelistManager;

public class UIListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("whitelist")) return;

        OptionMapping addOption = event.getOption("add");
        if (addOption != null) {
            handleWhitelistAddCommand(event, addOption);
        } else {
            handleWhitelistCommand(event);
        }
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session != null) {
            event.reply("У вас вже є активна сесія.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        session = new UI.Session(event.getUser().getIdLong(), event.getChannel().getIdLong(), 0L, UIStates.ROOT);
        UI.Session finalSession = session;
        event.getHook().sendMessage(UIMessages.root()).queue(message -> {
            finalSession.setMessageId(message.getIdLong());
            UI.UIMemory.putSession(event.getUser().getIdLong(), finalSession);
        });
    }

    private void handleWhitelistAddCommand(SlashCommandInteractionEvent event, OptionMapping usernameOption) {
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.reply("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").setEphemeral(true).queue();
            return;
        }

        UIStates currentState = session.getCurrentState();
        if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME) {
            event.reply("UI зараз не в режимі додавання користувача, закрийте це вікно та спробуйте ще раз.").setEphemeral(true).queue();
            return;
        }

        String username = usernameOption.getAsString();
        Member targetMember = session.getSelectedMember();

        boolean success;
        String replyMessage;
        if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
            success = WhitelistManager.addMain(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Користувач вже існує або це помилка бази данних.";
        } else {
            success = WhitelistManager.addTwin(targetMember.getIdLong(), username);
            replyMessage = success ? String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Твінк акаунт вже існує або основний аккаунт не був вказаний.";
        }

        event.reply(replyMessage).setEphemeral(true).queue();

        if (success) {
            session.changeState(UIStates.ROOT);
            event.getChannel().retrieveMessageById(session.getMessageId()).queue(message -> {
                message.editMessage(MessageEditData.fromCreateData(UIMessages.root())).queue();
            });
        }
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
                e.getMessage().delete().queue();
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
        } else {
            e.reply("Ця дія ще не реалізована.").setEphemeral(true).queue();
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
            default -> UIMessages.root();
        };
    }
}
