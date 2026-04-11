package com.smartcanteen.backend.exception;

public class MaxOrderLimitExceededException extends RuntimeException {
    public MaxOrderLimitExceededException(String message) {
        super(message);
    }
}
