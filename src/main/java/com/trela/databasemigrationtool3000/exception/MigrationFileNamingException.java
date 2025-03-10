package com.trela.databasemigrationtool3000.exception;

public class MigrationFileNamingException extends Exception{

    public MigrationFileNamingException(String message){
        super(message);
    }

    public MigrationFileNamingException(String message,Throwable cause){
        super(message,cause);
    }

}
