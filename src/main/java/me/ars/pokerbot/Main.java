package me.ars.pokerbot;

import java.io.IOException;
import java.io.File;
import java.util.logging.Logger;

import com.moandjiezana.toml.Toml;

import me.ars.pokerbot.config.BotConfig;
import me.ars.pokerbot.irc.KittehBot;

public class Main {
    public static void main(String[] args) {
        final Toml defaults = new Toml().read(new File("config.default.toml"));
        final File configFile = new File("config.toml");
        if (!configFile.exists()) {
            System.err.println("'config.toml' is missing.");
            System.err.println("Please refer to 'config.example.toml' and create your own configuration.");
            return;
        }
        final BotConfig config = new Toml(defaults).read(configFile).to(BotConfig.class);
        KittehBot bot = new KittehBot(config);
        bot.setVerbose(config.irc.verbose);
        bot.connect(
                config.irc.server,
                config.irc.port,
                config.irc.serverPassword
        );
        bot.joinGameChannel(config.irc.channel, config.irc.channelPassword);
    }
}
