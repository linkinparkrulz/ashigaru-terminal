package com.sparrowwallet.nightjar.stomp;

import com.samourai.stomp.client.IStompMessage;
import org.springframework.messaging.simp.stomp.StompHeaders;

public class JavaStompMessage implements IStompMessage {
    private final StompHeaders headers;
    private final Object payload;

    public JavaStompMessage(StompHeaders headers, Object payload) {
        this.headers = headers;
        this.payload = payload;
    }

    @Override
    public String getStompHeader(String headerName) {
        return headers.getFirst(headerName);
    }

    @Override
    public Object getPayload() {
        return payload;
    }
}
