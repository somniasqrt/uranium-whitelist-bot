package uranium.nz.bot.ui;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

public class UIListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        if (!commandName.equals("whitelist")) return;
        event.deferReply(true).queue();
        UI.Session session = new UI.Session(
                event.getUser().getIdLong(), event.getChannel().getIdLong(), 0L, UIStates.ROOT);
        event.getHook().sendMessage(UIMessages.root()).queue(message -> {
            session.setMessageId(message.getIdLong());
            UI.UIMemory.putSession(event.getUser().getIdLong(), session);
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        String componentId = e.getComponentId();
        if (!componentId.startsWith("wl:")) return;
        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId()) {
            e.reply("This is not your UI or the session has expired.").setEphemeral(true).queue();
            return;
        }
        session.touch();
        switch (componentId) {
            case "wl:add":
                session.changeState(UIStates.ADD_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.addUser())).queue();
                break;
            case "wl:prev":
                UIStates previousState = session.getPreviousState();
                e.editMessage(MessageEditData.fromCreateData(getMessageForState(previousState))).queue();
                break;
            case "wl:close":
                UI.UIMemory.removeSession(e.getUser().getIdLong());
                e.deferEdit().queue();
                e.getHook().deleteOriginal().queue();
                break;
            case "wl:remove":
                session.changeState(UIStates.REMOVE_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.removeUser())).queue();
                break;
            case "wl:find":
                session.changeState(UIStates.FIND_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.findUser())).queue();
                break;
            case "wl:change":
                session.changeState(UIStates.CHANGE_USER);
                e.editMessage(MessageEditData.fromCreateData(UIMessages.changeUser())).queue();
                break;
            case "wl:add_main":
            case "wl:add_twin":
                TextInput nicknameInput = TextInput.create("nickname", TextInputStyle.SHORT)
                        .setPlaceholder("name here")
                        .setRequired(true)
                        .build();

                String modalId = componentId.equals("wl:add_main") ? "modal:add_main" : "modal:add_twin";

                Modal modal = Modal.create(modalId, "Add to Whitelist")
                        .addComponents((ModalTopLevelComponent) nicknameInput)
                        .build();
                e.replyModal(modal).queue();
                break;
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent e) {
        if (!"wl:user".equals(e.getComponentId())) return;
        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId()) {
            e.reply("This is not your UI or the session has expired.").setEphemeral(true).queue();
            return;
        }
        session.touch();
        IMentionable selectedEntity = e.getValues().stream().findFirst().orElse(null);
        if (!(selectedEntity instanceof Member)) {
            e.reply("No user selected.").setEphemeral(true).queue();
            return;
        }
        Member selectedMember = (Member) selectedEntity;
        UIStates currentState = session.getCurrentState();
        switch (currentState) {
            case ADD_USER: {
                session.setSelectedMember(selectedMember);
                session.changeState(UIStates.AWAITING_ADD_TYPE);

                // TODO: Replace this with your actual database/whitelist check
                boolean isExistingUser = false; // whitelistService.isUserWhitelisted(selectedMember.getIdLong());

                e.editMessage(MessageEditData.fromCreateData(UIMessages.showAddUserOptions(selectedMember, isExistingUser))).queue();
                break;
            }
            case REMOVE_USER:
                // TODO: remove
                e.reply("Successfully removed " + selectedMember.getAsMention() + " from the whitelist!").setEphemeral(true).queue();
                break;
            case FIND_USER:
                // TODO: find
                e.reply("Found info for " + selectedMember.getAsMention() + ": ...").setEphemeral(true).queue();
                break;
            case CHANGE_USER:
                // TODO: change
                e.reply("Changed " + selectedMember.getAsMention() + ".").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent e) {
        String modalId = e.getModalId();
        if (!modalId.startsWith("modal:add")) return;

        UI.Session session = UI.UIMemory.getSession(e.getUser().getIdLong());
        if (session == null || e.getUser().getIdLong() != session.getUserId() || session.getSelectedMember() == null) {
            e.reply("This is not your UI, the session has expired, or no user was selected.").setEphemeral(true).queue();
            return;
        }

        String nickname = e.getValue("nickname").getAsString();
        Member targetMember = session.getSelectedMember();

        switch (modalId) {
            case "modal:add_main":
                // TODO: Your logic to add the main account with the nickname
                e.reply(String.format("Successfully added main account for %s with nickname `%s`.", targetMember.getAsMention(), nickname)).setEphemeral(true).queue();
                break;

            case "modal:add_twin":
                // TODO: Your logic to add the twin account with the nickname
                e.reply(String.format("Successfully added twin account for %s with nickname `%s`.", targetMember.getAsMention(), nickname)).setEphemeral(true).queue();
                break;
        }
    }

    private MessageCreateData getMessageForState(UIStates state) {
        switch (state) {
            case ADD_USER: return UIMessages.addUser();
            case REMOVE_USER: return  UIMessages.removeUser();
            case FIND_USER: return UIMessages.findUser();
            case CHANGE_USER: return UIMessages.changeUser();
            case AWAITING_ADD_TYPE:
            default: return UIMessages.root();
        }
    }
}
