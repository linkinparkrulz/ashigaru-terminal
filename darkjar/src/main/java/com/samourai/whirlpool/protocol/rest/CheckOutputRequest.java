package com.samourai.whirlpool.protocol.rest;

public class CheckOutputRequest {
  public String receiveAddress;
  public String signature;

  public CheckOutputRequest() {}

  public CheckOutputRequest(String receiveAddress, String signature) {
    this.receiveAddress = receiveAddress;
    this.signature = signature;
  }
}
