package me.ars.pokerbot.poker;

import me.ars.pokerbot.config.GameConfig;
import me.ars.pokerbot.stats.Roster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;

public class TableTest {

  private Table table;
  private StateCallback callback;
  private Roster roster;
  private GameConfig config;

  private GameConfig anteConfig() {
    final GameConfig conf = new GameConfig();
    conf.bigBlind = null;
    conf.startStash = 200;
    conf.ante = 5;
    return conf;
  }

  @Before
  public void before() {
    callback = Mockito.mock(StateCallback.class);
    roster = Mockito.mock(Roster.class);
    config = new GameConfig();
    config.startStash = 200;
    config.bigBlind = 5;
    table = new Table(callback, roster, config);
  }

  @Test
  public void shortGameWithAnte() {
    // Simple smoke test to see if two players can check all the way to the end without anything crashing.
    table = new Table(callback, roster, anteConfig());
    final Helper hadWinner = new Helper(false);

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      PlayerIdentifier winner = (PlayerIdentifier)args[0];
      System.out.println(winner + " has won!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareWinner(any(PlayerIdentifier.class), any(Hand.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      System.out.println("Split pot!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareSplitPot(anyList(), any(Hand.HandType.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      PlayerIdentifier currentPlayer = (PlayerIdentifier)args[2];
      List<Card> cards = (List<Card>)args[0];
      final String tableStr = cards.isEmpty() ? "no cards" : cards.stream()
          .map(Card::toString).collect(Collectors.joining(", "));
      System.out.println(currentPlayer + " - " + tableStr);
      return null;
    }).when(callback).updateTable(anyList(),anyInt(),any(PlayerIdentifier.class));

    final PlayerIdentifier player1 = new PlayerIdentifier("player1");
    final PlayerIdentifier player2 = new PlayerIdentifier("player2");
    table.registerPlayer(player1);
    table.registerPlayer(player2);
    table.startGame();
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    table.check(player1);
    table.check(player2);
    Assert.assertTrue("The game must have had a winner", hadWinner.get());
  }

  @Test
  public void testTurnOrderWithAnte() {
    table = new Table(callback, roster, anteConfig());
    final PlayerIdentifier player1 = new PlayerIdentifier("player1");
    final PlayerIdentifier player2 = new PlayerIdentifier("player2");
    table.registerPlayer(player1);
    table.registerPlayer(player2);
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", player1, table.getCurrentPlayerId());
    table.check(player1);
    Assert.assertEquals("Player 2 should be next", player2, table.getCurrentPlayerId());
    table.check(player2);
    Assert.assertEquals("Player 1 should be next", player1, table.getCurrentPlayerId());
    table.check(player1);
    Assert.assertEquals("Player 2 should be next", player2, table.getCurrentPlayerId());
    table.raise(player2, 5);
    Assert.assertEquals("It should be player 1's turn after player 2 has raised", player1, table.getCurrentPlayerId());
    table.call(player1);
    Assert.assertEquals("It should be back to Player 1's turn", player1, table.getCurrentPlayerId());
  }

  @Test
  public void testTurnOrderWithBlinds() {
    final PlayerIdentifier p1 = new PlayerIdentifier("player1");
    final PlayerIdentifier p2 = new PlayerIdentifier("player2");
    table.registerPlayer(p1);
    table.registerPlayer(p2);
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayerId());
    table.check(p1);
    Assert.assertEquals("Player 1 cannot check without calling the blind, it should still be player 1s turn",
            p1, table.getCurrentPlayerId());
    table.call(p1);
    Assert.assertEquals("After player 1 calls, it should be player 2", p2, table.getCurrentPlayerId());
    table.check(p2);
    Assert.assertEquals("Player 2 should be able to check, and it should be back to player 1", p1,
            table.getCurrentPlayerId());
  }

