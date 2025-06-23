package com.samourai.wallet.api.backend.beans;


import java.util.LinkedHashMap;
import java.util.Map;

public class WalletResponse {
  public Wallet wallet;
  public Info info;
  public Address[] addresses;
  public Tx[] txs;
  public UnspentOutput[] unspent_outputs;

  public WalletResponse() {}

  public static class Wallet {
    public long final_balance;
  }

  public static class Info {
    public InfoBlock latest_block;
    public Map<String, Integer> fees;
  }

  public static class InfoBlock {
    public int height;
    public String hash;
    public long time;
  }

  public static class Address {
    public String address;
    public String pubkey;
    public long final_balance;
    public int account_index;
    public int change_index;
    public int n_tx;
  }

  public Map<String,Address> getAddressesMap() {
    Map<String,Address> addressesMap = new LinkedHashMap<String, Address>();
    if (addresses != null) {
      for (Address address : addresses) {
        addressesMap.put(address.address, address);
      }
    }
    return addressesMap;
  }

  public static class Tx {
    public Integer block_height; // null when unconfirmed
    public String hash;
    public int version;
    public long locktime;
    public long result;
    public long balance;
    public long time;
    public TxInput[] inputs;
    public TxOutput[] out;
  }

  public static class TxInput {
    public int vin;
    public TxOut prev_out;
    public long sequence;
  }

  public static class TxOut {
    public String txid;
    public int vout;
    public long value;
    public UnspentOutput.Xpub xpub;
    public String addr; // may be null
    public String pubkey;
  }

  public static class TxOutput {
    public int n;
    public long value;
    public String addr; // may be null
    public String pubkey;
    public UnspentOutput.Xpub xpub;
  }
}
