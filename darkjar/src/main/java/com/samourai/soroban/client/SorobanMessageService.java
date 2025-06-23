package com.samourai.soroban.client;

public abstract class SorobanMessageService<M extends SorobanMessage, C extends SorobanContext> {
    public abstract M parse(String payload) throws Exception;

    public abstract SorobanReply reply(int account, C sorobanContext, M message) throws Exception;
}
