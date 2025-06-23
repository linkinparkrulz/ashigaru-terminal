package com.samourai.wallet.send.exceptions;

public class SignTxLengthException extends SignTxException {
    public SignTxLengthException() {
        super("Transaction length too long");
    }

    public SignTxLengthException(Exception cause) {
        super(cause);
    }
}
