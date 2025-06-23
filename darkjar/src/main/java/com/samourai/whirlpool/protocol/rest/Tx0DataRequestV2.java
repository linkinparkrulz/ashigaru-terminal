package com.samourai.whirlpool.protocol.rest;

public class Tx0DataRequestV2 {
  public String scode;
  public String partnerId;

  public Tx0DataRequestV2() {}

  public Tx0DataRequestV2(String scode, String partnerId) {
    this.scode = scode;
    this.partnerId = partnerId;
  }
}
