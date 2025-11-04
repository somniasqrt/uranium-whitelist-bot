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
        String commandName = event.getName();
        if (!commandName.equals("whitelist")) return;

        OptionMapping addOption = event.getOption("add");
        if (addOption != null) {
            handleWhitelistAddCommand(event);
        } else {
            handleWhitelistCommand(event);
        }
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        Session session = UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.deferReply(false).queue();
            session = new Session(
                    event.getUser().getIdLong(), event.getChannel().getIdLong(), 0L, UIStates.ROOT);
            Session finalSession = session;
            event.getHook().sendMessage(UIMessages.root()).queue(message -> {
                finalSession.setMessageId(message.getIdLong());
                UIMemory.putSession(event.getUser().getIdLong(), finalSession);
            });
        } else {
            event.reply("У вас вже є активна сесія.").setEphemeral(true).queue();
        }
    }

    private void handleWhitelistAddCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        Session session = UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.getHook().sendMessage("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").queue();
            return;
        }

        UIStates currentState = session.getCurrentState();
        if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME) {
            event.getHook().sendMessage("UI зараз не в режимі додавання користувача, закрийте це вікно та спробуйте ще раз").queue();
            return;
        }

        OptionMapping usernameOption = event.getOption("add");
        if (usernameOption == null) {
            event.getHook().sendMessage("Ви повинні вказати нік.").queue();
            return;
        }
        String username = usernameOption.getAsString();
        Member targetMember = session.getSelectedMember();

        boolean success;
        String replyMessage;
        if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
            success = WhitelistManager.addMain(targetMember.getIdLong(), username);
            if (success) {
                replyMessage = String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username);
            } else {
                replyMessage = "Користувач вже існує або це помилка бази данних";
            }
        } else {
            success = WhitelistManager.addTwin(targetMember.getIdLong(), username);
            if (success) {
                replyMessage = String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username);
            } else {
                replyMessage = "Твінк акаунт вже існує або основний аккаунт не був вказаний";
            }
        }

        if (success) {
            session.changeState(UIStates.ROOT);
            event.getChannel().retrieveMessageById(session.getMessageId()).queue(message -> {
                message.editMessage(MessageEditData.fromCreateData(UIMessages.root())).queue(
                        s -> event.getHook().sendMessage(replyMessage).queue(),
                        e -> event.getHook().sendMessage(replyMessage + " (Failed to update UI panel)").queue()
                );
            }, e -> event.getHook().sendMessage(replyMessage + " (Could not find UI panel to update)").queue());
        } else {
            event.getHook().sendMessage(replyMessage).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        String componentId = e.getComponentId();
        if (!componentId.startsWith("wl:")) return;
        Session session = UIMemory.getSession(e.getUser().getIdLong());
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
                UIStates previousState = session.getPreviousState();
                e.editMessage(MessageEditData.fromCreateData(getMessageForState(previousState, session.getSelectedMember()))).queue();
            }
            case "wl:close" -> {
                UIMemory.removeSession(e.getUser().getIdLong());
                e.editMessage(MessageEditData.fromContent("Сесію закрито.")).setComponents().queue();
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
        Session session = UIMemory.getSession(e.getUser().getIdLong());
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
            // TODO: Handle other cases like remove, find, change
            e.reply("Ця дія ще не реалізована.").setEphemeral(true).queue();
        }
    }

    private MessageCreateData getMessageForState(UIStates state, Member member) {
        return switch (state) {
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
