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
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionFailedEvent;
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
        if (config.irc.bypassSSL) {
            logDebug("Trusting all SSL certificates enabled.");
            serverBuilder.secureTrustManagerFactory(InsecureTrustManagerFactory.INSTANCE);
        }
        logDebug("Connecting to " + server + ":" + port);
        serverBuilder.host(server)
                .port(port, config.irc.useSSL ? SECURE : INSECURE)
                .password(password);

        ircClient = serverBuilder.then().buildAndConnect();
        ircClient.getEventManager().registerEventListener(new Listener());
        ircClient.addChannel(startingChannel);
    }

    private void getStats(String nickname, String channel) {
        if (roster == null) {
            message(channel, "No stats available at this time");
            return;
        }
        final Stats stats = roster.getStats(nickname);
        if (stats == null) {
            message(channel, "No stats tracked for " + nickname);
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

    public void onMessage(String channel, String sender, String login, String hostname, String message) {

        if (!tables.containsKey(channel) || message.isEmpty() || message.charAt(0) != config.irc.commandPrefix) {
            return;
        }

        final String[] split = SPACES.split(message);
        final Table table = tables.get(channel);
        final String command = split[0].substring(1).toLowerCase();

        switch (command) {
            case "ping": {
                sendReply(channel, sender, message);
                break;
            }
            case "join": {
                table.registerPlayer(sender);
                break;
            }
            case "pot": {
                table.showPot();
                break;
            }
            case "unjoin": {
                table.unjoin(sender);
                break;
            }
            case "current": {
                table.showCurrent();
                break;
            }
            case "players": {
                final List<Player> players = table.getPlayers();
                if (players.isEmpty()) {
                    message(channel, "No joined players.");
                    break;
                }

                if (table.isGameInProgress()) {
                    message(
                            channel,
                            "Now playing: "
                                    + players.stream().map(player ->
                                            player.getName() + " $" + player.getMoney())
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
                table.buyin(sender);
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
                getStats(sender, channel);
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

                table.stopGame();
                break;
            }
            case "call": {
                if (!table.isGameInProgress()) {
                    break;
                }
                table.call(sender);
                break;
            }
            case "c":
            case "czech":
            case "check": {
                if (!table.isGameInProgress()) {
                    break;
                }

                table.check(sender);
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

                if (newRaise == 1) {
                    table.call(sender);
                } else {
                    table.raise(sender, newRaise);
                }

                break;
            }
            case "allin": {
                if (!table.isGameInProgress()) {
                    break;
                }
                table.allIn(sender);
                break;
            }
            case "f":
            case "fold": {
                if (!table.isGameInProgress()) {
                    break;
                }

                table.fold(sender);
                break;
            }
            case "cashout": {
                if (!table.isGameInProgress()) {
                    break;
                }

                table.cashout(sender);
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

    private void configureTable(Table table, String channel, String[] arguments) {
        if (arguments.length == 1) {
            message(channel, "Specify an option to configure, followed by its new value.");
            return;
        }
        final String newValue;
        final String option = arguments[1];

        if (arguments.length == 2) {
            // User just wants to review whats currently configured
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
            if (ircClient.isUser(event.getTarget())) {
                removeGame(event.getChannel().getName());
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
        public void onConnectionFailed(ClientConnectionFailedEvent event) {
            logError("Connection failed.");
            if (event.getCause().isPresent()) {
                logError("Cause for failed connection", event.getCause().get());
            }
        }
    }
}
