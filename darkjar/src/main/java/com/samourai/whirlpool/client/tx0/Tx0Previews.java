package com.samourai.whirlpool.client.tx0;

import java.util.Collection;
import java.util.Map;

public class Tx0Previews {
  private Map<String, Tx0Preview> tx0PreviewsByPoolId;

  public Tx0Previews(Map<String, Tx0Preview> tx0PreviewsByPoolId) {
    this.tx0PreviewsByPoolId = tx0PreviewsByPoolId;
  }

  public Collection<Tx0Preview> getTx0Previews() {
    return tx0PreviewsByPoolId.values();
  }

  public Tx0Preview getTx0Preview(String poolId) {
    return tx0PreviewsByPoolId.get(poolId);
  }

  @Override
  public String toString() {
    return "tx0PreviewsByPoolId={" + tx0PreviewsByPoolId.toString() + "}";
  }
}
