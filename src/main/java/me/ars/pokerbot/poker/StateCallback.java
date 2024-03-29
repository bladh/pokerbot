package me.ars.pokerbot.poker;

import java.util.List;
import java.util.Map;

public interface StateCallback {
    /**
     * A player has called a bet. If the player didn't have enough money, then [money] will be lower than [owed].
     *
     * @param player  Player who called
     * @param money   Amount called
     */
    void playerCalled(Player player, int money);

    /**
     * A player has raised a bet.
     *
     * @param player   Player who raised
     * @param newRaise Amount that was raised
     */
    void playerRaised(Player player, int newRaise);

    /**
     * A player has checked.
     *
     * @param player Checking player
     */
    void playerChecked(Player player);

    /**
     * Announce a message to all players.
     *
     * @param message Message to announce
     */
    void announce(String message);

    /**
     * Update what's shown to be on the table.
     *
     * @param table         List of cards on the table (may be empty)
     * @param pot           Current pot on the table
     * @param currentPlayer Current players turn
     */
    void updateTable(List<Card> table, int pot, Player currentPlayer);

    /**
     * Notify that a player must call a raise.
     *
     * @param player     Player who needs to call
     * @param amountOwed Amount of money needed to call
     */
    void mustCallRaise(Player player, int amountOwed);

    /**
     * Announce that a player could not raise the specified bet.
     *
     * @param player Player who tried to bet
     * @param money  Amount of money they actually had
     */
    void playerCannotRaise(Player player, int money);

    /**
     * A player has gone all in.
     *
     * @param player Player who went all in.
     */
    void playerAllin(Player player);

    /**
     * A player has folded.
     *
     * @param player Folding player
     */
    void playerFolded(Player player);

    /**
     * A player cashed out and left the table.
     *
     * @param player Cashing out player
     * @param money Amount of money they walked away with
     */
    void playerCashedOut(Player player, int money);

    /**
     * Show the player the two cards they were dealt.
     *
     * @param player Player receiving their cards
     * @param card1 First card
     * @param card2 Second card
     * @param spycard If playing with spycards
     */
    void showPlayerCards(Player player, Card card1, Card card2, Card spycard);

    /**
     * Display the currently playing players and the money they have
     *
     * @param players Map of players to how much money they have
     */
    void showPlayers(Map<Player, Integer> players);

    /**
     * Reveals the hands of the supplied players.
     *
     * @param reveal Players mapped to their own cards
     */
    void revealPlayers(Map<Player, List<Card>> reveal);

    /**
     * Declare that a player has won the pot
     *
     * @param winner      Winning player
     * @param winningHand Their winning hand
     * @param pot         Money they've won from the pot
     */
    void declareWinner(Player winner, Hand winningHand, int pot);

    /**
     * Declare that there are multiple winners splitting the pot
     *
     * @param winners  List of winning players
     * @param handType The hand type they had in common
     * @param pot      The pot they are splitting
     */
    void declareSplitPot(List<Player> winners, Hand.HandType handType, int pot);

    /**
     * Declares who's turn it is.
     *
     * @param player Current player
     */
    void declarePlayerTurn(Player player);

    /**
     * Declare that ante is being collected
     *
     * @param ante How much money the ante is
     */
    void collectAnte(int ante);

    /**
     * Declare that the blinds are being collected
     *
     * @param bigBlindPlayer   The player that pays the big blind
     * @param bigBlind         How big the big blind is
     * @param smallBlindPlayer The player that pays the small blind
     * @param smallBlind       How big the small blind is
     */
    void collectBlinds(Player bigBlindPlayer, int bigBlind, Player smallBlindPlayer, int smallBlind);

    /**
     * Declare that the game has ended.
     *
     * @param oldPlayers Players that have left the ended game
     */
    void gameEnded(List<Player> oldPlayers);
}
