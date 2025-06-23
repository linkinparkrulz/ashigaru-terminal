package com.samourai.wallet.hd;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.json.JSONException;
import org.json.JSONObject;

public class HD_Chain {

    private DeterministicKey cKey = null;
    private int accountIndex;
    private boolean isReceive;
    private int chainIndex;
    private String strPath = null;

    private int addrIdx = 0;

    private NetworkParameters mParams = null;
    
    private HD_Chain() { ; }

    public HD_Chain(NetworkParameters params, DeterministicKey aKey, int accountIndex, boolean isReceive) {

        mParams = params;
        this.accountIndex = accountIndex;
        this.isReceive = isReceive;
        this.chainIndex = isReceive ? 0 : 1;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chainIndex);

        strPath = cKey.getPath().toString();
    }

    public int getAccountIndex() {
        return accountIndex;
    }

    public boolean isReceive() {
        return isReceive;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public HD_Address getAddressAt(int addrIdx) {
    	return new HD_Address(mParams, cKey, accountIndex, chainIndex, addrIdx);
    }

    public int getAddrIdx() {
        return addrIdx;
    }

    public void setAddrIdx(int idx) {
        addrIdx = idx;
    }

    public void incAddrIdx() {
        addrIdx++;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("path", strPath);
            obj.put("idx", addrIdx);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
