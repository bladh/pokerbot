package me.ars.pokerbot.irc;

import me.ars.pokerbot.poker.Player;

import java.util.List;

public interface Irc {
    void message(String channel, String message);

    /**
     * Sends a message to a player
     * @param player
     * @param message
     */
    void message(Player player, String message);

    void gameEnded(List<Player> players);
}
