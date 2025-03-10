package com.trela.databasemigrationtool3000.migration;

import com.trela.databasemigrationtool3000.config.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationRunnerIntegrationTest {

    private DatabaseManager dbManager;
    private MigrationRunner migrationRunner;
    Connection connection;

    @BeforeEach
    public void setUp() throws Exception{

        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String user =  "sa";
        String password = "";

        dbManager = new DatabaseManager(url,user,password);
        migrationRunner = new MigrationRunner(dbManager);

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
        try(Connection connection = dbManager.getConnection()){
            Statement statement = connection.createStatement();
            statement.execute(migrationTableQuery);
            Thread.sleep(500);

        }
    }


//    @Test
//    public void testRunMigrations() throws Exception{
//        migrationRunner.runMigrations("src/test/resources/migrations");
//        try(Connection connection = dbManager.getConnection()){
//            ResultSet resultSet = connection.createStatement().executeQuery(
//                    "SELECT COUNT(*) FROM migration_history");
//            assertTrue(resultSet.next());
//            assertTrue(resultSet.getInt(1)>0);
//        }
//    }
//
//   @Test
//    public void testLockMigrationTable() throws Exception{
//        try(Connection connection = dbManager.getConnection()){
//            connection.setAutoCommit(false);
//            assertDoesNotThrow(()->migrationRunner.lockMigrationTable(connection));
//        }
//    }

    @Test
    public void testExecuteMigration() throws Exception{
        try(Connection connection = dbManager.getConnection()){
            long executionTime = migrationRunner.executeMigration(connection,"V1__create_table_sql","CREATE TABLE test(id int)");
            assertTrue(executionTime>0);
        }
    }













}
