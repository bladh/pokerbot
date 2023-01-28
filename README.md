PokerBot -- An IRC Croupier
===========================

PokerBot is a lightweight IRC bot used for playing text-based poker (specifically, Texas hold 'em).
This repository is forked from [Pokerbot](https://github.com/arshajii/pokerbot).

![](misc/pokerbot_example_game.png)

Details
--------

### Starting up

The bot can be launched with the following command from the root directory:

```
./gradlew run
```

The bot will join the channel specified in `config.toml`, but supports multiple channels.

To create your own config file, reference the `config.example.toml` file.

### Commands

Commands are issued by prefixing a command keyword with a predefined command prefix (`.` by default). Available commands are listed below.

#### Creating more tables

Private message the bot with `.createtable #CHANNELNAME`, and it will join that channel and set up a table. Kicking the
bot removes the table. If the channel requires a password, just supply it like `.createtable #CHANNELNAME CHANNELPASSWORD`.

#### General Commands

Keyword | Description
--------|------------
`ping` | Ping the bot for a reply.
`join` | Add yourself to the players list for the next game.
`buyin` | Join the next hand for an existing game
`unjoin` | Remove yourself from the players list for the next game.
`joined` | Display who is in the players list for the next game.
`start` | Start the game.
`clear` | Clear the players list for the next game.
`stop` | Stop the game.
`activity` | Show the date and time of latest table activity
`current` | Show what cards are currently on the table and whose turn it is
`config` | Configure table settings, see below

#### Game Commands

Keyword | Description
--------|------------
`call` | Match the current bet.
`check` | Raise nothing, pass on to the next player.
`raise` | Raise by the specified amount *on top of* the last raise (which may have been 0).
`allin` | Go all in
`fold` | Discard your hand and forfeit. You can resume playing next hand.
`cashout` | Quit the game, taking the fortunes you've won with you.

### Customization

The bot needs a `config.toml` file to be able to launch. Inside you can configure
some options:

`startStash` the amount of money players will start with

`ante` How much money to pay for the ante. Set to 0 if you don't want to play with ante.

`bigBlind` How big the big blind is. The small blind will be half of the big blind, rounded up. Set to 0 if you don't want to play with blinds

`spyCards` Special rule that will reveal one card from one opponents hand to each player. Each player will see a different card, and one player will see a fake card that nobody is holding. Set to `true` if you want to play with this rule.

If you want to configure an already created table in your channel, you can write
`.config` followed by one of the options listed above, and then followed by the
new value. It will show the currently configured value if you omit giving a new value.

Requirements
------------


- KittehIrc (gradle dependency)
- Java 8


Issues
------

PokerBot is still in its early stages of development, so there will likely be various bugs that have been overlooked. If you happen to find one, please [submit an issue](https://github.com/bladh/pokerbot/issues/new) about it.

Feel free to also submit an issue to request a feature that does not exist, or to request an enhancement to an existing feature.



