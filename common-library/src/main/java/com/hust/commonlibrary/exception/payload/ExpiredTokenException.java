package com.hust.commonlibrary.exception.payload;

public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException(String message) {
        super(message);
    }

}