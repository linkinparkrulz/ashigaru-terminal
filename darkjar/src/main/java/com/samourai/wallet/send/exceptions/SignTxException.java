package com.samourai.wallet.send.exceptions;

public class SignTxException extends Exception {
    public SignTxException(String msg) {
        super(msg);
    }
    public SignTxException(String msg, Exception cause) {
        super(msg, cause);
    }

    public SignTxException(Exception cause) {
        super(cause);
    }
}
