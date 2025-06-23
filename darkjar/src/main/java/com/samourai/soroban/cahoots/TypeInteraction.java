package com.samourai.soroban.cahoots;

import com.samourai.wallet.cahoots.CahootsTypeUser;
import java8.util.Optional;

public enum TypeInteraction {
  TX_BROADCAST(CahootsTypeUser.SENDER, 4);

  private CahootsTypeUser typeUser;
  private int step;

  TypeInteraction(CahootsTypeUser typeUser, int step) {
    this.typeUser = typeUser;
    this.step = step;
  }

  public static Optional<TypeInteraction> find(CahootsTypeUser typeUser, int step) {
    for (TypeInteraction item : TypeInteraction.values()) {
      if (item.typeUser.equals(typeUser) && item.step == step) {
        return Optional.of(item);
      }
    }
    return Optional.empty();
  }

  public int getStep() {
    return step;
  }
}
