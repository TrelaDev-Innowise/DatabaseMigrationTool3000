package com.trela.databasemigrationtool3000.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The `DatabaseManager` class is responsible for managing database connections and
 * ensuring the existence of the migration history table. It uses the HikariCP
 * connection pool for efficient database connection management.
 */
public class DatabaseManager {

    // Logger for tracking events and errors
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    // Database user for tracking who executed the migrations
    private final String user;

    // HikariDataSource for managing the database connection pool
    private final HikariDataSource dataSource;


    public DatabaseManager(String url, String user, String password) {
        // Configure HikariCP connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);      // Set the JDBC URL
        config.setUsername(user);   // Set the database username
        config.setPassword(password); // Set the database password
        this.user = user;           // Store the user for later use
        this.dataSource = new HikariDataSource(config); // Initialize the connection pool
    }


    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createMigrationTableIfNotExists() {
        boolean migrationHistoryTableExists = false;

        // SQL query to create the migration history table and index (if they don't exist)
        String migrationTableQuery = """
                CREATE TABLE IF NOT EXISTS migration_history (
                    id SERIAL PRIMARY KEY,
                    version VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(255),
                    checksum BIGINT NOT NULL,
                    installed_by VARCHAR(100) NOT NULL,
                    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    execution_time_ms BIGINT
                );
                CREATE INDEX IF NOT EXISTS idx_version ON migration_history(version);""";

        // SQL query to check if the `migration_history` table exists
        String checkTableQuery = "SELECT COUNT (*) FROM information_schema.tables " +
                "WHERE table_name = 'migration_history';";

        // Check if the `migration_history` table exists
        try (Connection connection = getConnection()) {
            try(Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(checkTableQuery);
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                migrationHistoryTableExists = true; // Table exists
            }}
        } catch (SQLException e) {
            // Log the error if the table check fails
            logger.error("Error checking for migration history table!", e);
        }

        // If the table does not exist, create it
        if (!migrationHistoryTableExists) {
            logger.info("Creating migration history table!");
            try (Connection connection = getConnection()) {
                try(Statement statement = connection.createStatement()) {
                    statement.execute(migrationTableQuery);
                }
            } catch (SQLException e) {
                // Log the error if table creation fails
                logger.error("Error creating a migration history table!", e);
            }
        }
    }

    public String getUser() {
        return user;
    }
}