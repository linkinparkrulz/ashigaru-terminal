package com.samourai.websocket.client;

public interface IWebsocketClientListener {
    void onClose(String reason);
    void onMessage(String msg);
    void onConnect();
}
