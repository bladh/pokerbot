package me.ars.pokerbot.irc;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import me.ars.pokerbot.config.BotConfig;
import me.ars.pokerbot.poker.Player;
import me.ars.pokerbot.poker.Table;
import me.ars.pokerbot.stats.Roster;
import me.ars.pokerbot.stats.Stats;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionFailedEvent;
import org.kitteh.irc.client.library.event.user.UserNickChangeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.feature.ServerInfo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.kitteh.irc.client.library.Client.Builder.Server.SecurityType.INSECURE;
import static org.kitteh.irc.client.library.Client.Builder.Server.SecurityType.SECURE;

public class KittehBot implements Irc {

    /*
     * pattern representing one or more contiguous whitespace characters, used
     * for parsing commands
     */
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
    private final Map<String, Table> tables;

    private Roster roster;
    private final BotConfig config;

    private final Set<IrcPlayer> players;

    private String botName;

    private final String startingChannel;

    private Client ircClient;

    private boolean verbose = false;

    public KittehBot(BotConfig config) {
        try {
            roster = Roster.getRoster();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tables = new HashMap<>();
        this.config = config;
        startingChannel = config.irc.channel;
        botName = config.irc.nick;
        players = new HashSet<>(15);
    }

    private void logError(String message) {
        System.err.println(message);
    }

    private void logError(String message, Throwable throwable) {
        System.err.println(message);
        System.err.println(throwable.getMessage());
        if (throwable.getCause() != null) {
            System.err.println("Caused by: " + throwable.getCause().getMessage());
        }
    }

    private void logDebug(String message) {
        System.out.println(message);
    }

    private void logVerbose(String message) {
        if (!verbose) return;
        System.out.println(message);
    }

    public void connect(String server, Integer port) {
        connect(server, port, null);
    }

    public void connect(String server, Integer port, String password) {
        final Client.Builder builder = Client.builder();
        final Client.Builder.Server serverBuilder;

        builder.nick(botName)
                .name(botName)
                .realName(botName)
                .user(botName);
        serverBuilder = builder.server();
        if (config.irc.bypassSSL != null && config.irc.bypassSSL) {
            logDebug("Trusting all SSL certificates enabled.");
            serverBuilder.secureTrustManagerFactory(InsecureTrustManagerFactory.INSTANCE);
        }
        logDebug("Connecting to " + server + ":" + port);
        serverBuilder.host(server)
                .port(port, (config.irc.useSSL != null && config.irc.useSSL) ? SECURE : INSECURE)
                .password(password);

        ircClient = serverBuilder.then().buildAndConnect();
        ircClient.getEventManager().registerEventListener(new Listener());
        ircClient.addChannel(startingChannel);
    }

    private void getStats(IrcPlayer player, String channel) {
        if (roster == null) {
            message(channel, "No stats available at this time");
            return;
        }
        final Stats stats = roster.getStats(player.getName());
        if (stats == null) {
            message(channel, "No stats tracked for " + player.getName());
            return;
        }
        message(channel, stats.toString());
    }

    public void joinGameChannel(String channel, String channelPassword) {
        if (channelPassword == null) {
            ircClient.addChannel(channel);
        } else {
            ircClient.addChannel(channel, channelPassword);
        }
    }

    private IrcPlayer getPlayer(String nick, String login, String hostname) {
        for (IrcPlayer registeredPlayer: players) {
            if (registeredPlayer.getNick().equals(nick)) {
                return registeredPlayer;
            }
        }
        logDebug("Could not find a registered player with the nick " + nick + ", so creating one.");
        final String unique = UUID.randomUUID().toString();
        final IrcPlayer player = new IrcPlayer(unique);
        player.setNick(nick);
        player.setLogin(login);
        player.setHost(hostname);
        players.add(player);
        return player;
    }

    private void playerChangedNick(String oldNick, String newNick) {
        for (IrcPlayer registeredPlayer: players) {
            if (registeredPlayer.getNick().equals(oldNick)) {
                logDebug(oldNick + " changed their nickname to " + newNick);
                registeredPlayer.setNick(newNick);
            }
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {

        if (!tables.containsKey(channel) || message.isEmpty() || message.charAt(0) != config.irc.commandPrefix) {
            return;
        }

        final String[] split = SPACES.split(message);
        final Table table = tables.get(channel);
        final String command = split[0].substring(1).toLowerCase();
        final IrcPlayer player;

        switch (command) {
            case "ping": {
                sendReply(channel, sender, message);
                break;
            }
            case "join": {
                player = getPlayer(sender, login, hostname);
                table.registerPlayer(player);
                break;
            }
            case "pot": {
                table.showPot();
                break;
            }
            case "unjoin": {
                player = getPlayer(sender, login, hostname);
                table.unjoin(player);
                break;
            }
            case "current": {
                table.showCurrent();
                break;
            }
            case "players": {
                final Collection<Player> players = table.getPlayers();
                if (players.isEmpty()) {
                    message(channel, "No joined players.");
                    break;
                }

                if (table.isGameInProgress()) {
                    message(
                            channel,
                            "Now playing: "
                                    + players.stream().map(p -> p.getName() + " $" + p.getMoney())
                                    .collect(Collectors.joining(", ")) + ".");
                } else {
                    message(
                            channel,
                            "Joined players: "
                                    + players.stream().map(Player::getName)
                                    .collect(Collectors.joining(", ")) + ".");
                }
                break;
            }
            case "buyin": {
                player = getPlayer(sender, login, hostname);
                table.buyin(player);
                break;
            }
            case "activity": {
                final Calendar activity = table.getLastActivity();
                if (activity == null) {
                    message(channel, "There hasn't been any activity on this table.");
                } else {
                    message(channel, "Last activity: " + sdf.format(activity.getTime()));
                }
                break;
            }
            case "stats": {
                player = getPlayer(sender, login, hostname);
                getStats(player, channel);
                break;
            }
            case "clear": {
                if (table.isGameInProgress()) {
                    sendReply(channel, sender, "A game is already in progress.");
                    break;
                }
                table.clearPlayers();
                message(channel, "Players list cleared.");
                break;
            }
            case "start": {
                if (table.isGameInProgress()) {
                    break;
                }
                if (table.getPlayers().size() > 1) {
                    table.startGame();
                } else {
                    sendReply(channel, sender, "Need at least 2 players to join before starting.");
                }
                break;
            }
            case "stop": {
                if (!table.isGameInProgress()) {
                    break;
                }
                final List<Player> oldPlayers = table.getPlayers();
                table.stopGame();
                for (Table otherTable: tables.values()) {
                    oldPlayers.removeAll(otherTable.getPlayers());
                }
                players.removeAll(oldPlayers);
                break;
            }
            case "call": {
                if (!table.isGameInProgress()) {
                    break;
                }
                player = getPlayer(sender, login, hostname);
                table.call(player);
                break;
            }
            case "c":
            case "czech":
            case "check": {
                if (!table.isGameInProgress()) {
                    break;
                }
                player = getPlayer(sender, login, hostname);
                table.check(player);
                break;
            }
            case "r":
            case "raise": {
                if (!table.isGameInProgress()) {
                    break;
                }

                if (split.length == 1) {
                    sendReply(channel, sender, "Specify an amount to raise by.");
                    break;
                }

                int newRaise;

                try {
                    newRaise = Integer.parseInt(split[1]);
                } catch (NumberFormatException nfe) {
                    sendReply(channel, sender, "Malformed number: " + split[1]
                            + ".");
                    break;
                }

                if (newRaise <= 0) {
                    sendReply(channel, sender,
                            "Can only raise by a positive amount.");
                    break;
                }
                player = getPlayer(sender, login, hostname);

                if (newRaise == 1) {
                    table.call(player);
                } else {
                    table.raise(player, newRaise);
                }

                break;
            }
            case "allin": {
                if (!table.isGameInProgress()) {
                    break;
                }
                player = getPlayer(sender, login, hostname);
                table.allIn(player);
                break;
            }
            case "f":
            case "fold": {
                if (!table.isGameInProgress()) {
                    break;
                }

                player = getPlayer(sender, login, hostname);
                table.fold(player);
                break;
            }
            case "cashout": {
                if (!table.isGameInProgress()) {
                    break;
                }

                player = getPlayer(sender, login, hostname);
                table.cashout(player);
                checkRemovePlayer(player);
                break;
            }
            case "config": {
                configureTable(table, channel, split);
                break;
            }
            case "help": {
                // todo
                break;
            }
        }
    }

    private void checkRemovePlayer(IrcPlayer player) {
        boolean playingElsewhere = false;
        for (Table otherTable: tables.values()) {
            if (otherTable.getPlayers().contains(player)) {
                playingElsewhere = true;
                break;
            }
        }
        if (!playingElsewhere) {
            players.remove(player);
        }
    }

    private void configureTable(Table table, String channel, String[] arguments) {
        if (arguments.length == 1) {
            message(channel, "Specify an option to configure, followed by its new value.");
            return;
        }
        final String newValue;
        final String option = arguments[1];

        if (arguments.length == 2) {
            // User just wants to review what's currently configured
            newValue = null;
        } else {
            if (table.isGameInProgress()) {
                message(channel, "Can only change table configuration when a game is not in progress.");
                return;
            }
            newValue = arguments[2];
        }
        table.configure(option, newValue);
    }

    protected void sendReply(String channel, String name, String message) {
        message(channel, Formatting.BOLD + name + Formatting.CLEAR + ": " + message);
    }


    @Override
    public void message(String channel, String message) {
        if (ircClient == null) {
            logError("Bot not yet connected to irc.");
            return;
        }
        ircClient.sendMessage(channel, message);
    }

    @Override
    public void message(Player player, String message) {
        if (ircClient == null) {
            logError("Bot not yet connected to irc.");
            return;
        }
        final IrcPlayer ircPlayer = (IrcPlayer) player;
        ircClient.sendMessage(ircPlayer.getNick(), message);
    }

    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    private void removeGame(String channel) {
        logDebug("Removing table for " + channel);
        final Table table = tables.get(channel);
        table.stopGame();
        tables.remove(channel);
    }

    private void setUpTable(String channel) {
        if (tables.containsKey(channel)) {
            logVerbose("Already have a table for " + channel);
            return;
        }
        logDebug("Setting up a table for " + channel);
        final IrcStateCallback callback = new IrcStateCallback(this, channel);
        tables.put(channel, new Table(callback, roster, config.game));
    }

    @Override
    public void gameEnded(List<Player> oldPlayers) {
        for (Table otherTable: tables.values()) {
            oldPlayers.removeAll(otherTable.getPlayers());
        }
        players.removeAll(oldPlayers);
    }

    public class Listener {
        @Handler
        public void onMessageEvent(ChannelMessageEvent event) {
            final User user = event.getActor();
            onMessage(event.getChannel().getName(),
                    user.getNick(),
                    user.getUserString(),
                    user.getHost(),
                    event.getMessage());
        }

        @Handler
        public void onKickedEvent(ChannelKickEvent event) {
            final String channel = event.getChannel().getName();
            if (ircClient.isUser(event.getTarget())) {
                removeGame(channel);
            } else {
                final String nick = event.getTarget().getNick();
                if (!tables.containsKey(channel)) return;
                for (IrcPlayer player: players) {
                    if (nick.equals(player.getNick())) {
                        logDebug("A player has been kicked from a channel where they may have been playing.");
                        final Table table = tables.get(channel);
                        table.playerLeft(player);
                        checkRemovePlayer(player);
                        return;
                    }
                }
            }
        }

        @Handler
        public void onJoinedChannel(ChannelJoinEvent event) {
            if (ircClient.isUser(event.getUser())) {
                logDebug("Joined " + event.getChannel().getName() + ".");
                setUpTable(event.getChannel().getName());
            }
        }

        @Handler
        public void onConnectionEstablished(ClientConnectionEstablishedEvent event) {
            final ServerInfo server = event.getClient().getServerInfo();
            if (server.getAddress().isPresent()) {
                logDebug("Connection established to " + server.getAddress().get());
            }
        }

        @Handler
        public void onConnectionClosed(ClientConnectionClosedEvent event) {
            logError("Connection closed.");
            if (event.getLastMessage().isPresent()) {
                logError("Last message: " + event.getLastMessage().get());
            }
            if (event.getCause().isPresent()) {
                logError("Cause for closed connection", event.getCause().get());
            }
        }

        @Handler
        public void onChangedNick(UserNickChangeEvent event) {
            final String oldNick = event.getOldUser().getNick();
            final String newNick = event.getNewUser().getNick();
            playerChangedNick(oldNick, newNick);
        }

        @Handler
        public void onUserDisconnected(UserQuitEvent event) {
            final String nick = event.getUser().getNick();
            for (IrcPlayer player: players) {
                if (nick.equals(player.getNick())) {
                    logDebug("A player has disconnected.");
                    for (Table table: tables.values()) {
                        table.playerLeft(player);
                    }
                    players.remove(player);
                    checkRemovePlayer(player);
                    return;
                }
            }
        }

        @Handler
        public void onUserParted(ChannelPartEvent event) {
            final String nick = event.getUser().getNick();
            if (!event.getAffectedChannel().isPresent()) return;
            final String channel = event.getAffectedChannel().get().getName();
            if (!tables.containsKey(channel)) return;
            for (IrcPlayer player: players) {
                if (nick.equals(player.getNick())) {
                    logDebug("A player has left a channel where they may have been playing.");
                    final Table table = tables.get(channel);
                    table.playerLeft(player);
                    checkRemovePlayer(player);
                    return;
                }
            }
        }

        @Handler
        public void onConnectionFailed(ClientConnectionFailedEvent event) {
            logError("Connection failed.");
            if (event.getCause().isPresent()) {
                logError("Cause for failed connection", event.getCause().get());
            }
        }
    }
}
