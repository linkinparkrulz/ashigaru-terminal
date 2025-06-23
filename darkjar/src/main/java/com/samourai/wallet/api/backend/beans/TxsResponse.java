package com.samourai.wallet.api.backend.beans;

public class TxsResponse {
  public int n_tx;
  public int page;
  public int n_tx_page;

  public Tx[] txs;

  public TxsResponse() {}

  public static class Tx {
    public long block_height;
    public String hash;
    public int version;
    public long locktime;
    public long result;
    public long time;

    public TxInput[] inputs;
    public TxOutput[] out;
  }

  public static class TxInput {
    public int vin;
    public long sequence;
    public TxOut prev_out;
  }

  public static class TxOut {
    public String txid;
    public int vout;
    public long value;
    public UnspentOutput.Xpub xpub;
    public String addr;
    public String pubkey;
  }

  public static class TxOutput {
    public int n;
    public long value;
    public String addr;
    public String pubkey;
    public UnspentOutput.Xpub xpub;
  }

}

