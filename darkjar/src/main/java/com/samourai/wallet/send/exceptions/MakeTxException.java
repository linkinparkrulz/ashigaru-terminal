package com.samourai.wallet.send.exceptions;

public class MakeTxException extends Exception {
    public MakeTxException(String msg) {
        super(msg);
    }

    public MakeTxException(Exception cause) {
        super(cause);
    }
}
