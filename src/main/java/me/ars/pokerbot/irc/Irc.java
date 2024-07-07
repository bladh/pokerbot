package me.ars.pokerbot.irc;

import me.ars.pokerbot.poker.PlayerIdentifier;

import java.util.List;

public interface Irc {
    void message(String channel, String message);

    /**
     * Sends a message to a player
     * @param player
     * @param message
     */
    void message(PlayerIdentifier player, String message);

    void gameEnded(List<PlayerIdentifier> players);
}
