package com.samourai.wallet.cahoots;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum CahootsTypeUser {
    SENDER(0),
    COUNTERPARTY(1);

    private int value;

    CahootsTypeUser(int value) {
        this.value = value;
    }

    public static Optional<CahootsTypeUser> find(int value) {
      for (CahootsTypeUser item : CahootsTypeUser.values()) {
          if (item.value == value) {
              return Optional.of(item);
          }
      }
      return Optional.absent();
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public CahootsTypeUser getPartner() {
        return SENDER.equals(this) ? COUNTERPARTY : SENDER;
    }
}