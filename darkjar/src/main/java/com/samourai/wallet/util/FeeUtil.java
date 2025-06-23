package com.samourai.wallet.util;

import com.samourai.wallet.send.MyTransactionOutPoint;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Vector;

public class FeeUtil {
  private static final Logger log = LoggerFactory.getLogger(FeeUtil.class);

  private static final int ESTIMATED_INPUT_LEN_P2PKH =
      158; // (148), compressed key (180 uncompressed key)
  private static final int ESTIMATED_INPUT_LEN_P2SH_P2WPKH =
      108; // p2sh, includes segwit discount (ex: 146)
  private static final int ESTIMATED_INPUT_LEN_P2WPKH = 85; // bech32, p2wpkh
  private static final int ESTIMATED_OUTPUT_LEN = 33;
  private static final int ESTIMATED_OPRETURN_LEN = 80;

  private static FeeUtil instance = null;

  public static FeeUtil getInstance() {
    if(instance == null) {
      instance = new FeeUtil();
    }
    return instance;
  }

  public int estimatedSizeSegwit(
      int inputsP2PKH,
      int inputsP2SHP2WPKH,
      int inputsP2WPKH,
      int outputsNonOpReturn,
      int outputsOpReturn) {

    int txSize =
        (outputsNonOpReturn * ESTIMATED_OUTPUT_LEN)
            + (outputsOpReturn * ESTIMATED_OPRETURN_LEN)
            + (inputsP2PKH * ESTIMATED_INPUT_LEN_P2PKH)
            + (inputsP2SHP2WPKH * ESTIMATED_INPUT_LEN_P2SH_P2WPKH)
            + (inputsP2WPKH * ESTIMATED_INPUT_LEN_P2WPKH)
            + inputsP2PKH
            + inputsP2SHP2WPKH
            + inputsP2WPKH
            + 8
            + 1
            + 1;
    if (log.isTraceEnabled()) {
      log.trace(
          "tx size estimation: "
              + txSize
              + "b ("
              + inputsP2PKH
              + " insP2PKH, "
              + inputsP2SHP2WPKH
              + " insP2SHP2WPKH, "
              + inputsP2WPKH
              + " insP2WPKH, "
              + outputsNonOpReturn
              + " outsNonOpReturn, "
              + outputsOpReturn
              + " outsOpReturn)");
    }
    return txSize;
  }

  public BigInteger estimatedFeeSegwit(int inputsP2PKH, int inputsP2SHP2WPKH, int inputsP2WPKH, int outputs, BigInteger feePerKb)   {
    int size = estimatedSizeSegwit(inputsP2PKH, inputsP2SHP2WPKH, inputsP2WPKH, outputs, 0);
    return calculateFee(size, feePerKb);
  }

  public long estimatedFeeSegwit(
      int inputsP2PKH,
      int inputsP2SHP2WPKH,
      int inputsP2WPKH,
      int outputsNonOpReturn,
      int outputsOpReturn,
      long feePerB) {
    int size =
        estimatedSizeSegwit(
            inputsP2PKH, inputsP2SHP2WPKH, inputsP2WPKH, outputsNonOpReturn, outputsOpReturn);
    long minerFee = calculateFee(size, feePerB);
    if (log.isTraceEnabled()) {
      log.trace("minerFee = " + minerFee + " (size=" + size + "b, feePerB=" + feePerB + "s/b)");
    }
    return minerFee;
  }

  public long calculateFee(int txSize, long feePerB) {
    long fee = txSize * feePerB;
    if (Math.ceil(fee) < txSize) {
      long adjustedFee = txSize + (txSize / 20);
      if (log.isTraceEnabled()) {
        log.trace("adjustedFee: " + adjustedFee + " (fee=" + fee + ", txSize=" + txSize + ")");
      }
      return adjustedFee;
    } else {
      return fee;
    }
  }

  public BigInteger calculateFee(int txSize, BigInteger feePerKb)   {
    long feePerB = toFeePerB(feePerKb);
    long fee = calculateFee(txSize, feePerB);
    return BigInteger.valueOf(fee);
  }

  protected long toFeePerB(BigInteger feePerKb) {
    long feePerB = Math.round(feePerKb.doubleValue() / 1000.0);
    return feePerB;
  }

  public BigInteger toFeePerKB(long feePerB) {
    return BigInteger.valueOf(feePerB * 1000L);
  }

  public Triple<Integer,Integer,Integer> getOutpointCount(Vector<MyTransactionOutPoint> outpoints, NetworkParameters params) {

    int p2wpkh = 0;
    int p2sh_p2wpkh = 0;
    int p2pkh = 0;

    for(MyTransactionOutPoint out : outpoints)   {
      if(FormatsUtilGeneric.getInstance().isValidBech32(out.getAddress()))    {
        p2wpkh++;
      }
      else if(Address.fromBase58(params, out.getAddress()).isP2SHAddress())    {
        p2sh_p2wpkh++;
      }
      else   {
        p2pkh++;
      }
    }

    return Triple.of(p2pkh, p2sh_p2wpkh, p2wpkh);
  }
}
