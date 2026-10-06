package fr.moussax.blightedSMP.server.database;

import fr.moussax.bedrock.utils.debug.Log;
import lombok.Getter;

import java.sql.*;
import java.util.UUID;

/**
 * Manages the SQLite database connection, schema migrations, and custom block persistence.
 */
@Getter
public final class PluginDatabase {
    private final Connection connection;

    /**
     * Creates a database manager at the specified file path and initializes the schema.
     *
     * @param path absolute file path to the SQLite database file
     * @throws SQLException if a database access error occurs during connection or initialization
     */
    public PluginDatabase(String path) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        initializeSchema();
    }

    /**
     * Closes the active database connection if currently open.
     */
    public void closeConnection() {
        if (connection == null) return;
        synchronized (connection) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException exception) {
                Log.error("PluginDatabase", exception.getMessage());
                throw new RuntimeException("Unable to close the database connection");
            }
        }
    }

    private void initializeSchema() throws SQLException {
        synchronized (connection) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS players (
                            uuid TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            blight INTEGER NOT NULL DEFAULT 0,
                            mana REAL NOT NULL DEFAULT 0,
                            forge_fuel INTEGER NOT NULL DEFAULT 0
                        )
                        """
                );

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS blighted_blocks (
                            world_uid TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            block_id TEXT NOT NULL,
                            PRIMARY KEY (world_uid, x, y, z)
                        )
                        """
                );

                try {
                    statement.execute("ALTER TABLE players ADD COLUMN forge_fuel INTEGER NOT NULL DEFAULT 0");
                } catch (SQLException _) {
                }
            }
        }
    }

    /**
     * Persists or replaces a custom block entry at the specified coordinates.
     *
     * @param worldId unique identifier of the world containing the block
     * @param x       block X coordinate
     * @param y       block Y coordinate
     * @param z       block Z coordinate
     * @param blockId custom block identifier
     */
    public void addBlock(UUID worldId, int x, int y, int z, String blockId) {
        String query = "INSERT OR REPLACE INTO blighted_blocks(world_uid, x, y, z, block_id) VALUES(?, ?, ?, ?, ?)";
        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, worldId.toString());
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                statement.setString(5, blockId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                Log.error("PluginDatabase", exception.getMessage());
            }
        }
    }

    /**
     * Removes a custom block entry at the specified coordinates.
     *
     * @param worldId unique identifier of the world containing the block
     * @param x       block X coordinate
     * @param y       block Y coordinate
     * @param z       block Z coordinate
     */
    public void removeBlock(UUID worldId, int x, int y, int z) {
        String query = "DELETE FROM blighted_blocks WHERE world_uid = ? AND x = ? AND y = ? AND z = ?";

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, worldId.toString());
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                statement.executeUpdate();
            } catch (SQLException exception) {
                Log.error("PluginDatabase", exception.getMessage());
            }
        }
    }

    /**
     * Retrieves the custom block identifier at the specified coordinates.
     *
     * @param worldId unique identifier of the world containing the block
     * @param x       block X coordinate
     * @param y       block Y coordinate
     * @param z       block Z coordinate
     * @return custom block identifier, or {@code null} if no block is registered at the coordinates
     */
    public String getBlockId(UUID worldId, int x, int y, int z) {
        String query = "SELECT block_id FROM blighted_blocks WHERE world_uid = ? AND x = ? AND y = ? AND z = ?";

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, worldId.toString());
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString("block_id");
                    }
                }
            } catch (SQLException exception) {
                Log.error("PluginDatabase", exception.getMessage());
            }
        }
        return null;
    }
}
