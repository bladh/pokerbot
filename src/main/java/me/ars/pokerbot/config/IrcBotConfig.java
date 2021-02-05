package me.ars.pokerbot.config;

public class IrcBotConfig {
    public class Irc {
        public String server;
        public Integer port;
        public String serverPassword;
        public String nick;
        public String channel;
        public String channelPassword;
        public String adminPassword;
        public Character commandPrefix;
        public Boolean verbose = false;
    }

    public Irc irc;
    public GameConfig game;
}


