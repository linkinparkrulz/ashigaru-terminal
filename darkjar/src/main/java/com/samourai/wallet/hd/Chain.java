package com.samourai.wallet.hd;

import com.google.common.base.Optional;

public enum Chain {
    RECEIVE(0),
    CHANGE(1);

    private int index;

    Chain(int index) {
        this.index = index;
    }

    public static Optional<Chain> findByIndex(int index) {
        for (Chain item : Chain.values()) {
            if (item.index == index) {
                return Optional.of(item);
            }
        }
        return Optional.absent();
    }

    public int getIndex() {
        return index;
    }
}
