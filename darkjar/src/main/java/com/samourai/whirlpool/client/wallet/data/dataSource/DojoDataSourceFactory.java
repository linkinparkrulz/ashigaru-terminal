package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendOAuthApi;
import com.samourai.wallet.api.backend.websocket.BackendWsApi;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.oauth.OAuthApi;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.wallet.util.oauth.OAuthManagerJava;
import com.samourai.websocket.client.IWebsocketClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;

public class DojoDataSourceFactory implements DataSourceFactory {
  private String dojoUrl;
  private String dojoApiKey; // may be null
  private IWebsocketClient wsClient; // may be null

  public DojoDataSourceFactory(String dojoUrl, String dojoApiKey, final IWebsocketClient wsClient) {
    this.dojoUrl = dojoUrl;
    this.dojoApiKey = dojoApiKey;
    this.wsClient = wsClient;
  }

  // overridable
  protected String computeDojoApiKey(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
      throws Exception {
    return this.dojoApiKey;
  }

  @Override
  public DataSource createDataSource(
          WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister)
      throws Exception {
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    IHttpClient httpClientBackend = config.getHttpClient(HttpUsage.BACKEND);

    // configure OAuth
    OAuthManager oAuthManager = null;
    String myDojoApiKey = computeDojoApiKey(whirlpoolWallet, bip44w);
    if (myDojoApiKey != null) {
      OAuthApi backendOAuthApi = new BackendOAuthApi(httpClientBackend, dojoUrl);
      oAuthManager = new OAuthManagerJava(myDojoApiKey, backendOAuthApi);
    }

    // configure Samourai/Dojo backend
    BackendApi backendApi = new BackendApi(httpClientBackend, dojoUrl, oAuthManager);
    BackendWsApi backendWsApi =
        wsClient != null ? new BackendWsApi(wsClient, dojoUrl, oAuthManager) : null;
    checkConnectivity(backendApi, backendWsApi);

    return new SamouraiDataSource(whirlpoolWallet, bip44w, dataPersister, backendApi, backendWsApi);
  }

  protected void checkConnectivity(BackendApi backendApi, BackendWsApi backendWsApi)
      throws Exception {
    if (!backendApi.testConnectivity()) {
      throw new NotifiableException(
          "Unable to connect to wallet backend: " + backendApi.getUrlBackend());
    }
  }
}
