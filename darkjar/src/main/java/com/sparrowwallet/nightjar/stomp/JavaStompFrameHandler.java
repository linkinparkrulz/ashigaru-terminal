package com.sparrowwallet.nightjar.stomp;

import com.samourai.stomp.client.IStompMessage;
import com.samourai.wallet.util.MessageErrorListener;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;

public class JavaStompFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final MessageErrorListener<IStompMessage, String> onMessageOnErrorListener;

    public JavaStompFrameHandler(MessageErrorListener<IStompMessage, String> onMessageOnErrorListener) {
        this.onMessageOnErrorListener = onMessageOnErrorListener;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        String messageType = headers.get(WhirlpoolProtocol.HEADER_MESSAGE_TYPE).get(0);
        try {
            return Class.forName(messageType);
        } catch(ClassNotFoundException e) {
            log.error("unknown message type: " + messageType, e);
            this.onMessageOnErrorListener.onError("unknown message type: " + messageType);
            return null;
        }
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        IStompMessage stompMessage = new JavaStompMessage(headers, payload);

        // payload already deserialized by StompFrameHandler
        onMessageOnErrorListener.onMessage(stompMessage);
    }
}
