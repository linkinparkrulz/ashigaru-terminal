package com.samourai.whirlpool.protocol.rest;

/** for whirlpool-protocol <= 0.23.5 */
public class Tx0DataResponseV1 {
  public String feePaymentCode;
  public long feeValue;
  public long feeChange;
  public int feeDiscountPercent;
  public String message;
  public String feePayload64; // WhirlpoolProtocol.encodeBytes(sCodePayload) or null when no SCODE
  public String feeAddress;
  public Integer feeIndice;

  public Tx0DataResponseV1() {}

  public Tx0DataResponseV1(
      String feePaymentCode,
      long feeValue,
      long feeChange,
      int feeDiscountPercent,
      String message,
      String feePayload64,
      String feeAddress,
      int feeIndice) {
    this.feePaymentCode = feePaymentCode;
    this.feeValue = feeValue;
    this.feeChange = feeChange;
    this.feeDiscountPercent = feeDiscountPercent;
    this.message = message;
    this.feePayload64 = feePayload64;
    this.feeAddress = feeAddress;
    this.feeIndice = feeIndice;
  }
}
