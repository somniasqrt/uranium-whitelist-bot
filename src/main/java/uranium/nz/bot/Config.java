package uranium.nz.bot;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Config {

    public static final Dotenv dotenv;

    static {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            System.out.println("'.env' file not found. Creating a template file...");
            try (FileWriter writer = new FileWriter(envFile)) {
                writer.write("# Discord Bot Settings\n");
                writer.write("DISCORD_TOKEN=YOUR_DISCORD_BOT_TOKEN_HERE\n");
                writer.write("GUILD_ID=YOUR_DISCORD_SERVER_ID_HERE\n");
                writer.write("WHITELIST_ROLE=YOUR_WHITELIST_ROLE_ID_HERE\n");
                writer.write("\n");
                writer.write("# Database Settings\n");
                writer.write("DB_HOST=localhost\n");
                writer.write("DB_PORT=5432\n");
                writer.write("DB_NAME=YOUR_DATABASE_NAME\n");
                writer.write("DB_USER=YOUR_DATABASE_USERNAME\n");
                writer.write("DB_PASSWORD=YOUR_DATABASE_PASSWORD\n");
                System.out.println("Successfully created '.env'. Please fill it and restart the bot");
            } catch (IOException e) {
                System.err.println("Error creating '.env' file: " + e.getMessage());
            }
            System.exit(0);
        }
        dotenv = Dotenv.configure().load();
    }

    public static String get(String key) {
        return dotenv.get(key);
    }

    public static String get(String key, String defaultValue) {
        return dotenv.get(key, defaultValue);
    }
}
