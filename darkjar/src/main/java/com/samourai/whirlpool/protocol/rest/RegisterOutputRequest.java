package com.samourai.whirlpool.protocol.rest;

public class RegisterOutputRequest {
    public String inputsHash;
    public String unblindedSignedBordereau64;
    public String receiveAddress;
    public String bordereau64;

    public RegisterOutputRequest() {}

    public RegisterOutputRequest(
            final String inputsHash,
            final String unblindedSignedBordereau64,
            final String receiveAddress,
            final String bordereau64
    ) {
        this.inputsHash = inputsHash;
        this.unblindedSignedBordereau64 = unblindedSignedBordereau64;
        this.receiveAddress = receiveAddress;
        this.bordereau64 = bordereau64;
    }
}
