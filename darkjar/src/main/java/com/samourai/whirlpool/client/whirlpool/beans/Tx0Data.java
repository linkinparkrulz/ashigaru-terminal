package com.samourai.whirlpool.client.whirlpool.beans;

import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;

public class Tx0Data {
  private String poolId;
  private String feePaymentCode;
  private long feeValue;
  private long feeChange;
  private int feeDiscountPercent;
  private String message;
  private byte[] feePayload;
  private String feeAddress;

  public Tx0Data(Tx0DataResponseV2.Tx0Data tx0DataItem) throws Exception {
    this(
        tx0DataItem.poolId,
        tx0DataItem.feePaymentCode,
        tx0DataItem.feeValue,
        tx0DataItem.feeChange,
        tx0DataItem.feeDiscountPercent,
        tx0DataItem.message,
        WhirlpoolProtocol.decodeBytes(tx0DataItem.feePayload64),
        tx0DataItem.feeAddress);
  }

  public Tx0Data(
      String poolId,
      String feePaymentCode,
      long feeValue,
      long feeChange,
      int feeDiscountPercent,
      String message,
      byte[] feePayload,
      String feeAddress)
      throws Exception {
    if (feePayload == null) {
      throw new Exception("Invalid Tx0Data.feePayload: null");
    }
    this.poolId = poolId;
    this.feePaymentCode = feePaymentCode;
    this.feeValue = feeValue;
    this.feeChange = feeChange;
    this.feeDiscountPercent = feeDiscountPercent;
    this.message = message;
    this.feePayload = feePayload;
    this.feeAddress = feeAddress;
  }

  public long computeFeeValueOrFeeChange() {
    return feeValue > 0 ? feeValue : feeChange;
  }

  public String getPoolId() {
    return poolId;
  }

  public String getFeePaymentCode() {
    return feePaymentCode;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public long getFeeChange() {
    return feeChange;
  }

  public int getFeeDiscountPercent() {
    return feeDiscountPercent;
  }

  public String getMessage() {
    return message;
  }

  public byte[] getFeePayload() {
    return feePayload;
  }

  public String getFeeAddress() {
    return feeAddress;
  }

  @Override
  public String toString() {
    return "poolId="
        + poolId
        + ", feePaymentCode="
        + feePaymentCode
        + ", feeValue="
        + feeValue
        + ", feeChange="
        + feeChange
        + ", feeDiscountPercent="
        + feeDiscountPercent
        + ", message="
        + message
        + ", feePayload="
        + feePayload
        + ", feeAddress="
        + (feeAddress != null ? feeAddress : "null");
  }
}
