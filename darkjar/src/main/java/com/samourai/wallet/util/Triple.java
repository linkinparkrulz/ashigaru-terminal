package com.samourai.wallet.util;

public class Triple<L, M, R> {

    private L elementLeft = null;
    private M elementMiddle = null;
    private R elementRight = null;

    public static <L, M, R> Triple<L, M, R> of(L elementLeft, M elementMiddle, R elementRight) {
        return new Triple<L, M, R>(elementLeft, elementMiddle, elementRight);
    }

    public Triple(L elementLeft, M elementMiddle, R elementRight) {
        this.elementLeft = elementLeft;
        this.elementMiddle = elementMiddle;
        this.elementRight = elementRight;
    }

    public L getLeft() {
        return elementLeft;
    }

    public M getMiddle() {
        return elementMiddle;
    }

    public R getRight() {
        return elementRight;
    }

}
