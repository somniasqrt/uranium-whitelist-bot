package uranium.nz.bot;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Command extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        if (!commandName.equals("whitelist")) return;
        event.reply("whitelist")
                .setComponents(
                        ActionRow.of(Button.primary("w_add", "add"), Button.secondary("w_remove", "remove")),
                                ActionRow.of(Button.danger("w_close", "close")))
                .setEphemeral(true)
                .queue();

    }
}
