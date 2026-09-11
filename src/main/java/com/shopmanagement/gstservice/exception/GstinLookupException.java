package com.shopmanagement.gstservice.exception;

import org.springframework.http.HttpStatus;

public class GstinLookupException extends RuntimeException {

    private final HttpStatus status;

    public GstinLookupException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public GstinLookupException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
