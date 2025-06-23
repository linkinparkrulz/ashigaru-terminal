package com.samourai.whirlpool.protocol.rest;

/** for whirlpool-protocol > 0.23.5 */
public class Tx0DataResponseV2 {
  public Tx0Data[] tx0Datas;

  public Tx0DataResponseV2() {}

  public Tx0DataResponseV2(Tx0Data[] tx0Datas) {
    this.tx0Datas = tx0Datas;
  }

  public static class Tx0Data {
    public String poolId;
    public String feePaymentCode;
    public long feeValue;
    public long feeChange;
    public int feeDiscountPercent;
    public String message;
    public String
        feePayload64; // encodeBytes(encodeFeePayload(feeIndex, scodePayload, partnerPayload))
    public String feeAddress;

    public Tx0Data() {}

    public Tx0Data(
        String poolId,
        String feePaymentCode,
        long feeValue,
        long feeChange,
        int feeDiscountPercent,
        String message,
        String feePayload64,
        String feeAddress) {
      this.poolId = poolId;
      this.feePaymentCode = feePaymentCode;
      this.feeValue = feeValue;
      this.feeChange = feeChange;
      this.feeDiscountPercent = feeDiscountPercent;
      this.message = message;
      this.feePayload64 = feePayload64;
      this.feeAddress = feeAddress;
    }
  }
}
