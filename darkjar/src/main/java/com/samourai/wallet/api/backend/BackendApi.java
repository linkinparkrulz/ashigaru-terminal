package com.samourai.wallet.api.backend;

import com.samourai.wallet.api.backend.beans.*;
import com.samourai.wallet.util.oauth.OAuthManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BackendApi {
  private Logger log = LoggerFactory.getLogger(BackendApi.class);

  private static final String URL_UNSPENT = "/unspent?active=";
  private static final String URL_MULTIADDR = "/multiaddr?active=";
  private static final String URL_WALLET = "/wallet?active=";
  private static final String URL_TXS = "/txs?active=";
  private static final String URL_TX = "/tx/";
  private static final String URL_INIT_BIP84 = "/xpub";
  private static final String URL_MINER_FEES = "/fees";
  private static final String URL_PUSHTX = "/pushtx/";
  private static final String ZPUB_SEPARATOR = "%7C";

  private IBackendClient httpClient;
  private String urlBackend;
  private OAuthManager oAuthManager; // may be null

  public BackendApi(IBackendClient httpClient, String urlBackend) {
    this(httpClient, urlBackend, null);
  }

  public BackendApi(IBackendClient httpClient, String urlBackend, OAuthManager oAuthManager) {
    this.httpClient = httpClient;
    this.urlBackend = urlBackend;

    this.oAuthManager = oAuthManager;
    if (log.isDebugEnabled()) {
      String oAuthStr = oAuthManager != null ? "yes" : "no";
      log.debug("urlBackend=" + urlBackend + ", oAuth=" + oAuthStr);
    }
  }

  private String computeZpubStr(String[] zpubs) {
    String zpubStr = StringUtils.join(zpubs,ZPUB_SEPARATOR);
    return zpubStr;
  }

  public List<UnspentOutput> fetchUtxos(String zpub) throws Exception {
    return fetchUtxos(new String[]{zpub});
  }

  public List<UnspentOutput> fetchUtxos(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_UNSPENT + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchUtxos");
    }
    Map<String,String> headers = computeHeaders();
    UnspentResponse unspentResponse = httpClient.getJson(url, UnspentResponse.class, headers);
    List<UnspentOutput> unspentOutputs =
            new ArrayList<UnspentOutput>();
    if (unspentResponse.unspent_outputs != null) {
      unspentOutputs = Arrays.asList(unspentResponse.unspent_outputs);
    }
    return unspentOutputs;
  }

  public Map<String,MultiAddrResponse.Address> fetchAddresses(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_MULTIADDR + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchAddress");
    }
    Map<String,String> headers = computeHeaders();
    MultiAddrResponse multiAddrResponse = httpClient.getJson(url, MultiAddrResponse.class, headers);
    Map<String,MultiAddrResponse.Address> addressesByZpub = new LinkedHashMap<String, MultiAddrResponse.Address>();
    if (multiAddrResponse.addresses != null) {
      for (MultiAddrResponse.Address address : multiAddrResponse.addresses) {
        addressesByZpub.put(address.address, address);
      }
    }
    return addressesByZpub;
  }

  public MultiAddrResponse.Address fetchAddress(String zpub) throws Exception {
    Collection<MultiAddrResponse.Address> addresses = fetchAddresses(new String[]{zpub}).values();
    if (addresses.size() != 1) {
      throw new Exception("Address count=" + addresses.size());
    }
    MultiAddrResponse.Address address = addresses.iterator().next();

    if (log.isDebugEnabled()) {
      log.debug(
              "fetchAddress "
                      + zpub
                      + ": account_index="
                      + address.account_index
                      + ", change_index="
                      + address.change_index);
    }
    return address;
  }

  public TxsResponse fetchTxs(String[] zpubs, int page, int count) throws Exception {
    String zpubStr = computeZpubStr(zpubs);

    String url = computeAuthUrl(urlBackend + URL_TXS + zpubStr+"&page="+page+"&count="+count);
    if (log.isDebugEnabled()) {
      log.debug("fetchTxs");
    }
    Map<String,String> headers = computeHeaders();
    return httpClient.getJson(url, TxsResponse.class, headers);
  }

  public TxDetail fetchTx(String txid, boolean fees) throws Exception {
    String url = computeAuthUrl(urlBackend + URL_TX + txid + (fees ? "?fees=1" : ""));
    if (log.isDebugEnabled()) {
      log.debug("fetchTx: "+txid);
    }
    Map<String,String> headers = computeHeaders();
    return httpClient.getJson(url, TxDetail.class, headers);
  }

  public WalletResponse fetchWallet(String zpub) throws Exception {
    return fetchWallet(new String[]{zpub});
  }

  public WalletResponse fetchWallet(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_WALLET + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchWallet");
    }
    Map<String,String> headers = computeHeaders();
    // use async to avoid Jetty's buffer exceeded exception on large responses
    WalletResponse walletResponse = httpClient.getJson(url, WalletResponse.class, headers, true);
    return walletResponse;
  }

  public void initBip84(String zpub) throws Exception {
    String url = computeAuthUrl(urlBackend + URL_INIT_BIP84);
    if (log.isDebugEnabled()) {
      log.debug("initBip84");
    }
    Map<String,String> headers = computeHeaders();
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("xpub", zpub);
    postBody.put("type", "new");
    postBody.put("segwit", "bip84");
    httpClient.postUrlEncoded(url, Void.class, headers, postBody);
  }

  public MinerFee fetchMinerFee() throws Exception {
    String url = computeAuthUrl(urlBackend + URL_MINER_FEES);
    Map<String,String> headers = computeHeaders();
    Map<String, Integer> feeResponse = httpClient.getJson(url, Map.class, headers);
    if (feeResponse == null) {
      throw new Exception("Invalid miner fee response from server");
    }
    return new MinerFee(feeResponse);
  }

  public void pushTx(String txHex) throws Exception {
    pushTx(txHex, null);
  }

  public void pushTx(String txHex, List<Integer> strictModeVouts) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("pushTx... " + txHex);
    } else {
      log.info("pushTx...");
    }
    String url = computeAuthUrl(urlBackend + URL_PUSHTX);
    Map<String,String> headers = computeHeaders();
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("tx", txHex);

    if(strictModeVouts != null && !strictModeVouts.isEmpty()) {
      String strStrictVouts = "";
      for(int i = 0; i < strictModeVouts.size(); i++) {
        strStrictVouts += strictModeVouts.get(i);
        if(i < (strictModeVouts.size() - 1)) {
          strStrictVouts += "|";
        }
      }
      postBody.put("strict_mode_vouts", strStrictVouts);
    }

    try {
      PushTxResponse pushTxResponse = httpClient.postUrlEncoded(url, PushTxResponse.class, headers, postBody);
      checkPushTxResponse(pushTxResponse);
    } catch (HttpException e) {
      if (log.isDebugEnabled()) {
        log.error("pushTx failed", e);
      }
      log.error(
              "PushTx failed: response="
                      + e.getResponseBody()
                      + ". error="
                      + e.getMessage()
                      + " for txHex="
                      + txHex);
      throw new Exception("PushTx failed (" + e.getResponseBody() + ") for txHex=" + txHex);
    }
  }

  protected void checkPushTxResponse(PushTxResponse pushTxResponse) throws Exception {
    if (pushTxResponse.status == PushTxResponse.PushTxStatus.ok) {
      // success
      return;
    }

    if (log.isDebugEnabled()) {
      log.error("pushTx failed: "+pushTxResponse.toString());
    }

    // address reuse
    if (pushTxResponse.error != null && PushTxResponse.PushTxError.CODE_VIOLATION_STRICT_MODE_VOUTS.equals(pushTxResponse.error.code)) {
      Collection<Integer> adressReuseOutputIndexs = (Collection<Integer>)pushTxResponse.error.message;
      throw new PushTxAddressReuseException(adressReuseOutputIndexs);
    }

    // other error
    throw new Exception("PushTx failed: "+pushTxResponse.toString());
  }

  public boolean testConnectivity() {
    try {
      fetchMinerFee();
      return true;
    } catch (Exception e) {
      log.error("", e);
      return false;
    }
  }

  protected Map<String,String> computeHeaders() throws Exception {
    Map<String,String> headers = new HashMap<String, String>();
    if (oAuthManager != null) {
      // add auth token
      headers.put("Authorization", "Bearer " + oAuthManager.getOAuthAccessToken());
    }
    return headers;
  }

  protected String computeAuthUrl(String  url) throws Exception {
    // override for auth support
    return url;
  }

  protected IBackendClient getHttpClient() {
    return httpClient;
  }

  public String getUrlBackend() {
    return urlBackend;
  }

}
