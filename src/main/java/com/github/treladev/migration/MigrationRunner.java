package com.github.treladev.migration;

import com.github.treladev.config.DatabaseManager;
import com.github.treladev.exception.InvalidChecksumException;
import com.github.treladev.exception.MigrationFileNamingException;
import com.github.treladev.exception.MigrationVersionGapException;
import com.github.treladev.proxy.StatementProxy;
import com.github.treladev.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.stream.Stream;

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

    private boolean wasMigrationExecuted;
    private long lastMigrationVersion;

    public MigrationRunner(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void runMigrations(String migrationsFolder) {
        try (Connection connection = dbManager.getConnection()) {
            wasMigrationExecuted = false;
            connection.setAutoCommit(false);
            lockMigrationTable(connection);
            List<Path> migrationFiles;
            try (Stream<Path> stream = Files.list(Paths.get(migrationsFolder))) {
                migrationFiles = stream
                        .filter(path -> path.toString().toLowerCase().endsWith(".sql"))
                        .sorted()
                        .toList();
            }
            lastMigrationVersion = 0;
            for (Path path : migrationFiles) {
               boolean wasProcessingSuccessful = processSingleMigration(connection,path);
               // If exception occur stop the method and log exception
               if(!wasProcessingSuccessful) return;
            }

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


    private boolean processSingleMigration(Connection connection, Path path) throws SQLException{
        boolean wasProcessingSuccessful = true;
        try {
            String migrationFileContent = Files.readString(path);
            String migrationFileName = path.getFileName().toString();
            String currentMigrationVersion = getVersionFromFileName(migrationFileName);
            long checksum = ChecksumUtil.calculateChecksum(migrationFileContent);
            validateFileName(migrationFileName);
            validateChecksum(connection, currentMigrationVersion, checksum);
            validateVersionGap(Long.parseLong(currentMigrationVersion), migrationFileName);
            executeMigration(connection,currentMigrationVersion,migrationFileName,migrationFileContent,checksum); // Execute the migration

        } catch (IOException | SQLException | InvalidChecksumException | MigrationVersionGapException |
                 MigrationFileNamingException e) {
            logger.error("Error processing migration file: {}", path, e);
            connection.rollback();
            logger.info("Rolling back all the changes...");
            wasProcessingSuccessful = false;
        }
        return wasProcessingSuccessful;
    }





    public void validateFileName(String fileName) throws MigrationFileNamingException {
        boolean isMigrationFileValid = fileName.matches("^V\\d+__.+\\.[a-zA-Z0-9]+$");
        if (!isMigrationFileValid) {
            throw new MigrationFileNamingException("The migration file name is invalid: (" + fileName
                    + "). Correct format is V(version)__(description).(extension), e.g., V1__create_table.sql");
        }
    }

    private void validateVersionGap(long currentMigrationVersion, String fileName) throws MigrationVersionGapException{
        long versionGap = currentMigrationVersion - lastMigrationVersion;
        if (versionGap != 1){
            throw new MigrationVersionGapException("Version " + currentMigrationVersion + " in " + fileName + " is invalid. "
                    + "Migration versions must increase sequentially by 1.");
        }
        lastMigrationVersion++;
    }


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


    public void executeMigration(Connection connection, String migrationVersion, String migrationFileName,String migrationContent,long checksum) throws SQLException,MigrationFileNamingException {
        boolean isMigrationAlreadyExecuted = isMigrationExecuted(connection,migrationVersion);
        long executionTime;
        if(!isMigrationAlreadyExecuted) {
            try (Statement statement = connection.createStatement()) {
                StatementProxy statementProxy = new StatementProxy(statement); // Use a proxy to track execution time
                statementProxy.execute(migrationContent);
                executionTime = statementProxy.getExecutionTime();
                logger.info("Preparing execution: {}", migrationFileName);
                String installedBy = dbManager.getUser();
                saveMigrationRecord(connection, migrationFileName, checksum, installedBy, executionTime);
                wasMigrationExecuted = true;
            }
        }
    }


    private void saveMigrationRecord(Connection connection, String fileName, long checksum, String user, long executionTime) throws SQLException,MigrationFileNamingException {
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


    private boolean isMigrationExecuted(Connection connection, String currentMigrationVersion) throws SQLException {
        String query = "SELECT COUNT(*) FROM migration_history WHERE version = ?";
        boolean isVersionAlreadyMigrated = false;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
             ) {
            preparedStatement.setString(1, currentMigrationVersion);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
            if (resultSet.next()) {
                isVersionAlreadyMigrated = resultSet.getInt(1) > 0;
            }}
        }
        return isVersionAlreadyMigrated;
    }

    private void validateChecksum(Connection connection, String version, long checksum) throws SQLException, InvalidChecksumException {
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
        if(!isChecksumValid){
            throw new InvalidChecksumException("Checksum mismatch for version " + version);
        }
    }


    public String getVersionFromFileName(String fileName) {
        String[] splittedString = fileName.split("__");
        String versionWithV = splittedString[0];
        return versionWithV.substring(1); // Remove the 'V' prefix
    }

    public String getDescFromFileName(String fileName) throws MigrationFileNamingException{
        if(!fileName.contains("__")){
            throw new MigrationFileNamingException("The migration file name is invalid: (" + fileName
                    + "). Correct format is V(version)__(description).(extension), e.g., V1__create_table.sql");
        }

        int firstDoubleUnderScoreIndex = fileName.indexOf("__") + 2;
        String withoutVersion = fileName.substring(firstDoubleUnderScoreIndex);
        int lastDotIndex = withoutVersion.lastIndexOf('.');
        String leftPart = (lastDotIndex != -1) ? withoutVersion.substring(0, lastDotIndex) : withoutVersion;
        return leftPart.replace("_", " "); // Replace underscores with spaces
    }
}
