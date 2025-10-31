package uranium.nz;

import uranium.nz.bot.Bot;

public class Main {
    static void main() {
        Bot.init();
        Runtime.getRuntime().addShutdownHook(new Thread(Bot::stop));
        try {
           Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
