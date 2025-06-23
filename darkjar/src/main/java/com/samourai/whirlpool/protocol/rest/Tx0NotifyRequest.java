package com.samourai.whirlpool.protocol.rest;

public class Tx0NotifyRequest {
  public String txid;
  public String poolId;

  public Tx0NotifyRequest() {}

  public Tx0NotifyRequest(String txid, String poolId) {
    this.txid = txid;
    this.poolId = poolId;
  }
}
