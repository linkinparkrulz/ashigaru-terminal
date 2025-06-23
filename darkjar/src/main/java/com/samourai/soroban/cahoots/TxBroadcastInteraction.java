package com.samourai.soroban.cahoots;

import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.soroban.client.SorobanInteraction;

public class TxBroadcastInteraction extends SorobanInteraction {
    private Cahoots signedCahoots;

    public TxBroadcastInteraction(Cahoots signedCahoots) {
        super(TypeInteraction.TX_BROADCAST, new ManualCahootsMessage(signedCahoots));
        this.signedCahoots = signedCahoots;
    }

    public Cahoots getSignedCahoots() {
        return signedCahoots;
    }
}
