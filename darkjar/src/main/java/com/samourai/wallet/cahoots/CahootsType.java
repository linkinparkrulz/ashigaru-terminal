package com.samourai.wallet.cahoots;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;

public enum CahootsType {
    STONEWALLX2(0, "StonewallX2", true),
    STOWAWAY(1, "Stowaway", false);

    private int value;
    private String label;
    private boolean minerFeeShared;

    CahootsType(int value, String label, boolean minerFeeShared) {
        this.value = value;
        this.label = label;
        this.minerFeeShared = minerFeeShared;
    }

    public static Optional<CahootsType> find(int value) {
      for (CahootsType item : CahootsType.values()) {
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

    public String getLabel() {
        return label;
    }

    public boolean isMinerFeeShared() {
        return minerFeeShared;
    }
}