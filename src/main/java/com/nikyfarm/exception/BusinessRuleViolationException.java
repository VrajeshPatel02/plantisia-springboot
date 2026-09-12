package com.nikyfarm.exception;

/**
 * Thrown when a request is well-formed but violates a domain invariant (e.g. deleting a category
 * that still has products, a category set as its own parent).
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
