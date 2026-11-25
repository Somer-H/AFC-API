package com.afcgym.platform.exception;

public class OwnershipViolationException extends NotFoundException {

    public OwnershipViolationException(String message) {
        super(message);
    }
}
