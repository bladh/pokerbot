package me.ars.pokerbot.poker;

import me.ars.pokerbot.irc.IrcPlayer;
import me.ars.pokerbot.stats.Roster;
import me.ars.pokerbot.config.GameConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Table {
  private final StateCallback callback;
  private final List<Player> players = new ArrayList<>();
  private final Queue<Card> deck = new ArrayDeque<>(52);
  private final List<Card> table = new ArrayList<>(5);
  private final Queue<Player> buyInPlayers = new ArrayDeque<>();
  private final Roster roster;
  private final GameConfig config;
  private final Pot mainPot;
  private Calendar lastActivity = null;
  private boolean gameInProgress = false;
  private int turnIndex;
  private int lastIndex;
  private int startPlayer;

  public Table(StateCallback callback, Roster roster, GameConfig config) {
    this.callback = callback;
    this.roster = roster;
    this.config = config;
    this.mainPot = new Pot();
  }

  private boolean verifyCurrentPlayer(Player player) {
    if (player == null) return false;
    return (player.equals(getCurrentPlayer()));
  }

  public Player getCurrentPlayer() {
    System.out.println("Current turn index: " + turnIndex + " out of " + players.size() + " players. StartPlayer is " + startPlayer + ".");
    return players.get(turnIndex);
  }

  public Calendar getLastActivity() {
    return lastActivity;
  }

  private void setActivity() {
    lastActivity = Calendar.getInstance();
  }

  public boolean isGameInProgress() {
    return gameInProgress;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void showCurrent() {
    if (!gameInProgress) {
      callback.announce("Not currently playing.");
      return;
    }
    final Player currentPlayer = getCurrentPlayer();
    callback.updateTable(table, mainPot.getMoney(), currentPlayer);
    callback.announce(currentPlayer.getName() + " has $" + currentPlayer.getMoney());
  }

  /**
   * Incoming 'call' from [player]
   */
  public boolean call(Player player) {
    if (!verifyCurrentPlayer(player)) return false;
    setActivity();
    final int amount = mainPot.call(player);
    callback.playerCalled(player, amount);
    if (isEveryoneAllin()) {
      revealHands(players);
    }
    nextTurn();
    return true;
  }

  /**
   * Incoming check from [player]
   *
   * @return True if the player successfully checks.
   */
  public boolean check(Player player) {
    if (!verifyCurrentPlayer(player)) return false;
    setActivity();

    final boolean checked = mainPot.checkPlayer(player);
    System.out.println(player + " could check: " + checked);

    if (checked) {
      callback.playerChecked(player);
      nextTurn();
    } else {
      System.err.println(player + " cannot check, they owe " + mainPot.getTotalOwed(player));
      callback.mustCallRaise(player, mainPot.getTotalOwed(player));
    }
    return checked;
  }

  /**
   * Incoming raise from [player]
   *
   * @return True if the raise was successful
   */
  public boolean raise(Player player, int raise) {
    if (!verifyCurrentPlayer(player)) return false;
    setActivity();

    final int result = mainPot.raise(player, raise);
    if (result != -1) {
      callback.playerRaised(player, result);
      lastIndex = lastUnfolded(turnIndex - 1);
      nextTurn();
      return true;
    } else {
      callback.playerCannotRaise(player, player.getMoney());
      return false;
    }
  }

  /**
   * Incoming allin from [player]
   */
  public void allIn(Player player) {
    if (!verifyCurrentPlayer(player)) return;
    setActivity();
    mainPot.allIn(player);
    callback.playerAllin(player);
    lastIndex = lastUnfolded(turnIndex - 1);
    if (isEveryoneAllin()) {
      revealHands(players);
    }
    nextTurn();
  }

  /**
   * Incoming fold from [player]
   */
  public void fold(Player player) {
    if (!verifyCurrentPlayer(player)) return;
    setActivity();
    player.fold();
    callback.playerFolded(player);
    final boolean nextTurn = !checkForWinByFold();
    if (nextTurn) {
      nextTurn();
    }
  }

  public void cashout(Player player) {
    System.out.println("Cashing out " + player);
    setActivity();
    player.cashout();
    callback.playerCashedOut(player, player.getMoney());
    roster.modifyMoney(player.getName(), player.getMoney() - config.startStash);
    final boolean nextTurn = !checkForWinByFold();
    if (isGameInProgress() && verifyCurrentPlayer(player) && nextTurn) {
      nextTurn();
    }
  }

  public void registerPlayer(Player player) {
    if (gameInProgress) {
      callback.announce("A game is already in progress! Use the buyin command if you still want to join");
      return;
    }
    addPlayer(player, true);
    player.setMoney(config.startStash);
  }

  private boolean addPlayer(Player newPlayer, boolean verbose) {
    if (players.contains(newPlayer)) {
      if (verbose) callback.announce(newPlayer.getName() + " has already joined.");
      return false;
    }
    players.add(newPlayer);
    newPlayer.setMoney(config.startStash);
    if (verbose) callback.announce(newPlayer.getName() + " has joined the game.");
    return true;
  }

  private static Card pickRandomCard(Random random, Card... cards) {
    return cards[random.nextInt(cards.length)];
  }

  private void deal() {
    final boolean spyCards = config.spyCards != null && config.spyCards;
    final Random random = new Random();
    Player unlucky = null;
    Card phony = null;
    for (Player player : players) {
      final Card card1 = deck.poll();
      final Card card2 = deck.poll();
      player.receiveCards(card1, card2);
    }

    if (spyCards) {
      phony = deck.poll();
      unlucky = players.get(random.nextInt(players.size()));
    }
    for (Player player : players) {
      Card spyCard = null;
      if (spyCards) {
        if (player.equals(unlucky)) {
          spyCard = phony;
        } else {
          Player randPlayer = player;
          while (randPlayer.equals(player)) {
            randPlayer = players.get(random.nextInt(players.size()));
          }
          spyCard = pickRandomCard(random, randPlayer.getCard1(), randPlayer.getCard2());
        }
      }
      callback.showPlayerCards(player, player.getCard1(), player.getCard2(), spyCard);
    }
  }

  private void setupHand() {
    for (Player player : players) {
      player.setAllIn(false);
      if (player.isBroke()) {
        player.cashout();
        roster.modifyMoney(player.getName(), -config.startStash);
      }
    }

    if (!buyInPlayers.isEmpty()) {
      for (Player newPlayer : buyInPlayers) {
        addPlayer(newPlayer, false);
        //roster.trackGame(newPlayer);
      }
    }
    buyInPlayers.clear();

    try {
      roster.saveRoster();
    } catch (IOException e) {
      System.err.println(e);
      e.printStackTrace();
    }

    final Iterator<Player> playerIter = players.iterator();

    while (playerIter.hasNext()) {
      Player player = playerIter.next();
      if (!player.isActive()) {
        playerIter.remove();
      }
    }

    if (players.size() < 2) {
      System.out.println("Game ended!");
      callback.announce("Not enough players left to continue: game ended.");
      stopGame();
      return;
    }

    callback.announce("Starting new hand...");

    for (Player player : players) {
      player.newHand();
    }

    final List<Card> rawDeck = Arrays.asList(Card.getDeck());
    Collections.shuffle(rawDeck);
    deck.clear();
    deck.addAll(rawDeck);
    table.clear();
    turnIndex = startPlayer;
    lastIndex = lastUnfolded(startPlayer - 1);
    mainPot.reset();

    callback.showPlayers(players.stream().collect(Collectors.toMap((player) -> player, Player::getMoney)));
    deal();
    collectForcedBets();
    sendStatus(getCurrentPlayer());
  }

  private void incrementStartPlayer() {
    try {
      startPlayer = wrappedIncrement(startPlayer);
      System.out.println("Incremented startplayer to " + startPlayer);
    } catch (IndexOutOfBoundsException e) {
      System.err.println(e.toString());
      e.printStackTrace();
      startPlayer = 0;
    }
  }

  public int getStartPlayer() {
    return startPlayer;
  }

  private void nextTurn() {
    mainPot.newTurn();
    final Player player = getCurrentPlayer();
    if (isEveryoneAllin() || turnIndex == lastIndex && (player.isFolded() || player.isBroke() || mainPot.playerCleared(player))) {

      if (table.size() == 5) {
        // winner selection
        checkWinners(mainPot);
	incrementStartPlayer();
        setupHand();
        return;
      } else {
        turnIndex = wrappedDecrement(startPlayer);
        lastIndex = lastUnfolded(startPlayer - 1);
        draw();
      }
    }

    Player nextPlayer;

    do {
      turnIndex = wrappedIncrement(turnIndex);
    } while ((nextPlayer = players.get(turnIndex)).isNotPlaying());

    if (isEveryoneAllin()) {
      callback.updateTable(table, mainPot.getMoney(), null);
      nextTurn();
    } else if (nextPlayer.isAllIn()) {
      callback.announce(nextPlayer.getName() + " is all-in, next player...");
      nextTurn();
    } else {
      sendStatus(nextPlayer);
    }
  }

  private boolean isEveryoneAllin() {
    int activePlayers = 0;
    int allinPlayers = 0;
    for (Player player : players) {
      if (player.isAllIn()) allinPlayers++;
      if (!player.isNotPlaying()) activePlayers++;
    }
    return activePlayers == allinPlayers;
  }

  private void checkWinners(Pot pot) {
    final Set<Player> participants = pot.getParticipants();
    List<Hand> hands = new ArrayList<>(participants.size());
    for (Player p : participants) {
      final Card[] playerCards = table.toArray(new Card[7]);
      playerCards[5] = p.getCard1();
      playerCards[6] = p.getCard2();
      hands.add(Hand.getBestHand(p, playerCards));
    }

    hands.sort(Collections.reverseOrder());
    Iterator<Hand> orderedHands = hands.iterator();
    Hand winningHand;
    Player winner1;

    do {
      winningHand = orderedHands.next();
      winner1 = winningHand.getPlayer();
    } while (winner1.isFolded());

    List<Hand> winners = new ArrayList<>(players.size());
    winners.add(winningHand);

    while (orderedHands.hasNext()) {
      Hand next = orderedHands.next();
      if (winningHand.compareTo(next) != 0)
        break;

      if (!next.getPlayer().isFolded())
        winners.add(next);
    }
    revealHands(participants);

    int numWinners = winners.size();

    if (numWinners == 1) {
      callback.declareWinner(winner1, winningHand, pot.getMoney());
      winner1.win(pot.getMoney());
    } else {
      callback.declareSplitPot(winners.stream().map(Hand::getPlayer)
              .collect(Collectors.toList()), winningHand.getHandType(), pot.getMoney());
      pot.splitPot(winners.stream().map(Hand::getPlayer).collect(Collectors.toSet()));
    }
    if (pot.hasSidePot()) {
      callback.announce("Checking for sidepot winnings...");
      checkWinners(pot.getSidePot());
    }
  }

  /**
   * Reveals non-folded hands of the supplied players.
   */
  private void revealHands(Collection<Player> currentPlayers) {
    final Map<Player, List<Card>> reveal = new HashMap<>();
    for (Player p : currentPlayers) {
      if (!p.isFolded()) {
        final List<Card> cards = new ArrayList<>();
        cards.add(p.getCard1());
        cards.add(p.getCard2());
        reveal.put(p, cards);
      }
    }

    callback.revealPlayers(reveal);
  }

  private void sendStatus(Player player) {
    callback.updateTable(table, mainPot.getMoney(), player);
    callback.declarePlayerTurn(player);
  }

  private void collectForcedBets() {
    if (config.ante != null && config.ante != 0) {
      callback.collectAnte(config.ante);

      for (Player player : players) {
        mainPot.collectAnte(player, config.ante);
      }
    }
    if (config.bigBlind != null && config.bigBlind != 0) {
      final int oldTurnIndex = turnIndex;
      final int oldLastIndex = lastIndex;
      final int blindPlayer = turnIndex;
      final Player smallBlindPlayer = players.get(turnIndex);
      final int smallBlind = mainPot.collectSmallBlind(smallBlindPlayer, config.bigBlind);
      lastIndex = lastUnfolded(turnIndex - 1);
      turnIndex = wrappedIncrement(turnIndex);
      final Player bigBlindPlayer = players.get(wrappedIncrement(blindPlayer));
      final int bigBlind = mainPot.collectBigBlind(bigBlindPlayer, config.bigBlind);
      lastIndex = lastUnfolded(turnIndex - 1);
      turnIndex = wrappedIncrement(turnIndex);
      callback.collectBlinds(bigBlindPlayer, bigBlind, smallBlindPlayer, smallBlind);
      turnIndex = oldTurnIndex;
      lastIndex = oldLastIndex;
    }
  }

  private void draw() {
    if (table.isEmpty()) {
      table.add(deck.poll());
      table.add(deck.poll());
      table.add(deck.poll());
    } else if (table.size() < 5) {
      table.add(deck.poll());
    }
  }

  public void startGame() {
    callback.announce("Starting game with: "
        + players.stream().map(Player::getName)
        .collect(Collectors.joining(", ")) + ".");

    for (Player player : players) {
      roster.trackGame(player.getName());
    }

    gameInProgress = true;
    startPlayer = 0;
    setupHand();
  }

  public void stopGame() {
    System.out.println("Stopping game");
    gameInProgress = false;
    if (players.size() == 1) {
      final Player winner = players.get(0);
      roster.modifyMoney(winner.getName(), winner.getMoney() - config.startStash);
    } else {
      int highscore = 0;
      for (Player player: players) {
        final int playerMoney = player.getMoney();
        roster.modifyMoney(player.getName(), playerMoney - config.startStash);
        if (playerMoney > highscore) {
          highscore = playerMoney;
        }
      }
    }
    final List<Player> oldPlayers = new ArrayList<>(players);
    players.clear();
    deck.clear();
    table.clear();

    callback.gameEnded(oldPlayers);

    try {
      roster.saveRoster();
    } catch (IOException e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
  }

  private boolean checkForWinByFold() {
    Player last = null;
    int numPlayersLeft = players.size();
    for (Player player : players) {
      if (player.isNotPlaying())
        numPlayersLeft--;
      else
        last = player;
    }

    if (last == null) return false;

    if (numPlayersLeft == 1) {
      System.out.println("Have a winner: " + last);
      final int totalMoney = mainPot.getTotalMoney();
      callback.declareWinner(last, null, totalMoney);
      last.win(totalMoney);
      incrementStartPlayer();
      setupHand();
      return true;
    }

    return false;
  }

  private void ensureNotAllFolded() {
    for (Player player : players) {
      if (!player.isFolded())
        return;
    }

    throw new IllegalStateException("All players are folded.");
  }

  private int lastUnfolded(int index) {
    ensureNotAllFolded();

    if (index < 0)
      index = players.size() - 1;

    if (index >= players.size())
      index = 0;

    while (players.get(index).isNotPlaying()) {
      index = wrappedDecrement(index);
    }
    return index;
  }

  private int wrappedIncrement(int n) {
    n++;
    if (n >= players.size())
      n = 0;
    return n;
  }

  private int wrappedDecrement(int n) {
    n--;
    if (n < 0)
      n = players.size() - 1;
    return n;
  }

  public void clearPlayers() {
    players.clear();
  }

  public void unjoin(Player player) {
    if (isGameInProgress()) {
      if (buyInPlayers.contains(player)) {
        callback.announce(player.getName() + ": Your buyin was nulled.");
        buyInPlayers.remove(player);
      } else {
        if (players.contains(player)) {
          callback.announce(player.getName() + ": Cannot unjoin game in progress. Use cashout command.");
        } else {
          callback.announce(player.getName() + ": You are not part of the active game.");
        }
      }
    } else {
      if (players.contains(player)) {
        callback.announce(player.getName() + ": You have unjoined.");
        System.out.println(player.getName() + " unjoined.");
        players.remove(player);
      } else {
        callback.announce(player.getName() + ": You never joined.");
      }
    }
  }

  public void buyin(Player newPlayer) {
    if (!gameInProgress) {
      callback.announce(newPlayer.getName() + ": Game hasn't started yet, putting you up for the game");
      registerPlayer(newPlayer);
      return;
    }
    if (players.contains(newPlayer)) {
        callback.announce(newPlayer.getName() + ": You're already in the game.");
        return;
    }
    if (buyInPlayers.contains(newPlayer)) {
      callback.announce(newPlayer.getName() + ": You've already bought in");
      return;
    }
    buyInPlayers.add(newPlayer);
    callback.announce(newPlayer.getName() + " has bought in the game, will join on next hand.");
  }

  public void showPot() {
    final StringBuilder sb = new StringBuilder();
    getPot(sb, mainPot);
    callback.announce(sb.toString());
  }

  private void getPot(StringBuilder sb, Pot pot) {
    if (pot.isMainPot()) {
      sb.append("Main pot: ");
    } else {
      sb.append(", Side pot: ");
    }
    sb.append(pot.getMoney());
    if (pot.hasSidePot()) {
      getPot(sb, pot.getSidePot());
    }
  }

  /**
   * Configure an option on this table, or review its current value
   *
   * @param option The option to review or change
   * @param newValue The new value to set; if null then show the current value
   */
  public void configure(String option, String newValue) {
      switch(option) {
        case "bigblind": {
          if (newValue == null) {
            final int bigBlind = config.bigBlind;
            if (bigBlind < 1) {
              callback.announce("Blinds are disabled on this table.");
            } else {
              callback.announce("The big blind is currently set to " + bigBlind + ".");
            }
            break;
          }
          int newBigBlind;
          try {
            newBigBlind = Integer.parseInt(newValue);
          } catch (Exception e) {
            callback.announce("Invalid value for blind : " + newValue);
            break;
          }
          if (newBigBlind < 0) newBigBlind = 0;
          config.bigBlind = newBigBlind;
          callback.announce("Changed big blind to " + newBigBlind + ".");
          break;
        }
        case "ante": {
          if (newValue == null) {
            final int ante = config.ante;
            if (ante < 1) {
              callback.announce("Antes are disabled on this table.");
            } else {
              callback.announce("The ante is currently set to " + ante + ".");
            }
            break;
          }
          int newAnte;
          try {
            newAnte = Integer.parseInt(newValue);
          } catch (Exception e) {
            callback.announce("Invalid value for ante : " + newValue);
            break;
          }
          if (newAnte < 0) newAnte = 0;
          config.ante = newAnte;
          callback.announce("Changed ante to " + newAnte + ".");
          break;
        }
        case "startstash": {
          if (newValue == null) {
            final int startStash = config.startStash;
            callback.announce("The starting stash is currently set to " + startStash + ".");
            break;
          }
          int newStash;
          try {
            newStash = Integer.parseInt(newValue);
          } catch (Exception e) {
            callback.announce("Invalid value for starting stash : " + newValue);
            break;
          }
          if (newStash < 1) newStash = 1;
          config.startStash = newStash;
          callback.announce("Changed starting stash to " + newStash + ".");
          break;
        }
        case "spycards": {
          if (newValue == null) {
            if (config.spyCards) {
              callback.announce("Spycards are currently enabled.");
            } else {
              callback.announce("Spycards are currently disabled.");
            }
            break;
          }
          boolean newSpy;
          try {
            newSpy = Boolean.parseBoolean(newValue);
          } catch (Exception e) {
            callback.announce("Invalid value for spycards: " + newValue + ". Use only true or false.");
            break;
          }
          config.spyCards = newSpy;
          callback.announce("Spycards enabled: " + newSpy);
          break;
        }
        default: {
          callback.announce("Unrecognized option: " + option);
          break;
        }
      }
  }

  /**
   * Notify the table that the Player has disconnected from the game.
   */
  public void playerLeft(Player player) {
    if (!players.contains(player)) return;

    if (isGameInProgress()) {
      cashout(player);
    } else {
      unjoin(player);
    }
  }
}
