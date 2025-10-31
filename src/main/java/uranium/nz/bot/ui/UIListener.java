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

public class UIListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        switch (commandName) {
            case "whitelist" -> handleWhitelistCommand(event);
            case "wl" -> {
                if ("add".equals(event.getSubcommandName())) {
                    handleWlAddCommand(event);
                }
            }
        }
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.deferReply(true).queue();
            session = new UI.Session(
                    event.getUser().getIdLong(), event.getChannel().getIdLong(), 0L, UIStates.ROOT);
            UI.Session finalSession = session;
            event.getHook().sendMessage(UIMessages.root()).queue(message -> {
                finalSession.setMessageId(message.getIdLong());
                UI.UIMemory.putSession(event.getUser().getIdLong(), finalSession);
            });
        } else {
            event.reply("У вас вже є активна сесія.").setEphemeral(true).queue();
        }
    }

    private void handleWlAddCommand(SlashCommandInteractionEvent event) {
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.reply("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").setEphemeral(true).queue();
            return;
        }

        UIStates currentState = session.getCurrentState();
        if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME) {
            event.reply("Ви не в тому стані, щоб додавати користувача. Почніть спочатку.").setEphemeral(true).queue();
            return;
        }

        OptionMapping usernameOption = event.getOption("username");
        if (usernameOption == null) {
            event.reply("Ви повинні вказати нік.").setEphemeral(true).queue();
            return;
        }
        String username = usernameOption.getAsString();
        Member targetMember = session.getSelectedMember();

        if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
            // TODO: Your logic to add the main account with the username
            event.reply(String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)).setEphemeral(true).queue();
        } else {
            // TODO: Your logic to add the twin account with the username
            event.reply(String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)).setEphemeral(true).queue();
        }
        session.changeState(UIStates.ROOT);
        event.getChannel().retrieveMessageById(session.getMessageId()).queue(message -> {
            message.editMessage(MessageEditData.fromCreateData(UIMessages.root())).queue();
        });
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
                UIStates previousState = session.getPreviousState();
                e.editMessage(MessageEditData.fromCreateData(getMessageForState(previousState, session.getSelectedMember()))).queue();
            }
            case "wl:close" -> {
                UI.UIMemory.removeSession(e.getUser().getIdLong());
                e.deferEdit().queue();
                e.getHook().deleteOriginal().queue();
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
        switch (currentState) {
            case ADD_USER -> {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_ADD_TYPE);

                // TODO: Replace this with your actual database/whitelist check
                boolean isExistingUser = false; // whitelistService.isUserWhitelisted(selectedMember.getIdLong());

                e.editMessage(MessageEditData.fromCreateData(UIMessages.showAddUserOptions(selectedMember, isExistingUser))).queue();
            }
            case REMOVE_USER -> // TODO: remove
                    e.reply("Успішно видалено " + selectedMember.getAsMention() + " з вайтлиста!").setEphemeral(true).queue();
            case FIND_USER -> // TODO: find
                    e.reply("Інформація про " + selectedMember.getAsMention() + ": ...").setEphemeral(true).queue();
            case CHANGE_USER -> // TODO: change
                    e.reply("Змінено " + selectedMember.getAsMention() + ".").setEphemeral(true).queue();
        }
    }

    private MessageCreateData getMessageForState(UIStates state, Member member) {
        return switch (state) {
            case ADD_USER -> UIMessages.addUser();
            case REMOVE_USER -> UIMessages.removeUser();
            case FIND_USER -> UIMessages.findUser();
            case CHANGE_USER -> UIMessages.changeUser();
            case AWAITING_ADD_TYPE -> UIMessages.showAddUserOptions(member, false);
            case AWAITING_MAIN_USERNAME -> UIMessages.promptForMainUsername(member);
            case AWAITING_TWIN_USERNAME -> UIMessages.promptForTwinUsername(member);
            default -> UIMessages.root();
        };
    }
}
