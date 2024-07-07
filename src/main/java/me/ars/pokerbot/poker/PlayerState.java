package me.ars.pokerbot.poker;

class PlayerState {
    private final PlayerIdentifier identifier;
    private int money;
    private Card card1, card2;
    private boolean active = true;
    private boolean folded = false;
    private boolean isAllIn = false;

    public PlayerState(PlayerIdentifier identifier) {
        this.identifier = identifier;
    }

    public PlayerState(String name) {
        this(new PlayerIdentifier(name));
    }

    public PlayerIdentifier getIdentifier() {
        return identifier;
    }

    public String getName() {
        return identifier.getName();
    }

    public final boolean isAllIn() {
        return isAllIn;
    }

    final void setAllIn(boolean allIn) {
        isAllIn = allIn;
    }

    public final boolean isNotPlaying() {
        return folded || !active;
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

}
