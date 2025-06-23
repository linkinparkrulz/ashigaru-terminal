package com.samourai.soroban.client;

public interface SorobanMessage extends SorobanReply {
    String toPayload();

    boolean isDone();
}
