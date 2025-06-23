package com.samourai.whirlpool.client.tx0;

import java.util.List;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

public class Tx0 extends Tx0Preview {
  private Transaction tx;
  private List<TransactionOutput> premixOutputs;
  private List<TransactionOutput> changeOutputs;

  public Tx0(
      Tx0Preview tx0Preview,
      Transaction tx,
      List<TransactionOutput> premixOutputs,
      List<TransactionOutput> changeOutputs) {
    super(tx0Preview);
    this.tx = tx;
    this.premixOutputs = premixOutputs;
    this.changeOutputs = changeOutputs;
  }

  public Transaction getTx() {
    return tx;
  }

  public String getTxid() {
      return tx.getHashAsString();
  }

  public List<TransactionOutput> getPremixOutputs() {
    return premixOutputs;
  }

  public List<TransactionOutput> getChangeOutputs() {
    return changeOutputs;
  }
}
