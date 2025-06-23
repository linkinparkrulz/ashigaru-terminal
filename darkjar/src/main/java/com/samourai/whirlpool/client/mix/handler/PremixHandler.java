package com.samourai.whirlpool.client.mix.handler;

import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

public class PremixHandler implements IPremixHandler {
  private UtxoWithBalance utxo;
  private ECKey utxoKey;
  private String userPreHash;

  public PremixHandler(UtxoWithBalance utxo, ECKey utxoKey, String userPreHash) {
    this.utxo = utxo;
    this.utxoKey = utxoKey;
    this.userPreHash = userPreHash;
  }

  @Override
  public UtxoWithBalance getUtxo() {
    return utxo;
  }

  @Override
  public void signTransaction(Transaction tx, int inputIndex, NetworkParameters params)
      throws Exception {
    // TODO SendFactoryGeneric.getInstance().signInput(utxoKey, params, tx, inputIndex);
    long spendAmount = utxo.getBalance();
    signInputSegwit(tx, inputIndex, utxoKey, spendAmount, params);
  }

  // TODO
  protected void signInputSegwit(
          Transaction tx, int inputIdx, ECKey ecKey, long spendAmount, NetworkParameters params) {
    final SegwitAddress segwitAddress = new SegwitAddress(ecKey, params);
    final Script redeemScript = segwitAddress.segWitRedeemScript();
    final Script scriptCode = redeemScript.scriptCode();

    TransactionSignature sig =
        tx.calculateWitnessSignature(
            inputIdx, ecKey, scriptCode, Coin.valueOf(spendAmount), Transaction.SigHash.ALL, false);
    final TransactionWitness witness = new TransactionWitness(2);
    witness.setPush(0, sig.encodeToBitcoin());
    witness.setPush(1, ecKey.getPubKey());
    tx.setWitness(inputIdx, witness);
  }

  @Override
  public String signMessage(String message) {
    return utxoKey.signMessage(message);
  }

  @Override
  public String computeUserHash(String salt) {
    return ClientUtils.sha256Hash(salt + userPreHash);
  }
}
