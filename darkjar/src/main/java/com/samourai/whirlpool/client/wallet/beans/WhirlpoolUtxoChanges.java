package com.samourai.whirlpool.client.wallet.beans;

import java.util.ArrayList;
import java.util.Collection;

public class WhirlpoolUtxoChanges {
  private boolean isFirstFetch;
  private Collection<WhirlpoolUtxo> utxosAdded;
  private Collection<WhirlpoolUtxo> utxosConfirmed;
  private Collection<WhirlpoolUtxo> utxosRemoved;

  public WhirlpoolUtxoChanges(boolean isFirstFetch) {
    this.isFirstFetch = isFirstFetch;
    this.utxosAdded = new ArrayList<WhirlpoolUtxo>();
    this.utxosConfirmed = new ArrayList<WhirlpoolUtxo>();
    this.utxosRemoved = new ArrayList<WhirlpoolUtxo>();
  }

  public boolean isEmpty() {
    return utxosAdded.isEmpty() && utxosConfirmed.isEmpty() && utxosRemoved.isEmpty();
  }

  public boolean isFirstFetch() {
    return isFirstFetch;
  }

  public Collection<WhirlpoolUtxo> getUtxosAdded() {
    return utxosAdded;
  }

  public Collection<WhirlpoolUtxo> getUtxosConfirmed() {
    return utxosConfirmed;
  }

  public Collection<WhirlpoolUtxo> getUtxosRemoved() {
    return utxosRemoved;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "unchanged";
    }
    return utxosAdded.size()
        + " added, "
        + utxosConfirmed.size()
        + " confirmed, "
        + utxosRemoved.size()
        + " removed";
  }
}
