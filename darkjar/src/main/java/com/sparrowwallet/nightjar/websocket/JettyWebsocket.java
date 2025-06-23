package com.sparrowwallet.nightjar.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JettyWebsocket {
    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        this.session = null;
    }

    public void sendMessage(String msg) {
        try {
            Future<Void> fut;
            fut = session.getRemote().sendStringByFuture(msg);
            fut.get(2, TimeUnit.SECONDS);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
}
