package com.sparrowwallet.nightjar.http;

import com.samourai.wallet.api.backend.beans.HttpException;

public class JavaHttpException extends HttpException {
    private final int statusCode;

    public JavaHttpException(Exception cause, String responseBody, int statusCode) {
        super(cause, responseBody);
        this.statusCode = statusCode;
    }

    public JavaHttpException(String message, String responseBody, int statusCode) {
        super(message, responseBody);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
