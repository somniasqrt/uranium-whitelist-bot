package uranium.nz.bot;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import uranium.nz.bot.database.DatabaseManager;
import uranium.nz.bot.listeners.JoinLeaveListener;
import uranium.nz.bot.ui.UIListener;

public class Bot {

    @Getter
    public static JDA jda;
    public static Dotenv dotenv;

    public static Guild guild;

    public static void init() {
        dotenv = Dotenv.configure().directory("src/main/resources").ignoreIfMissing().load();
        String token = dotenv.get("DISCORD_TOKEN");

        if (token == null || !token.startsWith("M")) {
            System.out.println("No token found in .env file");
            dotenv = Dotenv.configure().directory("./").filename(".env.test").ignoreIfMissing().load();
            token = dotenv.get("DISCORD_TOKEN");
        }
        if (token == null || !token.startsWith("M")) {
            throw new IllegalStateException("No DISCORD_TOKEN found in .env (classpath) or .env.test (local).");
        }
        System.out.println("Starting bot...");
        try {
            build(token);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Bot started successfully!");

        DatabaseManager.init();

        guild = jda.getGuildById("1423544904574828668");


        if (guild != null) {
            guild.updateCommands()
                 .addCommands(
                         Commands.slash("whitelist", "Керування вайтлистом")
                                 .addOption(OptionType.STRING, "name", "Ігровий нік для додавання або зміни", false)
                                 .addOption(OptionType.STRING, "find", "Знайти користувача у вайтлисті за ніком", false)
                                 .addOption(OptionType.STRING, "remove", "Видалити користувача з вайтлисту за ID", false))
                    .queue();

            System.out.println("Commands updated for guild " + guild.getName());
        } else {
            System.out.println("Guild not found");
        }
    }
    public static void stop() {
        System.out.println("Shutting down...");
        if (jda != null) {
            DatabaseManager.close();
            jda.shutdown();
            jda.shutdownNow();
            System.out.println("Shut down successfully, bye!");
        }
    }
    public static void build(String token) throws InterruptedException {
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .disableCache(CacheFlag.ACTIVITY)
                .addEventListeners(new UIListener(), new JoinLeaveListener())
                .setToken(token)
                .build().awaitReady();
    }

}
