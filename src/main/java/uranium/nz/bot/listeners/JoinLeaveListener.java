package uranium.nz.bot.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uranium.nz.bot.database.DatabaseManager;
import uranium.nz.bot.database.WhitelistManager;

public class JoinLeaveListener extends ListenerAdapter {

    private final WhitelistManager whitelistManager;

    public JoinLeaveListener(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();
        DatabaseManager.getWhitelistedUser(userId).ifPresent(whitelistedUser -> {
            whitelistManager.setOnServerStatus(userId, false);
            System.out.printf("Left server: %s %s %d%n", whitelistedUser.minecraftName(), user.getName(), userId);
        });
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();
        DatabaseManager.getWhitelistedUser(userId).ifPresent(whitelistedUser -> {
            whitelistManager.setOnServerStatus(userId, true);
            whitelistManager.updateUserOnJoin(userId);
            System.out.printf("Joined server: %s %s %d%n", whitelistedUser.minecraftName(), user.getName(), userId);
        });
    }
}
