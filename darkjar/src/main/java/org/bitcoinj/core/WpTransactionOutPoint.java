package org.bitcoinj.core;

import java.math.BigInteger;

public class WpTransactionOutPoint extends TransactionOutPoint {

  private static final long serialVersionUID = 1L;

  public WpTransactionOutPoint(
      NetworkParameters params,
      Sha256Hash txHash,
      int txOutputN,
      BigInteger value,
      byte[] scriptBytes)
      throws ProtocolException {
    super(params, txOutputN, new Sha256Hash(txHash.getBytes()), Coin.valueOf(value.longValue()));
    this.connectedOutput = new TransactionOutput(params, null, getValue(), scriptBytes);
  }

  @Override
  public TransactionOutput getConnectedOutput() {
    return connectedOutput;
  }
}
