package me.ars.pokerbot.poker;

import java.util.Objects;

public class Player {
	/**
	 * Immutable unique identifier for this player.
	 */
	private final String uniqueIdentifier;
	private int money;
	private Card card1, card2;
	private boolean active = true;
	private boolean folded = false;
	private boolean isAllIn = false;

	/**
	 * Creates a new Player object.
	 *
	 * @param uniqueIdentifier An identifier for this player that will not change over the course of the game.
	 */
	public Player(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	@Override
	public String toString() {
		return getName();
	}

	public final boolean isAllIn() {
		return isAllIn;
	}

	final void setAllIn(boolean allIn) {
		isAllIn = allIn;
	}

	/**
	 * Returns the name of the player.
	 * This returns the unique identifier by default. You should override this if it would make more sense in your
	 * implementation, for instance to instead return an irc nickname (which is something that could change).
	 *
	 * @return Name of the player
	 */
	public String getName() {
		return uniqueIdentifier;
	}

	public final int getMoney() {
		return money;
	}

	final void setMoney(int money) {
		this.money = money;
	}

	public final boolean isBroke() {
		return money == 0;
	}

	final void newHand() {
		folded = false;
	}

	final void receiveCards(Card card1, Card card2) {
		this.card1 = card1;
		this.card2 = card2;
	}

	public final Card getCard1() {
		return card1;
	}

	public final Card getCard2() {
		return card2;
	}

	public final boolean isFolded() {
		return folded;
	}

	public boolean isActive() {
		return active;
	}

	final int bet(int amount) {
		if (!active)
			return 0;
		money -= amount;
		return amount;
	}

	final void win(int pot) {
		if (!active)
			return;

		money += pot;
	}

	final void cashout() {
		fold();
		active = false;
	}

	final void fold() {
		folded = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Player player = (Player) o;
		return uniqueIdentifier.equals(player.uniqueIdentifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uniqueIdentifier);
	}
}
