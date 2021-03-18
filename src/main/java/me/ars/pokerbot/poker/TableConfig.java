package me.ars.pokerbot.poker;

public class TableConfig {

    public final int startingMoney;
    public final int forcedBetType;
    public final int bigBlind;
    public final int ante;

    public TableConfig(int startingMoney, int forcedBetType, int bigBlind, int ante) {
        this.startingMoney = startingMoney;
        this.forcedBetType = forcedBetType;
        this.bigBlind = bigBlind;
        this.ante = ante;
    }
}
