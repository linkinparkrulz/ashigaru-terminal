package com.samourai.wallet.util;

import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

public class XPubUtil {
  private static XPubUtil instance;

  public static XPubUtil getInstance() {
    if (instance == null) {
      instance = new XPubUtil();
    }
    return instance;
  }

  private ECKey computeKey(String xpub, int x, int chainIndex) {
    DeterministicKey mKey = FormatsUtilGeneric.getInstance().createMasterPubKeyFromXPub(xpub);
    DeterministicKey cKey =
        HDKeyDerivation.deriveChildKey(mKey, new ChildNumber(chainIndex, false));
    DeterministicKey adk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(x, false));
    ECKey ecKey = ECKey.fromPublicOnly(adk.getPubKey());
    return ecKey;
  }

  public String getAddressSegwit(String xpub, int x, int chainIndex, NetworkParameters params) {
    ECKey ecKey = computeKey(xpub, x, chainIndex);
    String addressSegwit = new SegwitAddress(ecKey, params).getAddressAsString();
    return addressSegwit;
  }

  public String getAddressBech32(String xpub, int x, int chainIndex, NetworkParameters params) {
    ECKey ecKey = computeKey(xpub, x, chainIndex);
    String addressBech32 = Bech32UtilGeneric.getInstance().toBech32(ecKey.getPubKey(), params);
    return addressBech32;
  }

  public String getPath(int x, int chainIndex) {
    return "m/84'/"+chainIndex+"'/"+x+"'";
  }
}
