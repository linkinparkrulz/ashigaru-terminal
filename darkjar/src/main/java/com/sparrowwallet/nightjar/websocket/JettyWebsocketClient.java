package com.sparrowwallet.nightjar.websocket;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClientService;
import com.samourai.websocket.client.IWebsocketClient;
import com.samourai.websocket.client.IWebsocketClientListener;
import com.sparrowwallet.nightjar.http.JavaHttpClient;
import com.sparrowwallet.nightjar.http.JavaHttpClientService;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class JettyWebsocketClient implements IWebsocketClient {
    private static final Logger log = LoggerFactory.getLogger(JettyWebsocketClient.class);

    private final JavaHttpClientService httpClientService;
    private WebSocketClient webSocketClient;
    private JettyWebsocket jettyWebsocket;

    public JettyWebsocketClient(JavaHttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Override
    public void connect(String url, IWebsocketClientListener listener) throws Exception {
        JavaHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.COORDINATOR_WEBSOCKET);
        this.webSocketClient = new WebSocketClient(httpClient.getJettyHttpClient());
        this.jettyWebsocket = new JettyWebsocket();

        webSocketClient.start();
        webSocketClient.connect(jettyWebsocket, new URI(url));
    }

    @Override
    public void send(String payload) throws Exception {
        jettyWebsocket.sendMessage(payload);
    }

    @Override
    public void disconnect() {
        try {
            webSocketClient.stop();
        } catch(Exception e) {
            log.error("Error disconnecting", e);
        }
    }
}
