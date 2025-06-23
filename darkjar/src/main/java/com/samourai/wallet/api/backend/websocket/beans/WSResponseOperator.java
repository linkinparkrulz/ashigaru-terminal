package com.samourai.wallet.api.backend.websocket.beans;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum WSResponseOperator {
    BLOCK("block"),
    UTXO("utx");

    private String value;
    WSResponseOperator(String value) {
        this.value = value;
    }

    public static Optional<WSResponseOperator> find(String value) {
        for (WSResponseOperator item : WSResponseOperator.values()) {
            if (item.value.equals(value)) {
                return Optional.of(item);
            }
        }
        return Optional.absent();
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
