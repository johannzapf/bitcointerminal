package de.johannzapf.bitcoin.terminal.exception;

public class PaymentFailedException extends RuntimeException{

    public PaymentFailedException() {
        super();
    }

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
