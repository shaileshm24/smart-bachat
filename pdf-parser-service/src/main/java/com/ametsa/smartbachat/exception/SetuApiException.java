package com.ametsa.smartbachat.exception;

/**
 * Custom exception for errors occurring during interaction with the Setu API.
 */
public class SetuApiException extends RuntimeException {

    public SetuApiException(String message) {
        super(message);
    }

    public SetuApiException(String message, Throwable cause) {
        super(message, cause);
    }
}