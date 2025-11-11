package uranium.nz.bot.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uranium.nz.bot.database.WhitelistManager;

public class JoinLeaveListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();
        if (WhitelistManager.isUserWhitelisted(userId)) {
            WhitelistManager.setOnServerStatus(userId, false);
            System.out.printf("Whitelisted user %s (%d) left the server. on_server false.%n", user.getAsTag(), userId);
        }
    }
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();

        if (WhitelistManager.isUserWhitelisted(userId)) {
            WhitelistManager.setOnServerStatus(userId, true);
            System.out.printf("Whitelisted user %s (%d) joined the server. on_server true.%n", user.getAsTag(), userId);
        }
    }
}
