package com.trela.databasemigrationtool3000;

import com.trela.databasemigrationtool3000.config.DatabaseManager;
import com.trela.databasemigrationtool3000.migration.MigrationRunner;

public class DatabaseMigrationTool3000 {

    private final DatabaseManager databaseManager;
    private final MigrationRunner migrator;

    /**
     * Constructor to initialize the migration tool with database credentials.
     * This class is designed to be used as an external library for database migration.
     *
     * @param url      Database URL
     * @param user     Database username
     * @param password Database password
     */
    public DatabaseMigrationTool3000(String url, String user, String password) {
        this.databaseManager = new DatabaseManager(url, user, password);
        this.databaseManager.createMigrationTableIfNotExists();
        this.migrator = new MigrationRunner(databaseManager);
    }

    /**
     * Runs database migrations from the specified location.
     *
     * @param migrationLocation Location of the migration files (SQL, YAML, etc.)
     */
    public void runMigrations(String migrationLocation) {
        migrator.runMigrations(migrationLocation);
    }
}
