package com.samourai.wallet.send;

import com.samourai.wallet.SamouraiWalletConst;
import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.exceptions.MakeTxException;
import com.samourai.wallet.send.exceptions.SignTxException;
import com.samourai.wallet.send.exceptions.SignTxLengthException;
import com.samourai.wallet.send.provider.UtxoKeyProvider;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.util.TxUtil;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

//import android.util.Log;

public class SendFactoryGeneric {
    private static final Logger log = LoggerFactory.getLogger(SendFactoryGeneric.class);

    private static SendFactoryGeneric instance = null;
    public static SendFactoryGeneric getInstance() {
        if (instance == null) {
            instance = new SendFactoryGeneric();
        }
        return instance;
    }

    protected SendFactoryGeneric() { ; }

    /*
    Used by spends
     */
    public Transaction makeTransaction(Map<String, Long> receivers, List<MyTransactionOutPoint> unspent, boolean rbfOptIn, NetworkParameters params) throws MakeTxException {

        BigInteger amount = BigInteger.ZERO;
        for(Iterator<Map.Entry<String, Long>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Long> mapEntry = iterator.next();
            amount = amount.add(BigInteger.valueOf(mapEntry.getValue()));
        }

        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        Transaction tx = new Transaction(params);

        for(Iterator<Map.Entry<String, Long>> iterator = receivers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Long> mapEntry = iterator.next();
            String toAddress = mapEntry.getKey();
            BigInteger value = BigInteger.valueOf(mapEntry.getValue());
/*
            if(value.compareTo(SamouraiWallet.bDust) < 1)    {
                throw new Exception(context.getString(R.string.dust_amount));
            }
*/
            if(value == null || (value.compareTo(BigInteger.ZERO) <= 0 && !FormatsUtilGeneric.getInstance().isValidBIP47OpReturn(toAddress))) {
                throw new MakeTxException("Invalid amount");
            }

            TransactionOutput output = null;
            Script toOutputScript = null;
            if(!FormatsUtilGeneric.getInstance().isValidBitcoinAddress(toAddress, params) && FormatsUtilGeneric.getInstance().isValidBIP47OpReturn(toAddress))    {
                toOutputScript = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(Hex.decode(toAddress)).build();
                output = new TransactionOutput(params, null, Coin.valueOf(0L), toOutputScript.getProgram());
            }
            else {
                try {
                    output = computeTransactionOutput(toAddress, value.longValue(), params);
                } catch (Exception e) {
                    log.error("computeTransactionOutput failed", e);
                    throw new MakeTxException(e);
                }
            }
            outputs.add(output);
        }

        List<TransactionInput> inputs = new ArrayList<>();
        for(MyTransactionOutPoint outPoint : unspent) {
            Script script = outPoint.computeScript();
            if(script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            TransactionInput input = outPoint.computeSpendInput();
            if(rbfOptIn == true)    {
                input.setSequenceNumber(SamouraiWalletConst.RBF_SEQUENCE_VAL.longValue());
            }
            inputs.add(input);
        }

        //
        // deterministically sort inputs and outputs, see BIP69 (OBPP)
        //
        Collections.sort(inputs, new BIP69InputComparator());
        for(TransactionInput input : inputs) {
            tx.addInput(input);
        }

        Collections.sort(outputs, new BIP69OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        return tx;
    }

    public Transaction signTransaction(Transaction unsignedTx, UtxoKeyProvider utxoProvider) throws SignTxException {
        HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();

        for (TransactionInput input : unsignedTx.getInputs()) {
            try {
//                Log.i("SendFactory", "connected pubkey script:" + Hex.toHexString(scriptBytes));
//                Log.i("SendFactory", "address from script:" + address);
                String hash = input.getOutpoint().getHash().toString();
                int index = (int)input.getOutpoint().getIndex();
                ECKey ecKey = utxoProvider._getPrivKey(hash, index);
                if(ecKey == null) {
                    throw new Exception("Key not found for input: "+hash+":"+index);
                }
                keyBag.put(input.getOutpoint().toString(), ecKey);
            }
            catch(Exception e) {
                throw new SignTxException(e);
            }
        }

        Transaction signedTx = signTransaction(unsignedTx, keyBag);
        if(signedTx == null)    {
            return null;
        }
        else    {
            String hexString = new String(Hex.encode(signedTx.bitcoinSerialize()));
            if(hexString.length() > (100 * 1024)) {
                log.warn("Transaction length too long: txLength="+hexString.length());
                throw new SignTxLengthException();
//              Log.i("SendFactory", "Transaction length too long");
            }

            return signedTx;
        }
    }

    public synchronized Transaction signTransaction(Transaction transaction, Map<String,ECKey> keyBag) throws SignTxException {
        NetworkParameters params = transaction.getParams();
        List<TransactionInput> inputs = transaction.getInputs();

        for (int i = 0; i < inputs.size(); i++) {
            TransactionInput input = transaction.getInput(i);
            ECKey key = keyBag.get(input.getOutpoint().toString());
            try {
                this.signInput(key, params, transaction, i);
            } catch (Exception e) {
                log.error("Signing input #"+i+" failed", e);
                throw new SignTxException("Signing input #"+i+" failed", e);
            }
        }
        return transaction;
    }

    public void signInput(ECKey key, NetworkParameters params, Transaction tx, int inputIndex) throws Exception {
        if (key == null) {
            throw new Exception("No key found for signing input #"+inputIndex);
        }

        TransactionInput txInput = tx.getInput(inputIndex);
        TransactionOutput connectedOutput = txInput.getOutpoint().getConnectedOutput();
        Script scriptPubKey = connectedOutput.getScriptPubKey();
        Coin value = txInput.getValue();

        // sign input
        String inputAddress = TxUtil.getInstance().getToAddress(connectedOutput);
        AddressType addressType = AddressType.findByAddress(inputAddress, params);

        if (log.isDebugEnabled()) {
            log.debug("signInput #"+inputIndex+": value="+value+", addressType="+addressType+", address="+inputAddress);
        }

        switch(addressType) {
            case SEGWIT_NATIVE: case SEGWIT_COMPAT:
                SegwitAddress segwitAddress = new SegwitAddress(key.getPubKey(), params);
                final Script redeemScript = segwitAddress.segWitRedeemScript();

                TransactionSignature sig =
                        tx.calculateWitnessSignature(
                                inputIndex,
                                key,
                                redeemScript.scriptCode(),
                                value,
                                Transaction.SigHash.ALL,
                                false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, key.getPubKey());
                tx.setWitness(inputIndex, witness);

                if (addressType == AddressType.SEGWIT_COMPAT) {
                    // P2SH
                    final ScriptBuilder sigScript = new ScriptBuilder();
                    sigScript.data(redeemScript.getProgram());
                    txInput.setScriptSig(sigScript.build());
                    tx.getInput(inputIndex).getScriptSig().correctlySpends(tx, inputIndex, scriptPubKey, value, Script.ALL_VERIFY_FLAGS);
                }
                break;

            case LEGACY:
                TransactionSignature signature;
                if(key != null && (key.hasPrivKey() || key.isEncrypted())) {
                    byte[] scriptBytes = connectedOutput.getScriptBytes();
                    signature = tx.calculateSignature(inputIndex, key, scriptBytes, Transaction.SigHash.ALL, false);
                }
                else {
                    signature = TransactionSignature.dummy();   // watch only ?
                }

                if(scriptPubKey.isSentToAddress()) {
                    txInput.setScriptSig(ScriptBuilder.createInputScript(signature, key));
                }
                else if(scriptPubKey.isSentToRawPubKey()) {
                    txInput.setScriptSig(ScriptBuilder.createInputScript(signature));
                }
                else {
                    throw new RuntimeException("Unknown script type: " + scriptPubKey);
                }
                break;
        }
    }

    public TransactionOutput computeTransactionOutput(String address, long amount, NetworkParameters params) throws Exception {
        if(FormatsUtilGeneric.getInstance().isValidBech32(address))    {
            return Bech32UtilGeneric.getInstance().getTransactionOutput(address, amount, params);
        }
        else    {
            Script outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(params, address));
            return new TransactionOutput(params, null, Coin.valueOf(amount), outputScript.getProgram());
        }
    }

}
