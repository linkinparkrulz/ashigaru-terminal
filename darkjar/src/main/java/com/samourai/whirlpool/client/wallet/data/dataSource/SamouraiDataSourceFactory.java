package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.websocket.client.IWebsocketClient;

public class SamouraiDataSourceFactory extends DojoDataSourceFactory {
  public SamouraiDataSourceFactory(
          BackendServer backendServer, boolean onion, final IWebsocketClient wsClient) {
    super(backendServer.getBackendUrl(onion), null, wsClient);
  }
}
