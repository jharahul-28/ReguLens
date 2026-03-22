package com.rj.ReguLens.exception;

public class InvalidPolicyState extends RuntimeException {
    String message;
    public InvalidPolicyState(String message) {
        super(message);
    }
}
