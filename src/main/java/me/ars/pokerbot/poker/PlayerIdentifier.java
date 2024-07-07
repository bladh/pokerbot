package me.ars.pokerbot.poker;

import java.util.Collection;
import java.util.Objects;

public class PlayerIdentifier {
	/**
	 * Immutable unique identifier for this player.
	 */
	private final String uniqueIdentifier;

	/**
	 * Creates a new Player object.
	 *
	 * @param uniqueIdentifier An identifier for this player that will not change over the course of the game.
	 */
	public PlayerIdentifier(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	@Override
	public String toString() {
		return getName();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlayerIdentifier player = (PlayerIdentifier) o;
		return uniqueIdentifier.equals(player.uniqueIdentifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uniqueIdentifier);
	}

	public static PlayerState findState(Collection<PlayerState> collection, PlayerIdentifier identifier) {
		for(PlayerState state : collection) {
			if (state.getIdentifier().equals(identifier)) {
				return state;
			}
		}
		return null;
	}
}
