package model;

public class IllegalMoveException extends RuntimeException {
    public IllegalMoveException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalMoveException(String message) {
        super(message);
    }
}
