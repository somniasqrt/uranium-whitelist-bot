package uranium.nz.bot;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import uranium.nz.bot.database.DatabaseManager;
import uranium.nz.bot.database.WhitelistManager;
import uranium.nz.bot.listeners.JoinLeaveListener;
import uranium.nz.bot.ui.UIListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bot {

    @Getter
    public static JDA jda;

    public static Guild guild;
    public static WhitelistManager whitelistManager;
    public static ExecutorService executor = Executors.newCachedThreadPool();

    public static void init() {
        String token = Config.get("DISCORD_TOKEN");
        String guildId = Config.get("GUILD_ID");
        String whitelistRoleIdStr = Config.get("WHITELIST_ROLE");

        if (token == null || token.equals("YOUR_DISCORD_BOT_TOKEN_HERE")) {
            throw new IllegalStateException("Please set your DISCORD_TOKEN in the .env file.");
        }
        if (guildId == null) {
            throw new IllegalStateException("GUILD_ID is not defined in the .env file.");
        }
        if (whitelistRoleIdStr == null) {
            throw new IllegalStateException("WHITELIST_ROLE is not defined in the .env file.");
        }
        long whitelistRoleId = Long.parseLong(whitelistRoleIdStr);

        System.out.println("Starting bot...");
        whitelistManager = new WhitelistManager(whitelistRoleId);
        try {
            build(token, whitelistManager);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Bot started successfully!");

        DatabaseManager.init();

        guild = jda.getGuildById(guildId);


        if (guild != null) {
            guild.updateCommands()
                 .addCommands(
                         Commands.slash("whitelist", "Керування вайтлистом")
                                 .addOption(OptionType.STRING, "name", "Ігровий нік для додавання або зміни", false)
                                 .addOption(OptionType.STRING, "find", "Знайти користувача у вайтлисті за ніком", false)
                                 .addOption(OptionType.STRING, "remove", "Видалити користувача з вайтлисту за ID", false)
                                 .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                         Commands.slash("ban", "Забанити користувача")
                                 .addOption(OptionType.STRING, "name_or_id", "Ігровий нік або Discord ID", true)
                                 .addOption(OptionType.STRING, "reason", "Причина бану", false)
                                 .addOption(OptionType.STRING, "time", "Тривалість бану (наприклад, 30d, 12h, 1y)", false)
                                 .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                 )
                    .queue();

            System.out.println("Commands updated for guild " + guild.getName());
        } else {
            System.out.println("Guild not found");
        }
    }
    public static void stop() {
        System.out.println("Shutting down...");
        if (jda != null) {
            executor.shutdown();
            DatabaseManager.close();
            jda.shutdown();
            jda.shutdownNow();
            System.out.println("Shut down successfully, bye!");
        }
    }
    public static void build(String token, WhitelistManager whitelistManager) throws InterruptedException {
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .disableCache(CacheFlag.ACTIVITY)
                .addEventListeners(new UIListener(whitelistManager), new JoinLeaveListener(whitelistManager))
                .setToken(token)
                .build().awaitReady();
    }

}
