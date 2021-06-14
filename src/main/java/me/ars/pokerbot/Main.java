package me.ars.pokerbot;

import java.io.IOException;
import java.io.File;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.TrustingSSLSocketFactory;
import com.moandjiezana.toml.Toml;

import me.ars.pokerbot.config.BotConfig;

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
        IrcBot bot = new IrcBot(config);
        bot.setVerbose(config.irc.verbose);
        System.out.println(config.irc.server + ":" + config.irc.port);

        try {
            if (config.irc.serverPassword == null) {
                bot.connect(
                    config.irc.server,
                    config.irc.port,
                    new TrustingSSLSocketFactory()
                );
            } else {
                bot.connect(
                    config.irc.server, config.irc.port,
                    config.irc.serverPassword,
                    new TrustingSSLSocketFactory()
                );
            }
            bot.joinGameChannel(config.irc.channel, config.irc.channelPassword);
        } catch (IrcException | IOException e) {
            e.printStackTrace();
        }
    }
}
