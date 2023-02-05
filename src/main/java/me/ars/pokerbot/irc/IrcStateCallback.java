package me.ars.pokerbot.irc;

import me.ars.pokerbot.poker.Card;
import me.ars.pokerbot.poker.Hand;
import me.ars.pokerbot.poker.Player;
import me.ars.pokerbot.poker.StateCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IrcStateCallback implements StateCallback {

    private final Irc ircBot;
    private final String channel;

    public IrcStateCallback(Irc ircBot, String channel) {
        this.ircBot = ircBot;
        this.channel = channel;
    }

    private String moneyString(int amount) {
        return Formatting.BOLD + Formatting.COLOR_GREEN + "$" + amount + Formatting.CLEAR;
    }

    private String renderCard(Card card) {
        final String suit;
        switch (card.getSuit()) {
            case SPADES:
                suit = Formatting.SPADES;
                break;
            case HEARTS:
                suit = Formatting.HEARTS;
                break;
            case DIAMONDS:
                suit = Formatting.DIAMONDS;
                break;
            case CLUBS:
                suit = Formatting.CLUBS;
                break;
            default:
                throw new IllegalStateException();
        }
        final String color = (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS) ? Formatting.COLOR_RED
                : Formatting.COLOR_BLACK;

        final String valueStr;

        switch (card.getValue()) {
            case 11:
                valueStr = "J";
                break;
            case 12:
                valueStr = "Q";
                break;
            case 13:
                valueStr = "K";
                break;
            case 14:
                valueStr = "A";
                break;
            default:
                valueStr = Integer.toString(card.getValue());
        }

        return Formatting.BOLD + color + valueStr + suit + Formatting.CLEAR;
    }

    private String renderNick(Player player) {
        return Formatting.BOLD + player.getName() + Formatting.CLEAR;
    }

    private String renderHand(Hand hand) {
        return Formatting.BOLD + Arrays.stream(hand.getBestHand()).map(this::renderCard)
                .collect(Collectors.joining(", "))
                + " (" + hand.getHandType().toString() + ")" + Formatting.CLEAR;
    }

    @Override
    public void playerCalled(Player player, int money) {
        ircBot.message(channel, renderNick(player) + " called! (" + moneyString(money) + ")");
    }

    @Override
    public void playerRaised(Player player, int newRaise) {
        ircBot.message(channel, renderNick(player) + " raised " + moneyString(newRaise) + ".");
    }

    @Override
    public void playerChecked(Player player) {
        // Too verbose. Skip
    }

    @Override
    public void announce(String message) {
        ircBot.message(channel, message);
    }

    @Override
    public void updateTable(List<Card> table, int pot, Player currentPlayer) {
        final String tableStr = table.isEmpty() ? "no cards" : table.stream()
                .map(this::renderCard).collect(Collectors.joining(", "));
        if (currentPlayer == null) {
            ircBot.message(channel, "On the table: " + tableStr + " || In the pot: " + moneyString(pot));
        } else {
            ircBot.message(channel, "On the table: " + tableStr + " || In the pot: " + moneyString(pot) +
                    " || Current player: " + renderNick(currentPlayer));
        }
    }

    @Override
    public void mustCallRaise(Player player, int amountOwed) {
        ircBot.message(channel, renderNick(player) + " must at least call last raise (" + moneyString(amountOwed) + ").");
    }

    @Override
    public void playerCannotRaise(Player player, int money) {
        ircBot.message(channel, renderNick(player) + " doesn't have enough money to make the raise. They only have " + moneyString(money) + ".");
    }

    @Override
    public void playerAllin(Player player) {
        ircBot.message(channel, renderNick(player) + " goes all in!");
    }

    @Override
    public void playerFolded(Player player) {
        // Too verbose. Skip.
    }

    @Override
    public void playerCashedOut(Player player, int money) {
        ircBot.message(channel, renderNick(player) + " cashed out with " + moneyString(money) + "!");
    }

    @Override
    public void showPlayerCards(Player player, Card card1, Card card2, Card spyCard) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[").append(channel).append("] Your cards: ").append(renderCard(card1)).append(", ").append(renderCard(card2));
        if (spyCard != null) {
            sb.append(". Spied card: ").append(renderCard(spyCard));
        }
        ircBot.message(player, sb.toString());
    }

    @Override
    public void showPlayers(Map<Player, Integer> players) {
        ircBot.message(channel, players.keySet().stream()
                .map(player -> "[" + renderNick(player) + " - " + moneyString(players.get(player)) + "]")
                .collect(Collectors.joining(" ")));
    }

    @Override
    public void revealPlayers(Map<Player, List<Card>> reveal) {
        ircBot.message(channel, reveal.keySet().stream()
                .map(player -> "[" + renderNick(player) + " - " +
                        renderCard(reveal.get(player).get(0)) + ", " +
                        renderCard(reveal.get(player).get(1)) + "]")
                .collect(Collectors.joining(" ")));
    }

    @Override
    public void declareWinner(Player player, Hand winningHand, int pot) {
        final StringBuilder sb = new StringBuilder();
        sb.append(renderNick(player)).append(" wins ").append(moneyString(pot));
        if (winningHand != null) {
            sb.append(" with the hand ").append(renderHand(winningHand));
        }
        sb.append("!");
        ircBot.message(channel, sb.toString());
    }

    @Override
    public void declareSplitPot(List<Player> winners, Hand.HandType handType, int pot) {
        ircBot.message(channel,
                "Split pot between "
                        + winners.stream().map(Player::getName).collect(Collectors.joining(", "))
                        + " (each with a " + handType + ").");
    }

    @Override
    public void declarePlayerTurn(Player player) {
        ircBot.message(channel, renderNick(player) + "'s turn!");
    }

    @Override
    public void collectAnte(int ante) {
        ircBot.message(channel, "Collecting a " + moneyString(ante) + " ante from each player...");
    }

    @Override
    public void collectBlinds(Player bigBlindPlayer, int bigBlind, Player smallBlindPlayer, int smallBlind) {
        ircBot.message(channel, "Collecting blinds (" + moneyString(bigBlind) + " from " + renderNick(bigBlindPlayer) + ", " + moneyString(smallBlind) + " from " + renderNick(smallBlindPlayer) + ")");
    }

    @Override
    public void gameEnded(List<Player> oldPlayers) {
        ircBot.gameEnded(oldPlayers);
    }
}
