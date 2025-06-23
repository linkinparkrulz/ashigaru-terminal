package com.samourai.soroban.client.cahoots;

import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.wallet.cahoots.Cahoots;
import org.json.JSONObject;

public class OnlineCahootsMessage extends ManualCahootsMessage {

  private OnlineCahootsMessage(Cahoots cahoots) {
    super(cahoots);
  }

  public OnlineCahootsMessage(ManualCahootsMessage msg) {
    this(msg.getCahoots());
  }

  public static OnlineCahootsMessage parse(String payload) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("cahoots")) {
      throw new Exception("missing .cahoots");
    }

    Cahoots cahoots = Cahoots.parse(obj.getString("cahoots"));
    return new OnlineCahootsMessage(cahoots);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("cahoots", getCahoots().toJSONString());
    return obj.toString();
  }

  @Override
  public String toString() {
    return "(OnlineCahootsMessage)" + super.toString();
  }
}
