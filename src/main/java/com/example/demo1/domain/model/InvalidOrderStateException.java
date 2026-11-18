package com.example.demo1.domain.model;

public class InvalidOrderStateException extends IllegalStateException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
