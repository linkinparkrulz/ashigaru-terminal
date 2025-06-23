package com.samourai.wallet.api.backend.websocket.beans;


public class WSSubscribeRequest {
  public WSSubscribeOperator op;
  public String at;

  public WSSubscribeRequest(WSSubscribeOperator op, String at) {
    this.op = op;
    this.at = at;
  }

}
