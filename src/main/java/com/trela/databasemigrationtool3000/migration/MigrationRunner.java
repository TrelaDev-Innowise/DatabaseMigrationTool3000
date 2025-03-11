package com.trela.databasemigrationtool3000.migration;

import com.trela.databasemigrationtool3000.config.DatabaseManager;
import com.trela.databasemigrationtool3000.exception.*;
import com.trela.databasemigrationtool3000.proxy.StatementProxy;
import com.trela.databasemigrationtool3000.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The `MigrationRunner` class is responsible for executing database migrations
 * from SQL files located in a specified folder. It ensures that migrations are
 * executed in the correct order, validates file names and checksums, and tracks
 * executed migrations in the `migration_history` table.
 */
public class MigrationRunner {

    // Logger for tracking events and errors
    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    // DatabaseManager for managing database connections
    private final DatabaseManager dbManager;

    /**
     * Constructor for MigrationRunner.
     *
     * @param dbManager The DatabaseManager instance used to manage database connections.
     */
    public MigrationRunner(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Executes database migrations from SQL files in the specified folder.
     *
     * @param migrationsFolder The path to the folder containing migration SQL files.
     */
    public void runMigrations(String migrationsFolder) {
        try (Connection connection = dbManager.getConnection()) {
            boolean wasMigrationExecuted = false;
            connection.setAutoCommit(false); // Disable auto-commit to manage transactions manually

            // Lock the migration history table to prevent concurrent migrations
            lockMigrationTable(connection);

            // List and sort migration files in the specified folder
            List<Path> migrationFiles = Files.list(Paths.get(migrationsFolder))
                    .filter(path -> path.toString().toLowerCase().endsWith(".sql")) // Filter for SQL files
                    .sorted() // Sort files by name (version)
                    .collect(Collectors.toList());

            long lastMigrationVersion = 0; // Track the last executed migration version

            // Process each migration file
            for (Path path : migrationFiles) {
                try {
                    String fileQuery = Files.readString(path); // Read the SQL file content
                    String fileName = path.getFileName().toString(); // Get the file name
                    validateFileName(fileName); // Validate the file name format

                    String version = getVersionFromFileName(fileName); // Extract version from file name
                    long checksum = ChecksumUtil.calculateChecksum(fileQuery); // Calculate checksum of the file content

                    // Check if the migration has already been executed
                    boolean isMigrationAlreadyExecuted = isMigrationExecuted(connection, version);
                    // Validate the checksum of the migration file
                    boolean isChecksumValid = isChecksumValid(connection, version, checksum);

                    // Ensure migration versions increase sequentially by 1
                    long versionGap = Long.parseLong(version) - lastMigrationVersion;
                    if (versionGap == 1) {
                        lastMigrationVersion++;
                    } else {
                        throw new MigrationVersionGapException("Version " + version + " in " + fileName + " is invalid. "
                                + "Migration versions must increase sequentially by 1.");
                    }

                    // Throw an exception if the checksum is invalid
                    if (!isChecksumValid) {
                        throw new InvalidChecksumException("Checksum mismatch for version " + version);
                    }

                    // Execute the migration if it hasn't been executed yet
                    if (!isMigrationAlreadyExecuted) {
                        wasMigrationExecuted = true;
                        long executionTime = executeMigration(connection, fileName, fileQuery); // Execute the migration
                        String installedBy = dbManager.getUser(); // Get the user who executed the migration
                        saveMigrationRecord(connection, fileName, checksum, installedBy, executionTime); // Save migration record
                    }
                } catch (IOException | SQLException | InvalidChecksumException | MigrationVersionGapException | MigrationFileNamingException e) {
                    // Log the error and roll back the transaction if an error occurs
                    logger.error("Error processing migration file: {}", path, e);
                    connection.rollback();
                    logger.info("Rolling back all the changes...");
                    return;
                }
            }

            // Commit the transaction if any migrations were executed
            if (wasMigrationExecuted) {
                logger.info("Committing all new migrations!");
            } else {
                logger.info("No new migrations to commit. Your database is up to date!");
            }
            connection.commit();
        } catch (IOException | SQLException e) {
            logger.error("Error: {}", e.getMessage());
        }
    }

    /**
     * Validates the format of the migration file name.
     *
     * @param fileName The name of the migration file.
     * @throws MigrationFileNamingException If the file name format is invalid.
     */
    public void validateFileName(String fileName) throws MigrationFileNamingException {
        boolean isMigrationFileValid = fileName.matches("^V\\d+__.+\\.[a-zA-Z0-9]+$");
        if (!isMigrationFileValid) {
            throw new MigrationFileNamingException("The migration file name is invalid: (" + fileName
                    + "). Correct format is V(version)__(description).(extension), e.g., V1__create_table.sql");
        }
    }

    /**
     * Locks the `migration_history` table to prevent concurrent migrations.
     *
     * @param connection The database connection.
     * @throws SQLException If a database access error occurs.
     */
    public void lockMigrationTable(Connection connection) throws SQLException {
        int attemptCount = 50; // Maximum number of attempts to lock the table
        int retryInterval = 1000; // Retry interval in milliseconds

        while (attemptCount > 0) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("LOCK TABLE migration_history IN EXCLUSIVE MODE"); // Lock the table
                return;
            } catch (SQLException e) {
                attemptCount--;
                if (attemptCount == 0) {
                    throw e; // Throw the exception if all attempts fail
                }

                try {
                    logger.warn("History Table is blocked now. Waiting to unlock. {} attempts left.", attemptCount);
                    Thread.sleep(retryInterval); // Wait before retrying
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for lock", interruptedException);
                }
            }
        }
    }

    /**
     * Executes a migration SQL script.
     *
     * @param connection The database connection.
     * @param fileName   The name of the migration file.
     * @param sql        The SQL script to execute.
     * @return The execution time in milliseconds.
     * @throws SQLException If a database access error occurs.
     */
    public long executeMigration(Connection connection, String fileName, String sql) throws SQLException {
        long executionTime = -1;
        try (Statement statement = connection.createStatement()) {
            StatementProxy statementProxy = new StatementProxy(statement); // Use a proxy to track execution time
            statementProxy.execute(sql); // Execute the SQL script
            executionTime = statementProxy.getExecutionTime(); // Get the execution time
            logger.info("Preparing execution: {}", fileName);
        }
        return executionTime;
    }

    /**
     * Saves a record of the executed migration in the `migration_history` table.
     *
     * @param connection    The database connection.
     * @param fileName      The name of the migration file.
     * @param checksum      The checksum of the migration file.
     * @param user          The user who executed the migration.
     * @param executionTime The execution time in milliseconds.
     * @throws SQLException If a database access error occurs.
     */
    public void saveMigrationRecord(Connection connection, String fileName, long checksum, String user, long executionTime) throws SQLException,MigrationFileNamingException {
        String insertQuery = "INSERT INTO migration_history(version, description, checksum, installed_by, execution_time_ms) VALUES (?, ?, ?, ?, ?)";

        String version = getVersionFromFileName(fileName); // Extract version from file name
        String description = getDescFromFileName(fileName); // Extract description from file name

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, version);
            preparedStatement.setString(2, description);
            preparedStatement.setLong(3, checksum);
            preparedStatement.setString(4, user);
            preparedStatement.setLong(5, executionTime);

            preparedStatement.executeUpdate(); // Insert the migration record
        }
    }

    /**
     * Checks if a migration with the specified version has already been executed.
     *
     * @param connection The database connection.
     * @param version    The migration version.
     * @return True if the migration has already been executed, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    private boolean isMigrationExecuted(Connection connection, String version) throws SQLException {
        String query = "SELECT COUNT(*) FROM migration_history WHERE version = ?";
        boolean isVersionAlreadyMigrated = false;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
             ) {
            preparedStatement.setString(1, version);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
            if (resultSet.next()) {
                isVersionAlreadyMigrated = resultSet.getInt(1) > 0;
            }}
        }
        return isVersionAlreadyMigrated;
    }

    /**
     * Validates the checksum of a migration file against the checksum stored in the database.
     *
     * @param connection The database connection.
     * @param version    The migration version.
     * @param checksum   The checksum of the migration file.
     * @return True if the checksum is valid, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    private boolean isChecksumValid(Connection connection, String version, long checksum) throws SQLException {
        String query = "SELECT checksum FROM migration_history WHERE version = ?";
        boolean isChecksumValid = true;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, version);
            try(ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    isChecksumValid = resultSet.getLong(1) == checksum;
                }
            }
        }
        return isChecksumValid;
    }

    /**
     * Extracts the version number from the migration file name.
     *
     * @param fileName The name of the migration file.
     * @return The version number as a string.
     */
    public String getVersionFromFileName(String fileName) {
        String[] splittedString = fileName.split("__");
        String versionWithV = splittedString[0];
        return versionWithV.substring(1); // Remove the 'V' prefix
    }

    /**
     * Extracts the description from the migration file name.
     *
     * @param fileName The name of the migration file.
     * @return The description as a string.
     */
    public String getDescFromFileName(String fileName) throws MigrationFileNamingException{
        if(!fileName.contains("__")){
            throw new MigrationFileNamingException("The migration file name is invalid: (" + fileName
                    + "). Correct format is V(version)__(description).(extension), e.g., V1__create_table.sql");
        }

        int firstDoubleUnderScoreIndex = fileName.indexOf("__") + 2;
        String withoutVersion = fileName.substring(firstDoubleUnderScoreIndex);
        int lastDotIndex = withoutVersion.lastIndexOf('.');
        String leftPart = (lastDotIndex != -1) ? withoutVersion.substring(0, lastDotIndex) : withoutVersion;
        return leftPart.replaceAll("_", " "); // Replace underscores with spaces
    }
}
