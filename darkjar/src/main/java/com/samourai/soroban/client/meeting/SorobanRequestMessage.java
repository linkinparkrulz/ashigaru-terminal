package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.AbstractSorobanMessage;
import com.samourai.wallet.cahoots.CahootsType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanRequestMessage extends AbstractSorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(SorobanRequestMessage.class);

  private CahootsType type;
  private String sender;

  public SorobanRequestMessage() {
    this(null);
  }

  public SorobanRequestMessage(CahootsType type) {
    super(true);
    this.type = type;
    this.sender = null;
  }

  public static SorobanRequestMessage parse(String payload) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("type")) {
      throw new Exception("missing .type");
    }
    CahootsType type = CahootsType.find(obj.getInt("type")).get();
    return new SorobanRequestMessage(type);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("type", type.getValue());
    // 'sender' is transmitted in SorobanMessageWithSender
    return obj.toString();
  }

  @Override
  public boolean isDone() {
    return true;
  }

  public CahootsType getType() {
    return type;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }
}
