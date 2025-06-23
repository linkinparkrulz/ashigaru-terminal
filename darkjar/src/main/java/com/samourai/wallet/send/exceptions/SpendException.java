package com.samourai.wallet.send.exceptions;

import com.samourai.wallet.send.beans.SpendError;

public class SpendException extends Exception {
    private SpendError spendError;

    public SpendException(SpendError spendError) {
        this.spendError = spendError;
    }

    public SpendError getSpendError() {
        return spendError;
    }
}
