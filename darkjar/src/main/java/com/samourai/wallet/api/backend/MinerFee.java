package com.samourai.wallet.api.backend;

import java.util.Map;

public class MinerFee {
  private Map<String, Integer> feesResponse;

  public MinerFee(Map<String, Integer> feesResponse) {
    this.feesResponse = feesResponse;
  }

  public int get(MinerFeeTarget feeTarget) {
    int fee = feesResponse.get(feeTarget.getValue());
    return fee;
  }

  public Map<String, Integer> _getMap() {
    return feesResponse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MinerFee minerFee = (MinerFee) o;
    return feesResponse.equals(minerFee.feesResponse);
  }
}
