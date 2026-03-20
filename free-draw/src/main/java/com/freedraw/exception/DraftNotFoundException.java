package com.freedraw.exception;

public class DraftNotFoundException extends RuntimeException {
    public DraftNotFoundException(String errMessage) {
        super(errMessage);
    }
}
