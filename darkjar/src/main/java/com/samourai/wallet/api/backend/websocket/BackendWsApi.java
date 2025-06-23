package com.samourai.wallet.api.backend.websocket;

import com.samourai.wallet.api.backend.websocket.beans.*;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.wallet.util.MessageListener;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.oauth.OAuthManager;
import com.samourai.websocket.client.IWebsocketClient;
import com.samourai.websocket.client.IWebsocketClientListener;
import java8.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendWsApi {
  private static final Logger log = LoggerFactory.getLogger(BackendWsApi.class);
  private static final JSONUtils jsonUtils = JSONUtils.getInstance();

  private static final String URL_INV = "/inv";

  private boolean connected;
  private IWebsocketClient wsClient;
  private String urlBackend;
  private OAuthManager oAuthManager; // or null

  private MessageListener<WSResponseBlock> blockListener;
  private MessageListener<WSResponseUtxo> addressListener;
  private boolean terminated;

  public BackendWsApi(IWebsocketClient wsClient, String urlBackend, OAuthManager oAuthManager) {
    this.connected = false;
    this.wsClient = wsClient;
    this.urlBackend = urlBackend;
    this.oAuthManager = oAuthManager;
    if (log.isDebugEnabled()) {
      String oAuthStr = oAuthManager != null ? "yes" : "no";
      log.debug("urlBackend=" + urlBackend + ", oAuth=" + oAuthStr);
    }

    this.blockListener = null;
    this.addressListener = null;
    this.terminated = false;
  }

  public synchronized void connect(MessageListener onConnect, boolean autoReconnect) throws Exception {
    if (connected) {
      return;
    }

    // connect
    String url = toWsUrl(urlBackend + URL_INV);
    if (log.isDebugEnabled()) {
      log.debug("connecting: "+url);
    }
    try {
      wsClient.connect(url, computeListener(onConnect, autoReconnect));
    } catch (Exception e) {
      log.error("connect failed", e);
      onClose(onConnect, autoReconnect);
    }
  }

  private static String toWsUrl(String httpUrl) {
    return httpUrl.replace("https://","wss://").replace("http://","ws://");
  }

  public void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("disconnect");
    }
    disconnect(true);
  }

  private synchronized void disconnect(boolean terminate) {
    if (terminate) {
      terminated = true;
    }
    if (!connected) {
      return;
    }
    connected = false;

    // stop
    Thread stopThread =
            new Thread(
                    () -> {
                      wsClient.disconnect();
                    },
                    "BackendWsApi-stop");
    stopThread.setDaemon(true);
    stopThread.start();
  }

  public void subscribeBlock(MessageListener<WSResponseBlock> blockListener) throws Exception {
    this.blockListener = blockListener;
    String accessToken = getAccessToken();
    WSSubscribeRequest request = new WSSubscribeBlockRequest(accessToken);
    String json = jsonUtils.getObjectMapper().writeValueAsString(request);
    wsClient.send(json);
  }

  public void subscribeAddress(String[] addresses, MessageListener<WSResponseUtxo> addressListener) throws Exception {
    this.addressListener = addressListener;
    if (addresses != null && addresses.length>0) {
      String accessToken = getAccessToken();
      for (String address : addresses) {
        WSSubscribeRequest request = new WSSubscribeAddressRequest(address, accessToken);
        String json = jsonUtils.getObjectMapper().writeValueAsString(request);
        wsClient.send(json);
      }
    }
  }

  private void onClose(MessageListener<Void> onConnect, boolean autoReconnect) {
    disconnect(false);

    // auto-reconnect
    if (!terminated && autoReconnect) {
      // wait & auto-reconnect in a non-blocking thread
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          // wait for reconnect-delay
          int randomDelaySeconds = RandomUtil.random(10, 300);
          if (log.isDebugEnabled()) {
            log.debug("Reconnecting in "+randomDelaySeconds+"s");
          }
          try {
            synchronized (this) {
              wait(randomDelaySeconds * 1000);
            }
          } catch (InterruptedException e) {}
          if (log.isDebugEnabled()) {
            log.debug("Reconnecting after "+randomDelaySeconds+"s");
          }
          try {
            connect(onConnect, autoReconnect);
          } catch (Exception e) {
            log.error("auto-reconnect failed", e);
          }
        }
      }, "BackendWsApi-autoreconnect");
      t.setDaemon(true);
      t.start();
    }
  }

  private IWebsocketClientListener computeListener(MessageListener<Void> onConnect, boolean autoReconnect) {
    return new IWebsocketClientListener() {
      @Override
      public void onClose(String reason) {
        if (log.isDebugEnabled()) {
          log.debug("ws: onClose: "+reason);
        }
        BackendWsApi.this.onClose(onConnect, autoReconnect);
      }

      @Override
      public void onMessage(String msg) {
        if (log.isDebugEnabled()) {
          log.debug(" <- "+msg);
        }
        BackendWsApi.this.onMessage(msg);
      }

      @Override
      public void onConnect() {
        if (log.isDebugEnabled()) {
          log.debug("ws: connected");
        }
        onConnect.onMessage(null);
      }
    };
  }

  private void onMessage(String msg) {
    if (log.isDebugEnabled()) {
      log.debug("ws: onResponse: "+msg);
    }
    try {
      // tx?
      WSResponseUtxo utxo = jsonUtils.getObjectMapper().readValue(msg, WSResponseUtxo.class);
      if (addressListener != null) {
        if (log.isDebugEnabled()) {
          log.debug("new tx detected: "+utxo.x.hash);
        }
        addressListener.onMessage(utxo);
      } else {
        log.warn("new tx detected IGNORED");
      }
    } catch(Exception e) {
      try {
        // block?
        WSResponseBlock block = jsonUtils.getObjectMapper().readValue(msg, WSResponseBlock.class);
        if (blockListener != null) {
          if (log.isDebugEnabled()) {
            log.debug("new block detected: #" + block.x.height);
          }
          blockListener.onMessage(block);
        } else {
          log.warn("new block detected IGNORED");
        }
      } catch(Exception ee) {
        log.error("Unknown response: "+msg);
      }
    }
  }

  private String getAccessToken() throws Exception {
    if (oAuthManager == null) {
      return null;
    }
    return oAuthManager.getOAuthAccessToken();
  }
}
