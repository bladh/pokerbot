package me.ars.pokerbot.poker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class PotTest {

  private Player player1;
  private Player player2;
  private Player player3;

  private static final int ANTE = 5;

  public static <T> Set<T> toSet(T... stuff) {
    Set<T> set = new HashSet<>();
    Collections.addAll(set, stuff);
    return set;
  }

  @Before
  public void setup() {
    player1 = new Player("player1");
    player1.setMoney(200);
    player2 = new Player("player2");
    player2.setMoney(200);
    player3 = new Player("player3");
    player3.setMoney(200);
  }

  @Test
  public void testAnte() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testAnteAndRaise() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    final Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    Assert.assertEquals("The ante should remain in the pot", ANTE * 3, pot.getMoney());
    Assert.assertEquals("The 'bet' should remain at " + ANTE, ANTE, pot.getCurrentBet());
    Assert.assertEquals("Everyone should still be on the main pot", expectedList, pot.getParticipants());
    Assert.assertFalse("There still shouldn't be a side pot", pot.hasSidePot());
    pot.raise(player1, 50);
    final int paid = 50 + ANTE;
    Assert.assertEquals(paid, pot.getTotalContribution(player1));
    Assert.assertEquals("Player 2 should owe the new raise", 50, pot.getTotalOwed(player2));
    pot.call(player2);
    pot.call(player3);
    pot.newTurn();
    Assert.assertEquals(paid*3, pot.getTotalMoney());
    Assert.assertEquals(paid, pot.getTotalContribution(player1));
    Assert.assertEquals(paid, pot.getTotalContribution(player2));
    Assert.assertEquals(paid, pot.getTotalContribution(player3));
  }

  @Test
  public void testAnteRaiseAndFold() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    Assert.assertEquals(ANTE * 2, pot.getMoney());
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    final Set<Player> expectedList = toSet(player1, player2);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.newTurn();
    pot.raise(player1, 5);
    // player 2 folds
    final int winnings = pot.getTotalMoney();
    player1.win(winnings);
    pot.reset();
    Assert.assertEquals(205, player1.getMoney());
    Assert.assertEquals(195, player2.getMoney());
  }

  @Test
  public void testCheckingRound() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.checkPlayer(player3);
    Set<Player> expectedSet = toSet(player1, player2, player3);
    Assert.assertEquals(expectedSet, pot.getParticipants());
    Assert.assertEquals(ANTE*3, pot.getMoney());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testRaisingAndCallingOnAnteRound() {
    final Pot pot = new Pot();
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    pot.collectAnte(player3, ANTE);
    Assert.assertEquals(ANTE * 3, pot.getMoney());
    pot.raise(player1, 100);
    Assert.assertEquals((ANTE * 3)+100, pot.getMoney());
    Assert.assertEquals(ANTE + 100, pot.getCurrentBet());
    pot.call(player2);
    Assert.assertEquals("There should be 215 in the pot",(ANTE * 3)+200, pot.getMoney());
    Assert.assertEquals("The current bet should remain unchanged at 105",ANTE + 100, pot.getCurrentBet());
    pot.call(player3);
    Set<Player> expectedList = toSet(player1, player2, player3);
    Assert.assertEquals(expectedList, pot.getParticipants());
    Assert.assertEquals((ANTE * 3)+300, pot.getMoney());
    Assert.assertEquals(100 + ANTE, pot.getCurrentBet());
    Assert.assertFalse("There shouldn't be a side pot", pot.hasSidePot());
  }

  @Test
  public void testMakingSidePots() {
    final Player scrooge = new Player("Scrooge");
    scrooge.setMoney(200);
    final Player donald = new Player("Donald");
    donald.setMoney(100);
    final Pot pot = new Pot();
    pot.raise(scrooge, 150);
    pot.call(donald);
    Assert.assertEquals("There should only be 200 in the main pot", 200, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 100", 100, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 50 in the side pot", 50, sidePot.getMoney());
    Set<Player> everyone = toSet(scrooge, donald);
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Set<Player> loneList = toSet(scrooge);
    Assert.assertEquals("Scrooge should be alone in the side pot", loneList, sidePot.getParticipants());
    Assert.assertEquals("The total pot size should be 250", 250, pot.getTotalMoney());
  }

  @Test
  public void testMakingSidePotsThreePlayers() {
    final Player scrooge = new Player("Scrooge");
    scrooge.setMoney(500);
    final Player gearloose = new Player("Gearloose");
    gearloose.setMoney(200);
    final Player donald = new Player("Donald");
    donald.setMoney(50);
    final Set<Player> everyone = toSet(scrooge, gearloose, donald);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose);
    pot.call(donald);
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 50", 50, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 100 in the side pot", 100, sidePot.getMoney());
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Set<Player> sidePotList = toSet(scrooge, gearloose);
    Assert.assertEquals("Scrooge and Gearloose should be in the side pot", sidePotList, sidePot.getParticipants());
  }

  @Test
  public void testAllins() {
    final Player scrooge = new Player("Scrooge");
    scrooge.setMoney(500);
    final Player gearloose = new Player("Gearloose");
    gearloose.setMoney(200);
    final Player donald = new Player("Donald");
    donald.setMoney(50);
    final Set<Player> everyone = toSet(scrooge, gearloose, donald);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose);
    pot.allIn(donald);
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("The main pot bet should be floored to 50", 50, pot.getCurrentBet());
    Assert.assertTrue("There should be a sidepot", pot.hasSidePot());
    final Pot sidePot = pot.getSidePot();
    Assert.assertEquals("There should be 100 in the side pot", 100, sidePot.getMoney());
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Set<Player> sidePotList = toSet(scrooge, gearloose);
    Assert.assertEquals("Scrooge and Gearloose should be in the side pot", sidePotList, sidePot.getParticipants());
    pot.newTurn();
    pot.checkPlayer(scrooge);
    pot.allIn(gearloose);
    pot.checkPlayer(donald);
    pot.call(scrooge);
    final int totalmoney = pot.getTotalMoney();
    Assert.assertEquals(450, totalmoney);
    Assert.assertEquals(50, pot.getTotalContribution(donald));
    Assert.assertEquals(200, pot.getTotalContribution(gearloose));
    Assert.assertEquals(200, pot.getTotalContribution(scrooge));
  }

  @Test
  public void testMultipleSidepots() {
    final Player scrooge = new Player("Scrooge");
    scrooge.setMoney(500);
    final Player gearloose = new Player("Gearloose");
    gearloose.setMoney(200);
    final Player donald = new Player("Donald");
    donald.setMoney(50);
    final Set<Player> everyone = toSet(scrooge, gearloose, donald);
    final Set<Player> secondPotParticipants = toSet(scrooge, gearloose);
    final Set<Player> thirdPotParticipants = toSet(scrooge);
    final Pot pot = new Pot();
    pot.raise(scrooge, 100);
    pot.call(gearloose);
    pot.call(donald); // 250
    final Pot firstSidePot = pot.getSidePot();
    Assert.assertEquals("There should be only 100 in the side pot at this point", 100, firstSidePot.getMoney());
    Assert.assertEquals("Only 250 has been put into the pot at this point", 250, pot.getTotalMoney());
    pot.raise(scrooge, 200);
    pot.call(gearloose); // 300
    pot.checkPlayer(donald);
    Assert.assertEquals("Everyone should be in the main pot", everyone, pot.getParticipants());
    Assert.assertEquals("There should only be 150 in the main pot", 150, pot.getMoney());
    Assert.assertEquals("Scrooge and Gearloose should be in the first sidepot", secondPotParticipants, firstSidePot.getParticipants());
    Assert.assertEquals("There should only be 300 in the first sidepot", 300, firstSidePot.getMoney());
    final Pot secondSidePot = firstSidePot.getSidePot();
    Assert.assertNotNull("There should be a second side pot", secondSidePot);
    Assert.assertEquals("Scrooge should be alone in the final side pot", thirdPotParticipants, secondSidePot.getParticipants());
    Assert.assertEquals("Everyone has put in 550", 550, pot.getTotalMoney());
    Assert.assertEquals("Scrooge should be alone in the last sidepot", thirdPotParticipants, secondSidePot.getParticipants());
  }

  @Test
  public void testReRaiseAndSplitPot() {
    final Pot pot = new Pot();
    final Set<Player> expectedList = toSet(player1, player2);
    pot.collectAnte(player1, ANTE);
    pot.collectAnte(player2, ANTE);
    Assert.assertEquals(ANTE, pot.getCurrentBet());
    pot.raise(player1, 5);
    pot.raise(player2, 5);
    pot.call(player1);
    Assert.assertEquals(30, pot.getMoney());
    Assert.assertEquals("Player 1 should have put in 15 so far.", (200-15), player1.getMoney());
    Assert.assertEquals("Player 2 should have put in 15 so far.", (200-15), player2.getMoney());
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.checkPlayer(player2);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.raise(player2, 10);
    pot.call(player1);
    pot.newTurn();
    pot.checkPlayer(player1);
    pot.raise(player2, 10);
    pot.call(player1);

    pot.splitPot(expectedList);
    Assert.assertEquals("Player 1 should be back at starting money",200, player1.getMoney());
    Assert.assertEquals("Player 2 should be back at starting money",200, player2.getMoney());
  }

  @Test
  public void testBlinds() {
    final Pot pot = new Pot();
    final int bigBlind = 5;
    final int smallBlind = Pot.calculateSmallBlind(bigBlind);
    player1.setMoney(100);
    player2.setMoney(100);
    player3.setMoney(100);
    pot.collectBigBlind(player1, 5);
    pot.collectSmallBlind(player2, 5);
    Assert.assertEquals("Player 1 should have contributed the big blind ", bigBlind, pot.getTotalContribution(player1));
    Assert.assertEquals("Player 1 should have had the big blind deducted from their funds", 100 - 5, player1.getMoney());
    Assert.assertEquals("Player 2 should have contributed the small blind ", smallBlind, pot.getTotalContribution(player2));
    Assert.assertEquals("Player 2 should have the small blind deducted from their funds", 100 - smallBlind, player2.getMoney());
    Assert.assertEquals("Player 3 should not have contributed anything yet", 0, pot.getTotalContribution(player3));
    pot.call(player2);
    Assert.assertEquals("By calling, player 2 should have matched the big blind", bigBlind, pot.getTotalContribution(player2));
    pot.call(player3);
    Assert.assertEquals("By calling, player 3 should have matched the big blind", bigBlind, pot.getTotalContribution(player3));
    final boolean couldCheck = pot.checkPlayer(player1);
    Assert.assertTrue("Player1 should be able to check", couldCheck);
  }

  /**
   * This bug only occurs in the following scenario:
   * 2 players have money, one player is broke.
   * One player raises, the broke player calls the raise but can't afford
   * the whole raise, creating a side pot. The third player, who can afford the
   * raise but did not yet get a chance to call, calls the raise. Since the pot has no concept
   * of turn order it has already moved the contributions from the third player (who did not yet call)
   * into the side pot which causes their contribution in the main pot to be negative, since it assumed
   * that everyone except the broke player has called.
   * <p>
   * See issue 28 for the bug report.
   * <p>
   * <a href="https://github.com/bladh/pokerbot/issues/28">Issue 28 on GitHub</a>
   */
  @Test
  public void brokePlayerCallingRaise() {
    final Pot pot = new Pot();
    player1.setMoney(220);
    player2.setMoney(40);
    player3.setMoney(340);
    pot.collectBigBlind(player1, 5);
    pot.collectSmallBlind(player2, 5);
    pot.call(player2);
    Assert.assertEquals("There should be 10 in the pot", 10, pot.getMoney());
    pot.raise(player1, 20);
    pot.call(player3);
    Assert.assertEquals("There should be 55 in the pot", 55, pot.getMoney());
    pot.call(player2);
    Assert.assertEquals("There should be 75 in the pot", 75, pot.getMoney());
    pot.call(player2);
    pot.raise(player1, 10);
    Assert.assertEquals("There should be 85 in the pot", 85, pot.getMoney());
    pot.raise(player3, 40);
    Assert.assertEquals("There should be 135 in the pot", 135, pot.getMoney());
    Assert.assertEquals("At this point, player 2 should have very little money",15, player2.getMoney());
    pot.call(player2);
    Assert.assertEquals("The previous contribution by player 1 should remain in the main pot", 35, pot.getContribution(player1));
    Assert.assertEquals("There should be no contribution by player 1 in the side pot yet", 0, pot.getSidePot().getContribution(player1));
    System.out.println(pot);
    Assert.assertEquals("Player 2 should have spent all their money",0, player2.getMoney());
    Assert.assertEquals("All of player 2s money should be in the main pot", 40, pot.getTotalContribution(player2));
    Assert.assertEquals("The total bet in the main pot should match 40", 40, pot.getCurrentBet());
    pot.call(player1);
    Assert.assertEquals("At this point, the main pot has matched the minimum bet for everyone", 40*3, pot.getMoney());
    final int secondPotMoney = pot.getSidePot().getMoney();
    Assert.assertEquals("The spilled over money should be in the side pot", 70, secondPotMoney);
    System.out.println(pot);
  }
}
