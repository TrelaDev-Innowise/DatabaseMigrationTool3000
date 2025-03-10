package com.trela.databasemigrationtool3000.exception;

public class MigrationVersionGapException extends RuntimeException{

    public MigrationVersionGapException(String message){
        super(message);
    }

    public MigrationVersionGapException(String message,Throwable cause){
        super(message,cause);
    }


}


