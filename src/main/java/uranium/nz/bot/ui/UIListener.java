package uranium.nz.bot.ui;

import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
            handleWhitelistAddCommand(event, addOption);
        } else {
            handleWhitelistCommand(event);
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

    private void handleWhitelistAddCommand(SlashCommandInteractionEvent event, OptionMapping usernameOption) {
        event.deferReply(true).queue();
        UI.Session session = UI.UIMemory.getSession(event.getUser().getIdLong());
        if (session == null) {
            event.getHook().sendMessage("У вас немає активної сесії. Будь ласка, відкрийте її за допомогою `/whitelist`.").queue();
            return;
        }

        UIStates currentState = session.getCurrentState();
        if (currentState != UIStates.AWAITING_MAIN_USERNAME && currentState != UIStates.AWAITING_TWIN_USERNAME) {
            event.getHook().sendMessage("Ви не в тому стані, щоб додавати користувача. Почніть спочатку.").queue();
            return;
        }

        String username = usernameOption.getAsString();
        Member targetMember = session.getSelectedMember();

        String reply;
        if (currentState == UIStates.AWAITING_MAIN_USERNAME) {
            boolean success = WhitelistManager.addMain(targetMember.getIdLong(), username);
            reply = success ? String.format("Основний акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Не вдалося додати основний акаунт. Можливо, такий нік вже існує.";
        } else {
            boolean success = WhitelistManager.addTwin(targetMember.getIdLong(), username);
            reply = success ? String.format("Твінк акаунт для %s з ніком `%s` успішно додано.", targetMember.getAsMention(), username)
                    : "Не вдалося додати твінк акаунт. Можливо, такий нік вже існує.";
        }
        event.getHook().sendMessage(reply).queue();

        session.changeState(UIStates.ROOT);
        TextChannel channel = event.getJDA().getTextChannelById(session.getChannelId());
        if (channel != null) {
            channel.retrieveMessageById(session.getMessageId()).queue(message -> {
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
            case "wl:remove_main" -> {
                WhitelistManager.removeMain(session.getSelectedMember().getIdLong());
                session.changeState(UIStates.ROOT);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.root())).queue();
                e.getHook().sendMessage("Основний акаунт для " + session.getSelectedMember().getAsMention() + " видалено.").setEphemeral(true).queue();
            }
            case "wl:remove_twin" -> {
                WhitelistManager.removeTwin(session.getSelectedMember().getIdLong());
                session.changeState(UIStates.ROOT);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.root())).queue();
                e.getHook().sendMessage("Твінк акаунти для " + session.getSelectedMember().getAsMention() + " видалено.").setEphemeral(true).queue();
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

                boolean isExistingUser = WhitelistManager.isUserWhitelisted(selectedMember.getIdLong());

                e.editMessage(MessageEditData.fromCreateData(UIMessages.showAddUserOptions(selectedMember, isExistingUser))).queue();
            }
            case REMOVE_USER -> {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_REMOVE_TYPE);
                boolean hasMain = WhitelistManager.hasMain(selectedMember.getIdLong());
                boolean hasTwins = WhitelistManager.hasTwin(selectedMember.getIdLong());
                e.editMessage(MessageEditData.fromCreateData(UIMessages.showRemoveUserOptions(selectedMember, hasMain, hasTwins))).queue();
            }
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
            case AWAITING_REMOVE_TYPE -> UIMessages.showRemoveUserOptions(member, true, true);
            case AWAITING_MAIN_USERNAME -> UIMessages.promptForMainUsername(member);
            case AWAITING_TWIN_USERNAME -> UIMessages.promptForTwinUsername(member);
            default -> UIMessages.root();
        };
    }
}
