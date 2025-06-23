package com.samourai.wallet.api.backend.websocket.beans;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum WSSubscribeOperator {
    BLOCK("blocks_sub"),
    ADDR("addr_sub");

    private String value;
    WSSubscribeOperator(String value) {
        this.value = value;
    }

    public static Optional<WSSubscribeOperator> find(String value) {
        for (WSSubscribeOperator item : WSSubscribeOperator.values()) {
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
