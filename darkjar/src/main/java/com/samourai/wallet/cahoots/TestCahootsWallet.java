package com.samourai.wallet.cahoots;

import com.google.common.base.Strings;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;

public class TestCahootsWallet extends SimpleCahootsWallet {
    public static final int POST_CHANGE_INDEX = 123;
    public static final int FEE_PER_B = 1;

    public TestCahootsWallet(HD_Wallet bip84w, NetworkParameters params) throws Exception {
        super(bip84w, params, POST_CHANGE_INDEX, FEE_PER_B);
    }

    public void addUtxo(int account, String txid, int n, long value, String address) {
        // mock utxo
        String path = "M/0/1"; // mock path
        ECKey key = ECKey.fromPrivate(BigInteger.valueOf(1234)); // mock key
        byte[] scriptBytes = mockScriptBytes();
        MyTransactionOutPoint outpoint = new MyTransactionOutPoint(getParams(), Sha256Hash.of(txid.getBytes()), n, BigInteger.valueOf(value), scriptBytes, address, 99);
        CahootsUtxo utxo = new CahootsUtxo(outpoint, path, key);
        addUtxo(account, utxo);
    }

    private byte[] mockScriptBytes() {
        String script = Strings.padEnd(Bech32UtilGeneric.SCRIPT_P2WPKH, Bech32UtilGeneric.SCRIPT_P2WPKH_LEN, '0');
        byte[] scriptBytes = Hex.decode(script.getBytes());
        return scriptBytes;
    }
}
