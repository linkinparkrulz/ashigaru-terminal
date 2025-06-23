package com.sparrowwallet.nightjar.stomp;

import org.eclipse.jetty.client.HttpClient;
import org.springframework.web.socket.sockjs.client.JettyXhrTransport;

public class JavaXhrTransport extends JettyXhrTransport {

    public JavaXhrTransport(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public void stop() {
        //Do nothing - the HttpClient lifecycle is managed by JavaHttpClientService
    }
}