  @Test
  public void testCashoutDuringGame() {
    final PlayerIdentifier p1 = new PlayerIdentifier("player1");
    final PlayerIdentifier p2 = new PlayerIdentifier("player2");
    final PlayerIdentifier p3 = new PlayerIdentifier("player3");
    table.registerPlayer(p1);
    table.registerPlayer(p2);
    table.registerPlayer(p3);
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayerId());
    table.call(p1);
    Assert.assertEquals("After player 1 calls, it should be player 2", p2, table.getCurrentPlayerId());
    table.playerLeft(p3);
    Assert.assertEquals("After player 3 leaves, it should still be player 2", p2, table.getCurrentPlayerId());
    table.check(p2);
    Assert.assertEquals("Player 2 should be able to check, and it should be back to player 1", p1,
            table.getCurrentPlayerId());
  }

  @Test
  public void testCashoutDuringGame2() {
    final PlayerIdentifier p1 = new PlayerIdentifier("player1");
    final PlayerIdentifier p2 = new PlayerIdentifier("player2");
    final PlayerIdentifier p3 = new PlayerIdentifier("player3");
    table.registerPlayer(p1);
    table.registerPlayer(p2);
    table.registerPlayer(p3);
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayerId());
    table.playerLeft(p1);
    Assert.assertEquals("After player 1 leaves, it should be player 2", p2, table.getCurrentPlayerId());
    table.check(p2);
    Assert.assertEquals("After player 2 checks, it should be player 3", p3, table.getCurrentPlayerId());
    boolean couldCall = table.call(p3);
    Assert.assertTrue("Player 3 should be able to call the blind", couldCall);
    Assert.assertEquals("It should be back to player 2s turn", p2,
            table.getCurrentPlayerId());
    table.check(p2);
    table.check(p3);
    table.check(p2);
    table.check(p3);
    Assert.assertEquals("After player 3 checks, it should be player 2", p2, table.getCurrentPlayerId());
  }

  @Test
  public void testOtherPlayersCashingOut() {
    final Helper hadWinner = new Helper(false);
    final PlayerIdentifier p1 = new PlayerIdentifier("player1");
    final PlayerIdentifier p2 = new PlayerIdentifier("player2");
    final PlayerIdentifier p3 = new PlayerIdentifier("player3");

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      PlayerIdentifier winner = (PlayerIdentifier)args[0];
      Assert.assertEquals("player1 should be the winner", p1, winner);
      hadWinner.set(true);
      return null;
    }).when(callback).declareWinner(any(PlayerIdentifier.class), isNull(), anyInt());

    table.registerPlayer(p1);
    table.registerPlayer(p2);
    table.registerPlayer(p3);
    table.startGame();
    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayerId());
    table.playerLeft(p2);
    Assert.assertEquals("After player 2 leaves, it should still be player 1", p1, table.getCurrentPlayerId());
    table.playerLeft(p3);
    Assert.assertTrue("The game must have had a winner", hadWinner.get());
    Assert.assertFalse("The game should be over", table.isGameInProgress());
  }

  @Test
  public void testFlopOrder() {
    final Helper hadWinner = new Helper(false);

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      PlayerIdentifier winner = (PlayerIdentifier)args[0];
      System.out.println(winner + " has won!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareWinner(any(PlayerIdentifier.class), any(Hand.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      System.out.println("Split pot!");
      hadWinner.set(true);
      return null;
    }).when(callback).declareSplitPot(anyList(), any(Hand.HandType.class), anyInt());

    Mockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      Object mock = invocation.getMock();
      PlayerIdentifier currentPlayer = (PlayerIdentifier)args[2];
      List<Card> cards = (List<Card>)args[0];
      final String tableStr = cards.isEmpty() ? "no cards" : cards.stream()
          .map(Card::toString).collect(Collectors.joining(", "));
      System.out.println(currentPlayer + " - " + tableStr);
      return null;
    }).when(callback).updateTable(anyList(),anyInt(),any(PlayerIdentifier.class));
    final PlayerIdentifier p1 = new PlayerIdentifier("player1");
    final PlayerIdentifier p2 = new PlayerIdentifier("player2");

    table.registerPlayer(p1);
    table.registerPlayer(p2);
    table.startGame();

    Assert.assertEquals("The first joined player should be the first to play", p1, table.getCurrentPlayerId());
    Assert.assertEquals("StartPlayer should be 0", 0, table.getStartPlayer());
    table.call(p1);
    Assert.assertEquals("After player 1 calls, it should be player 2", p2, table.getCurrentPlayerId());
    table.check(p2);
    Assert.assertEquals("It should be player 1s turn on the first flop.", p1, table.getCurrentPlayerId());
    table.check(p1);
    table.check(p2);
    Assert.assertEquals("It should be player 1s turn on the first turn.", p1, table.getCurrentPlayerId());
    table.check(p1);
    table.check(p2);
    Assert.assertEquals("It should be player 1s turn on the first river.", p1, table.getCurrentPlayerId());
    table.check(p1);
    table.check(p2);
    Assert.assertTrue("The game must have had a winner", hadWinner.get());
    Assert.assertEquals("StartPlayer should be 1", 1, table.getStartPlayer());
    Assert.assertEquals("Player2 should be the starting player on the next hand.", p2, table.getCurrentPlayerId());
    table.call(p2);
    Assert.assertEquals("After player 2 calls, it should be player 1", p1, table.getCurrentPlayerId());
    final boolean couldCall = table.call(p1);
    Assert.assertTrue("Player 1 should be able to call", couldCall);
    Assert.assertEquals("It should be player 2s turn on the second flop.", p2, table.getCurrentPlayerId());
    table.check(p2);
    table.check(p1);
    Assert.assertEquals("It should be player 2s turn on the second turn.", p2, table.getCurrentPlayerId());
    table.check(p2);
    table.check(p1);
    Assert.assertEquals("It should be player 2s turn on the second river.", p2, table.getCurrentPlayerId());
    table.check(p2);
    table.check(p1);
    Assert.assertTrue("The game must have had a winner", hadWinner.get());
  }

  /**
   * Helper class to hold state for usage with Mockito
   */
  private final class Helper {
    private boolean b;

    Helper(boolean b) {
      this.b = b;
    }

    void set(boolean b) {
      this.b = b;
    }

    boolean get() {
      return b;
    }
  }
}
