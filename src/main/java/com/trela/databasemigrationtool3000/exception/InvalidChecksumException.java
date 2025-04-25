package com.trela.databasemigrationtool3000.exception;

public class InvalidChecksumException extends RuntimeException{
    public InvalidChecksumException(String message){
        super(message);
    }
}
