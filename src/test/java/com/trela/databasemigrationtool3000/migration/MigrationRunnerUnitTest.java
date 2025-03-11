package com.trela.databasemigrationtool3000.migration;

import com.trela.databasemigrationtool3000.exception.MigrationFileNamingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationRunnerUnitTest {


    @Test
    public void testValidateFileName_ValidFileName() {
        MigrationRunner migrationRunner = new MigrationRunner(null);
        assertDoesNotThrow(() -> migrationRunner.validateFileName("V1__create_table.sql"));
    }

    @Test
    public void testValidateFileName_InvalidFileName(){
        MigrationRunner migrationRunner = new MigrationRunner(null);
        Exception exception = assertThrows(MigrationFileNamingException.class,()->
        {
            migrationRunner.validateFileName("invalid_file_name_.txt");
        });
    }

    @Test
    public void testGetVersionFromFileName(){
        MigrationRunner migrationRunner = new MigrationRunner(null);
        assertEquals("1", migrationRunner.getVersionFromFileName("V1__create_table.sql"));
    }

    @Test
    public void testGetDescFromFileName() throws MigrationFileNamingException {
        MigrationRunner migrationRunner = new MigrationRunner(null);
        assertEquals("cReated  231123312    TABLES", migrationRunner.getDescFromFileName("V5__cReated__231123312____TABLES.sql"));
    }












}
