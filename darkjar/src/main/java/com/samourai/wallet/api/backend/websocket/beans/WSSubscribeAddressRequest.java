package com.samourai.wallet.api.backend.websocket.beans;


public class WSSubscribeAddressRequest extends WSSubscribeRequest {
  public String addr;

  public WSSubscribeAddressRequest(String addr, String at) {
    super(WSSubscribeOperator.ADDR, at);
    this.addr = addr;
  }

}
