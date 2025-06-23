package com.samourai.wallet.segwit.bech32;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

public class Bech32UtilGeneric {
    public static final String SCRIPT_P2WPKH = "0014";
    public static final String SCRIPT_P2WSH = "0020";
    public static final int SCRIPT_P2WPKH_LEN = 20 * 2 + 2 * 2;
    public static final int SCRIPT_P2WSH_LEN = 32 * 2 + 2 * 2;

    private static Bech32UtilGeneric instance = null;

    protected Bech32UtilGeneric() { ; }

    public static Bech32UtilGeneric getInstance() {

        if(instance == null) {
            instance = new Bech32UtilGeneric();
        }

        return instance;
    }

    public boolean isBech32Script(String script) {
        return isP2WPKHScript(script) || isP2WSHScript(script);
    }

    public boolean isP2WPKHScript(String script) {
        return script.startsWith(SCRIPT_P2WPKH) && script.length() == SCRIPT_P2WPKH_LEN;
    }

    public boolean isP2WSHScript(String script) {
        return script.startsWith(SCRIPT_P2WSH) && script.length() == SCRIPT_P2WSH_LEN;
    }

    public String getAddressFromScript(String script, NetworkParameters params) throws Exception    {
        String hrp = getHrp(params);
        return Bech32Segwit.encode(hrp, (byte)0x00, Hex.decode(script.substring(4).getBytes()));
    }

    public String getAddressFromScript(Script script, NetworkParameters params) throws Exception    {
        String hrp = getHrp(params);
        byte[] buf = script.getProgram();
        byte[] scriptBytes = new byte[buf.length - 2];
        System.arraycopy(buf, 2, scriptBytes, 0, scriptBytes.length);
        return Bech32Segwit.encode(hrp, (byte)0x00, scriptBytes);
    }

    protected String getHrp(NetworkParameters params) {
        return params instanceof TestNet3Params ? "tb" : "bc";
    }

    public String getAddressFromScript(TransactionOutput output) throws Exception    {
        String script = new String(Hex.encode(output.getScriptBytes()));
        String outputAddressBech32 = getAddressFromScript(script, output.getParams());
        return outputAddressBech32;
    }

    public TransactionOutput getTransactionOutput(String address, long value, NetworkParameters params) throws Exception {
        byte[] scriptPubKey = computeScriptPubKey(address, params);
        return new TransactionOutput(params, null, Coin.valueOf(value), scriptPubKey);
    }

    public byte[] computeScriptPubKey(String address, NetworkParameters params) throws Exception {
        // decode bech32
        String hrp = getHrp(params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(hrp, address);

        // get scriptPubkey
        return Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
    }

    public String toBech32(HD_Address hdAddress, NetworkParameters params) {
        return toBech32(hdAddress.getPubKey(), params);
    }

    public String toBech32(byte[] pubkey, NetworkParameters params) {
        return new SegwitAddress(pubkey, params).getBech32AsString();
    }

}
