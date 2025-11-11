package uranium.nz;

import uranium.nz.bot.Bot;

import java.util.TimeZone;

public class Main {
     static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        Bot.init();
        Runtime.getRuntime().addShutdownHook(new Thread(Bot::stop));
        try {
           Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
