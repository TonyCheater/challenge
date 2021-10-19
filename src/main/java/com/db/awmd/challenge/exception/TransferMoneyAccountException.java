package com.db.awmd.challenge.exception;

public class TransferMoneyAccountException extends RuntimeException {

    public TransferMoneyAccountException() {
        super();
    }

    public TransferMoneyAccountException(String message) {
        super(message);
    }

    public TransferMoneyAccountException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransferMoneyAccountException(Throwable cause) {
        super(cause);
    }

    protected TransferMoneyAccountException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
