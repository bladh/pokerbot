package me.ars.pokerbot.irc;

import me.ars.pokerbot.poker.Player;

public class IrcPlayer extends Player {

    private String nick;
    private String login;
    private String host;

    public IrcPlayer(String identifier) {
        super(identifier);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getName() {
        return nick;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}
