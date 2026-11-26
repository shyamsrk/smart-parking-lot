package com.smartparkinglot.application;

public final class NoCompatibleSpotException extends RuntimeException {
    public NoCompatibleSpotException(String message) {
        super(message);
    }
}
