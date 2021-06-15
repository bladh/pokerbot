package me.ars.pokerbot;

import me.ars.pokerbot.poker.Player;

public class Main {
    public static void main(String[] args) {
        final Database database = new Database();
        database.prepare();
        final Player jocke = new Player("jocke", 200);
        database.addNewPlayer(jocke, "derp");
        System.out.println("...");
        final Player a = database.getPlayer("jocke", "derp");
        System.out.println("Got " + a.toString() + " from db");
        /*
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
         */
    }
}
