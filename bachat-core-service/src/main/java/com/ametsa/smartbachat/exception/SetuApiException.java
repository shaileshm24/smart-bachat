package com.ametsa.smartbachat.exception;

/**
 * Custom exception for errors occurring during interaction with the Setu API.
 */
public class SetuApiException extends RuntimeException {

    private final Integer httpStatusCode;

    public SetuApiException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public SetuApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public SetuApiException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public SetuApiException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public boolean isClientError() {
        return httpStatusCode != null && httpStatusCode >= 400 && httpStatusCode < 500;
    }

    public boolean isServerError() {
        return httpStatusCode != null && httpStatusCode >= 500;
    }
}