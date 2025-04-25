package com.trela.databasemigrationtool3000;

import com.trela.databasemigrationtool3000.config.DatabaseManager;
import com.trela.databasemigrationtool3000.migration.MigrationRunner;

public class DatabaseMigrationTool3000 {

    private final MigrationRunner migrator;


    public DatabaseMigrationTool3000(String url, String user, String password) {
        DatabaseManager databaseManager = new DatabaseManager(url, user, password);
        databaseManager.createMigrationTableIfNotExists();
        this.migrator = new MigrationRunner(databaseManager);
    }


    public void runMigrations(String migrationLocation) {
        migrator.runMigrations(migrationLocation);
    }
}
