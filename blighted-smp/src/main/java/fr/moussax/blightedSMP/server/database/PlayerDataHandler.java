package fr.moussax.blightedSMP.server.database;

import fr.moussax.blightedSMP.BlightedSMP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages SQLite database persistence for player resources and forge fuel state.
 */
public final class PlayerDataHandler {

    /**
     * Immutable data carrier representing loaded player state from the database.
     *
     * @param gems      current gem balance
     * @param mana      current mana pool
     * @param forgeFuel current thermal forge fuel in millibuckets
     */
    public record PlayerData(int gems, double mana, int forgeFuel) {
        public static final PlayerData DEFAULT = new PlayerData(0, 100.0, 0);
    }

    private final UUID playerId;
    private final String playerName;
    private final Connection connection;

    /**
     * Creates a player data handler for the specified player.
     *
     * @param playerId   unique identifier of the player
     * @param playerName current username of the player
     */
    public PlayerDataHandler(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.connection = BlightedSMP.getInstance().getDatabase().getConnection();
    }

    /**
     * Persists player gems, mana, and forge fuel state to the database.
     *
     * @param gems      current gems balance to save
     * @param mana      current mana level to save
     * @param forgeFuel current forge fuel amount to save
     */
    public void save(int gems, double mana, int forgeFuel) {
        String query = """
                INSERT INTO players (uuid, name, gems, mana, forge_fuel)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                  name = excluded.name,
                  gems = excluded.gems,
                  mana = excluded.mana,
                  forge_fuel = excluded.forge_fuel
                """;

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, playerName);
                statement.setInt(3, gems);
                statement.setDouble(4, mana);
                statement.setInt(5, forgeFuel);
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to save player data to database.", exception);
            }
        }
    }

    /**
     * Loads the player's saved state from the database.
     * If no existing record exists, inserts and returns the default state.
     *
     * @return loaded player data
     */
    public PlayerData load() {
        String query = "SELECT name, gems, mana, forge_fuel FROM players WHERE uuid = ?";

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerId.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int gems = resultSet.getInt("gems");
                        double mana = resultSet.getDouble("mana");
                        int forgeFuel = resultSet.getInt("forge_fuel");

                        String storedName = resultSet.getString("name");
                        if (!storedName.equals(playerName)) {
                            save(gems, mana, forgeFuel);
                        }
                        return new PlayerData(gems, mana, forgeFuel);
                    } else {
                        save(PlayerData.DEFAULT.gems(), PlayerData.DEFAULT.mana(), PlayerData.DEFAULT.forgeFuel());
                        return PlayerData.DEFAULT;
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to load player data from database.", exception);
            }
        }
    }
}
