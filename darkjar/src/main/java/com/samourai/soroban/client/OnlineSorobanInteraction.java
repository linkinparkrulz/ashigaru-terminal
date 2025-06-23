package com.samourai.soroban.client;

import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;

public class OnlineSorobanInteraction extends SorobanInteraction {
  private SorobanInteraction interaction;
  private SorobanService sorobanService;

  public OnlineSorobanInteraction(SorobanInteraction interaction, SorobanService sorobanService) {
    super(interaction);
    this.interaction = interaction;
    this.sorobanService = sorobanService;
  }

  public SorobanInteraction getInteraction() {
    return interaction;
  }

  public void sorobanAccept() throws Exception {
    ManualCahootsMessage reply = (ManualCahootsMessage) interaction.getReplyAccept();
    OnlineCahootsMessage onlineReply = new OnlineCahootsMessage(reply);
    sorobanService.replyInteractive(onlineReply);
  }

  public void sorobanReject(String reason) throws Exception {
    sorobanService.replyInteractive(new Exception(reason));
  }
}
