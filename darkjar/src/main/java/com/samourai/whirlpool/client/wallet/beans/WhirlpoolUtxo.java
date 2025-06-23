package com.samourai.whirlpool.client.wallet.beans;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.AddressType;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfig;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigPersisted;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class WhirlpoolUtxo {
  private static final Logger log = LoggerFactory.getLogger(WhirlpoolUtxo.class);
  private static final int MIX_MIN_CONFIRMATIONS = 1;

  private UnspentOutput utxo;
  private Integer blockHeight; // null when unconfirmed
  private WhirlpoolAccount account;
  private AddressType addressType;
  private WhirlpoolUtxoState utxoState;

  private UtxoConfigSupplier utxoConfigSupplier;

  public WhirlpoolUtxo(
      UnspentOutput utxo,
      WhirlpoolAccount account,
      AddressType addressType,
      String poolId,
      UtxoConfigSupplier utxoConfigSupplier,
      int latestBlockHeight) {
    super();
    this.utxo = utxo;
    this.blockHeight = computeBlockHeight(utxo.confirmations, latestBlockHeight);
    this.account = account;
    this.addressType = addressType;
    this.utxoState = new WhirlpoolUtxoState(poolId);
    this.utxoConfigSupplier = utxoConfigSupplier;

    this.setMixableStatus(latestBlockHeight);
  }

  private Integer computeBlockHeight(int utxoConfirmations, int latestBlockHeight) {
    if (utxoConfirmations <= 0) {
      return null;
    }
    return latestBlockHeight - utxoConfirmations;
  }

  private void setMixableStatus(int latestBlockHeight) {
    MixableStatus mixableStatus = computeMixableStatus(latestBlockHeight);
    utxoState.setMixableStatus(mixableStatus);
  }

  public void setUtxoConfirmed(UnspentOutput utxo, int latestBlockHeight) {
    this.utxo = utxo;
    this.blockHeight = computeBlockHeight(utxo.confirmations, latestBlockHeight);
    this.setMixableStatus(latestBlockHeight);
  }

  private MixableStatus computeMixableStatus(int latestBlockHeight) {
    // check pool
    if (utxoState.getPoolId() == null) {
      return MixableStatus.NO_POOL;
    }

    // check confirmations
    if (computeConfirmations(latestBlockHeight) < MIX_MIN_CONFIRMATIONS) {
      return MixableStatus.UNCONFIRMED;
    }

    // ok
    return MixableStatus.MIXABLE;
  }

  public static long sumValue(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    long sumValue = 0;
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      sumValue += whirlpoolUtxo.getUtxo().value;
    }
    return sumValue;
  }

  public int computeConfirmations(int latestBlockHeight) {
    if (blockHeight == null) {
      return 0;
    }
    return latestBlockHeight - blockHeight;
  }

  public UtxoConfig getUtxoConfigOrDefault() {
    UtxoConfig utxoConfig = utxoConfigSupplier.getUtxo(utxo.tx_hash, utxo.tx_output_n);
    if (utxoConfig == null) {
      int mixsDone = account == WhirlpoolAccount.POSTMIX ? 1 : 0;
      utxoConfig = new UtxoConfigPersisted(mixsDone, null);
    }
    return utxoConfig;
  }

  public int getMixsDone() {
    return getUtxoConfigOrDefault().getMixsDone();
  }

  public void setMixsDone(int mixsDone) {
    utxoConfigSupplier.setUtxo(utxo.tx_hash, utxo.tx_output_n, mixsDone);
  }

  public UnspentOutput getUtxo() {
    return utxo;
  }

  public Integer getBlockHeight() {
    return blockHeight;
  }

  public WhirlpoolAccount getAccount() {
    return account;
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public WhirlpoolUtxoState getUtxoState() {
    return utxoState;
  }

  public boolean isAccountDeposit() {
    return WhirlpoolAccount.DEPOSIT.equals(account);
  }

  public boolean isAccountPremix() {
    return WhirlpoolAccount.PREMIX.equals(account);
  }

  public boolean isAccountPostmix() {
    return WhirlpoolAccount.POSTMIX.equals(account);
  }

  public String getPathFull() {
    return utxo.getPathFull(addressType.getPurpose(), account.getAccountIndex());
  }

  @Override
  public String toString() {
    UtxoConfig utxoConfig = getUtxoConfigOrDefault();
    return account
        + " / "
        + addressType
        + ": "
        + utxo.toString()
        + ", blockHeight="
        + (blockHeight != null ? blockHeight : "null")
        + ", state={"
        + utxoState
        + "}, utxoConfig={"
        + utxoConfig
        + "}";
  }

  public String getDebug() {
    StringBuilder sb = new StringBuilder();
    sb.append(toString());
    sb.append(", path=").append(getPathFull());

    String poolId = getUtxoState().getPoolId();
    sb.append(", poolId=").append((poolId != null ? poolId : "null"));
    sb.append(", mixsDone=").append(getMixsDone());
    sb.append(", state={").append(getUtxoState().toString()).append("}");
    return sb.toString();
  }
}
