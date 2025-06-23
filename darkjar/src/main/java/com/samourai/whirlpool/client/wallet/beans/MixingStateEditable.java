package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.whirlpool.client.event.MixStateChangeEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

import java.util.Collection;

public class MixingStateEditable extends MixingState {
  private WhirlpoolWallet whirlpoolWallet;

  public MixingStateEditable(WhirlpoolWallet whirlpoolWallet, boolean started) {
    super(started);
    this.whirlpoolWallet = whirlpoolWallet;
  }

  @Override
  protected void emit() {
    super.emit();
    WhirlpoolEventService.getInstance().post(new MixStateChangeEvent(whirlpoolWallet));
  }

  @Override
  public void setStarted(boolean started) {
    super.setStarted(started);
  }

  @Override
  public synchronized void set(
          Collection<WhirlpoolUtxo> utxosMixing, Collection<WhirlpoolUtxo> utxosQueued) {
    super.set(utxosMixing, utxosQueued);
  }

  @Override
  public void setUtxosMixing(Collection<WhirlpoolUtxo> utxosMixing) {
    super.setUtxosMixing(utxosMixing);
  }

  @Override
  public synchronized void setUtxosQueued(Collection<WhirlpoolUtxo> utxosQueued) {
    super.setUtxosQueued(utxosQueued);
  }

  public synchronized void incrementUtxoQueued(WhirlpoolUtxo utxoQueued) {
    super.incrementUtxoQueued(utxoQueued);
  }
}
