package com.github.treladev.exception;

public class InvalidChecksumException extends RuntimeException{
    public InvalidChecksumException(String message){
        super(message);
    }
}
