package com.samourai.wallet.hd;

import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import org.json.JSONException;
import org.json.JSONObject;

public class HD_Address {

    private int accountIndex;
    private int chainIndex;
    private int mChildNum; // addressIndex
    private String strPath = null;
    private ECKey ecKey = null;
    private byte[] mPubKey = null;

    private NetworkParameters mParams = null;

    private HD_Address() { ; }

    public HD_Address(NetworkParameters params, DeterministicKey cKey, int accountIndex, int chainIndex, int child) {

        mParams = params;
        this.accountIndex = accountIndex;
        this.chainIndex = chainIndex;
        mChildNum = child;

        DeterministicKey dk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(mChildNum, false));
        if(dk.hasPrivKey())    {
            ecKey = new ECKey(dk.getPrivKeyBytes(), dk.getPubKey());
        }
        else    {
            ecKey = ECKey.fromPublicOnly(dk.getPubKey());
        }
        long now = Utils.now().getTime() / 1000;
        ecKey.setCreationTimeSeconds(now);

        mPubKey = ecKey.getPubKey();

        strPath = dk.getPath().toString();
    }

    public String getAddressString() {
        return ecKey.toAddress(mParams).toString();
    }

    public String getAddressString(AddressType addressType) {
        switch (addressType) {
            case LEGACY:
                return getAddressString();
            case SEGWIT_COMPAT:
                return new SegwitAddress(getPubKey(), mParams).getAddressAsString();
            case SEGWIT_NATIVE:
                return Bech32UtilGeneric.getInstance().toBech32(getPubKey(), mParams);
        }
        return null;
    }

    public String getPrivateKeyString() {

        if(ecKey.hasPrivKey()) {
            return ecKey.getPrivateKeyEncoded(mParams).toString();
        }
        else    {
            return null;
        }

    }

    public int getAccountIndex() {
        return accountIndex;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public int getAddressIndex() {
        return mChildNum;
    }

    public byte[] getPubKey() {
        return mPubKey;
    }

    public Address getAddress() {
        return ecKey.toAddress(mParams);
    }

    public ECKey getECKey() {
        return ecKey;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("path", strPath);
            obj.put("address", getAddressString());

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getPathFull(AddressType addressType) {
        return getPathFull(addressType.getPurpose(), 0, accountIndex, chainIndex, mChildNum);
    }

    public static String getPathFull(int purpose, int coinType, int account, int chain, int address) {
        return "m/"+purpose+"'/"+coinType+"'/"+account+"'/"+chain+"/"+address;
    }
}
