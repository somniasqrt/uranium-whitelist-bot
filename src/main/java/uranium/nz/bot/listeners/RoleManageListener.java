package uranium.nz.bot.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import uranium.nz.bot.database.WhitelistManager;

import java.util.List;

public class RoleManageListener extends ListenerAdapter {

    private final long whitelistRoleId;
    public RoleManageListener(long whitelistRoleId) {
        this.whitelistRoleId = whitelistRoleId;
    }
    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        User user = event.getUser();
        List<Role> removedRoles = event.getRoles();

        boolean wasWhitelistRoleRemoved = removedRoles.stream().anyMatch(role -> role.getIdLong() == whitelistRoleId);

        if (wasWhitelistRoleRemoved) {
            if (WhitelistManager.removeMain(user.getIdLong())) {
                System.out.printf("Player role removed from %s (%d). User removed from whitelist.%n", user.getAsTag(), user.getIdLong());
            }
        }
    }
    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        Member member = event.getMember();
        List<Role> addedRoles = event.getRoles();
        addedRoles.stream()
                .filter(role -> role.getIdLong() == whitelistRoleId && !WhitelistManager.isUserWhitelisted(member.getIdLong()))
                .findFirst()
                .ifPresent(role -> {
                    event.getGuild().removeRoleFromMember(member, role).queue();
                    System.out.printf("Player role was added to non-whitelisted user %s (%d). Removing role.%n", member.getUser().getAsTag(), member.getIdLong());
                });
    }
}