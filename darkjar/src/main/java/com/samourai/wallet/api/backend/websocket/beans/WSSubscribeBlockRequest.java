package com.samourai.wallet.api.backend.websocket.beans;


public class WSSubscribeBlockRequest extends WSSubscribeRequest {

  public WSSubscribeBlockRequest(String at) {
    super(WSSubscribeOperator.BLOCK, at);
  }

}
