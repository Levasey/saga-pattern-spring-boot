package io.github.levasey.saga.core.exceptions;

public class CreditCardPaymentDeclinedException extends RuntimeException {

    public CreditCardPaymentDeclinedException(Throwable cause) {
        super(cause);
    }

    public CreditCardPaymentDeclinedException(String message) {
        super(message);
    }
}
