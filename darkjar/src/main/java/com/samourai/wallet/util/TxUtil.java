package com.samourai.wallet.util;

import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxUtil {
  private static final Logger log = LoggerFactory.getLogger(TxUtil.class);

  private static TxUtil instance = null;

  private static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  public static TxUtil getInstance() {
    if(instance == null) {
      instance = new TxUtil();
    }
    return instance;
  }

  public void verifySignInput(Transaction tx, int inputIdx, long inputValue, byte[] connectedScriptBytes) throws Exception {
    Script connectedScript = new Script(connectedScriptBytes);
    tx.getInput(inputIdx).getScriptSig().correctlySpends(tx, inputIdx, connectedScript, Coin.valueOf(inputValue), Script.ALL_VERIFY_FLAGS);
  }

  public Integer findInputIndex(Transaction tx, String txoHash, long txoIndex) {
    for (int i = 0; i < tx.getInputs().size(); i++) {
      TransactionInput input = tx.getInput(i);
      TransactionOutPoint outPoint = input.getOutpoint();
      if (outPoint.getHash().toString().equals(txoHash) && outPoint.getIndex() == txoIndex) {
        return i;
      }
    }
    return null;
  }

  public byte[] findInputPubkey(Transaction tx, int inputIndex, Callback<byte[]> fetchInputOutpointScriptBytes) {
    TransactionInput transactionInput = tx.getInput(inputIndex);
    if (transactionInput == null) {
      return null;
    }

    // try P2WPKH / P2SH-P2WPKH: get from witness
    byte[] inputPubkey = null;
    try {
      inputPubkey = tx.getWitness(inputIndex).getPush(1);
      if (inputPubkey != null) {
        return inputPubkey;
      }
    } catch(Exception e) {
      // witness not found
    }

    // try P2PKH: get from input script
    Script inputScript = new Script(transactionInput.getScriptBytes());
    try {
      inputPubkey = inputScript.getPubKey();
      if (inputPubkey != null) {
        return inputPubkey;
      }
    } catch(Exception e) {
      // not P2PKH
    }

    // try P2PKH: get pubkey from input script
    if (fetchInputOutpointScriptBytes != null) {
      byte[] inputOutpointScriptBytes = fetchInputOutpointScriptBytes.execute();
      if (inputOutpointScriptBytes != null) {
        inputPubkey = new Script(inputOutpointScriptBytes).getPubKey();
      }
    }
    return inputPubkey;
  }

  public String getToAddress(TransactionOutput output) {
    String outputScript = Hex.toHexString(output.getScriptBytes());
    if (bech32Util.isBech32Script(outputScript)) {
      // bech32
      try {
        String outputAddress = bech32Util.getAddressFromScript(outputScript, output.getParams());
        return outputAddress;
      } catch (Exception e) {
        log.error("", e);
      }
    } else {
      // P2PKH
      try {
        return output.getAddressFromP2PKHScript(output.getParams()).toString();
      } catch (Exception e) {}
      // P2SH
      try {
        return output.getAddressFromP2SH(output.getParams()).toString();
      } catch (Exception e) {}
    }
    return null;
  }
}
