package me.ars.pokerbot;

import me.ars.pokerbot.poker.Player;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
    private static final String DATABASE = "jdbc:sqlite:poker.db";
    private static final Logger LOGGER = Logger.getLogger("database");

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE);
    }

    public void prepare() {
        try (final Connection connection = getConnection();
             final Statement statement = connection.createStatement()) {
            final String playerTable = "CREATE TABLE IF NOT EXISTS players (\n"
                    + "	id integer PRIMARY KEY,\n"
                    + "	username text NOT NULL UNIQUE,\n"
                    + "	email text UNIQUE,\n"
                    + " password text NOT NULL,\n"
                    + "	games integer NOT NULL,\n"
                    + "	wins integer NOT NULL,\n"
                    + "	money integer NOT NULL\n"
                    + ");";
            statement.execute(playerTable);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not prepare database", e);
        }
    }

    public Player getPlayer(String name, String password) {
        final String sql = "SELECT * FROM players WHERE username IS ? AND password IS ?";
        try (final Connection connection = getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, password);
            final ResultSet results = statement.executeQuery();
            if (!results.isBeforeFirst()) {
                LOGGER.log(Level.INFO, name + " does not exist in the database");
                return null;
            }
            return new Player(results.getString("username"), results.getInt("money"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not retrieve player " + name, e);
        }
        return null;
    }

    public void addNewPlayer(Player player, String password) {
        final String sql = "INSERT INTO players(username, password, games, wins, money) VALUES(?, ?, 0, 0, ?)";

        try (final Connection connection = getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getName());
            statement.setString(2, password);
            statement.setInt(3, player.getMoney());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not add player", e);
        }
    }
}
