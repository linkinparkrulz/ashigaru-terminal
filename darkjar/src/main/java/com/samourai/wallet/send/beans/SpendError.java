package com.samourai.wallet.send.beans;

public enum SpendError {
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_FEE,
    DUST_CHANGE,
    BIP126_OUTPUT,
    MAKING,
    SIGNING;
}
