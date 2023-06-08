package me.ars.pokerbot.poker;

import java.util.*;

public class Pot {

    private static final int MAX_RECURSIONS = 64;

    private final Map<Player, Integer> contributions;
    private final boolean isMainPot;
    private Pot sidePot;
    private int currentBet;

    public Pot() {
        contributions = new HashMap<>();
        currentBet = 0;
        sidePot = null;
        isMainPot = true;
    }

    private Pot(int currentBet) {
        this.contributions = new HashMap<>();
        this.currentBet = currentBet;
        this.sidePot = null;
        this.isMainPot = false;
    }

    private void addContribution(Player player, int money) {
        if (!contributions.containsKey(player)) {
            contributions.put(player, money);
        } else {
            int oldmoney = contributions.get(player);
            contributions.replace(player, oldmoney + money);
        }
    }

    public int getContribution(Player player) {
        return contributions.getOrDefault(player, 0);
    }

    public int getTotalMoney() {
        int money = getMoney();
        if (sidePot != null) {
            money += sidePot.getTotalMoney();
        }
        return money;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getTotalBets() {
        int bet = currentBet;
        if (sidePot != null) {
            bet += sidePot.getTotalBets();
        }
        return bet;
    }

    public int getTotalContribution(Player player) {
        int contribution = 0;
        if (contributions.containsKey(player)) {
            contribution += contributions.get(player);
        }
        if (sidePot != null) {
            contribution += sidePot.getTotalContribution(player);
        }
        return contribution;
    }

    public int getTotalOwed(Player player) {
        final int totalBet = getTotalBets();
        final int contributions = getTotalContribution(player);
        return totalBet - contributions;
    }

    private int getOwed(Player player) {
        final int bet = getCurrentBet();
        final int contributions = getContribution(player);
        return bet - contributions;
    }

    public void newTurn() {
        //currentBet = 0;
        if (sidePot != null) {
            sidePot.newTurn();
        }
    }

    public boolean checkPlayer(Player player) {
        if (player.isAllIn()) {
            return true;
        }
        if (!contributions.containsKey(player)) {
            contributions.put(player, 0);
        }
        return (getTotalOwed(player) == 0);
    }

    public void reset() {
        contributions.clear();
        currentBet = 0;
        sidePot = null;
    }

    public void collectAnte(Player player, int ante) {
        currentBet = ante;
        addContribution(player, player.bet(ante));
        System.out.println("Collecting ante from " + player + ", total paid: " + getTotalContribution(player));
    }

    public int collectBigBlind(Player player, int bigBlind) {
        raise(player, bigBlind);
        System.out.println("Collecting big blind (" + bigBlind + ") from " + player);
        return bigBlind;
    }

    static int calculateSmallBlind(int bigBlind) {
        return (int) Math.ceil(((double) bigBlind) / 2);
    }

    public int collectSmallBlind(Player player, int bigBlind) {
        final int smallBlind = calculateSmallBlind(bigBlind);
        addContribution(player, player.bet(smallBlind));
        System.out.println("Collecting small blind (" + smallBlind + ") from " + player);
        return smallBlind;
    }

    public Set<Player> getParticipants() {
        return contributions.keySet();
    }

    public int getMoney() {
        int cash = 0;
        for(Player player: contributions.keySet()) {
            cash += contributions.get(player);
        }
        return cash;
    }

    public boolean hasSidePot() {
        return sidePot != null;
    }

    private void setBet(int bet) {
        currentBet = bet;
    }

    public boolean isMainPot() {
        return isMainPot;
    }

    public Pot getSidePot() {
        return sidePot;
    }

    public int raise(Player player, int amount) {
        final int totalRaised;
        final int owed;

        if (player.isAllIn()) {
            owed = call(player);
            totalRaised = Math.min(player.getMoney(), amount - owed);
            if (totalRaised < 1) {
                // Not enough money to raise the specified amount, we stop at the call() above.
                return 0;
            }
        } else {
            owed = getTotalOwed(player);
            totalRaised = Math.min(player.getMoney(), amount + owed);
            if (owed > totalRaised + player.getMoney()) {
                // Cannot raise, not enough money. Let player reconsider raise.
                return -1;
            }
        }
        if (sidePot == null) {
            currentBet += amount;
            addContribution(player, player.bet(totalRaised));
            System.out.println(player + " has raised by " + totalRaised);
        } else {
            sidePot.raise(player, totalRaised);
            System.out.println(player + " raised, but its going into a sidepot");
        }
        return totalRaised-owed;
    }

    public void allIn(Player player) {
        player.setAllIn(true);
        raise(player, player.getMoney());
    }

    public int call(Player player) {
        final int previousContribution = getTotalContribution(player);
        final int owed = getTotalOwed(player);
        final int callAmount = Math.min(owed, player.getMoney());
        call(player, callAmount, 0);
        if (player.getMoney() == 0) {
            player.setAllIn(true);
        }
        return getTotalContribution(player) - previousContribution;
    }

    private void call(Player player, int amount, int recursion) {
        final int currentContribution = getContribution(player);
        if (currentContribution < 0) {
            throw new IllegalStateException("Current contribution for player '" +
                    player + "' can't be negative: " + currentContribution + ". From mainPot: " + isMainPot);
        }
        System.out.println(player + " is putting " + amount + " into pot [ current contribution: " + currentContribution + ", current bet: " + currentBet + "]");
        if (getContribution(player) == currentBet) {
            // Player has already satisfied this pot
            if (sidePot != null) {
                System.out.println(player + " is channeling " + amount + " into a side pot.");
                sidePot.call(player, amount, 0);
                return;
            } else {
                // This should just be a check.
                System.out.println(player + " checks.");
                if (sidePot != null) {
                    sidePot.checkPlayer(player);
                }
            }
        }
        final int total = getContribution(player) + amount;
        if (total > currentBet) {
            System.out.println("Feeding a sidepot!");
            if (sidePot == null) {
                throw new IllegalStateException(player + " called in excess and there is no sidepot. (Tried to put " + amount + " into " + currentBet);
            }
            if (getContribution(player) == currentBet) {
                System.out.println(player + " is funneling " + amount + " into a sidepot");
                sidePot.call(player, amount, 0);
            } else {
                final int needed = currentBet - getContribution(player);
                if (needed > 0) {
                    System.err.println("Needed to put " + needed + " in this pot, current bet is " + currentBet);
                    if (recursion < MAX_RECURSIONS) {
                        call(player, needed, recursion + 1);
                    } else {
                        throw new IllegalStateException("Exceeded max recursion calls");
                    }
                    sidePot.call(player, amount - needed, 0);
                } else {
                    System.out.println(player + " is shoving " + amount + " into a sidepot (needed: " + needed + ")");
                    sidePot.call(player, amount, 0);
                }
            }
            return;
        }
        final int contribution = player.bet(amount);
        addContribution(player, contribution);
        if (getContribution(player) == currentBet) {
            return;
        } else if (getContribution(player) < currentBet) {
            createSidePot(player);
        } else {
            final int owed = getOwed(player);
            System.out.println(player + " owes " + owed + " in this pot, in total owes " + getTotalOwed(player));
            throw new IllegalStateException("This case should have been caught earlier in this method.");
        }
        final int finalContribution = getContribution(player);
        if (finalContribution < 0) {
            throw new IllegalStateException("Impossible that a players contribution is below 0 (final contribution: "
                    + finalContribution + " for player '" + player + "')");
        }
    }

    private void createSidePot(Player creatingPlayer) {
        System.out.println("Creating a new side pot");
        final int newBet = getTotalContribution(creatingPlayer);
        final int difference = currentBet - newBet;
        sidePot = new Pot(newBet);
        for (Player participant : getParticipants()) {
            if (!participant.equals(creatingPlayer)) {
                final int individualContribution = getContribution(participant);
                if (individualContribution < newBet) {
                    // Nothing to move yet
                    System.out.println(participant + " has only put in " + individualContribution + " so far.");
                    continue;
                }
                final int diff = individualContribution - newBet;
                if (diff < 0) {
                    System.out.println("Player " + participant + " has not yet called. Diff: " + diff);
                    addContribution(participant, diff);
                    sidePot.addContribution(participant, -diff);
                } else {
                    addContribution(participant, -difference);
                    sidePot.addContribution(participant, difference);
                }
            }
        }
        sidePot.setBet(difference);
        currentBet -= difference;
    }

    @Override
    public String toString() {
        return "Pot{" +
                "contributions=" + contributions +
                ", isMainPot=" + isMainPot +
                ", sidePot=" + sidePot +
                ", currentBet=" + currentBet +
                '}';
    }

    /**
     * Split up the winnings of this pot to multiple players
     */
    public void splitPot(Set<Player> winners) {
        int winnings = getMoney() / winners.size();
        for(Player winner: winners) {
            winner.win(winnings);
        }
    }

    /**
     * Returns true if the player does not owe anything to the pot.
     */
    public boolean playerCleared(Player player) {
        return getTotalOwed(player) == 0;
    }
}
