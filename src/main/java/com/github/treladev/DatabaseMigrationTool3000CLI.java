package com.github.treladev;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "DatabaseMigrationTool", description = "CLI for database migration")
public class DatabaseMigrationTool3000CLI implements Callable<Integer> {

    // Database connection options
    @CommandLine.Option(names = {"--url"}, description = "JDBC URL for the database", required = true)
    private String url;

    @CommandLine.Option(names = {"-u", "--username"}, description = "Username for the database", required = true)
    private String user;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Password for the database", required = true)
    private String password;

    // Directory containing migration files
    @CommandLine.Option(names = {"-d", "--directory"}, description = "Directory for migration files", required = true)
    private String migrationDirectory;

    /**
     * Main logic for executing database migrations using provided command-line options.
     *
     * @return Exit code (0 for success)
     * @throws Exception if migration fails
     */
    public Integer call() throws Exception {
        DatabaseMigrationTool3000 databaseMigrationTool = new DatabaseMigrationTool3000(url, user, password);
        databaseMigrationTool.runMigrations(migrationDirectory);
        return 0;
    }

    /**
     * Main entry point for the CLI application.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new DatabaseMigrationTool3000CLI()).execute(args);
        System.exit(exitCode);
    }
}
