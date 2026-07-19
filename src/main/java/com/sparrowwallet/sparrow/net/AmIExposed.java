package com.sparrowwallet.sparrow.net;

import com.sparrowwallet.sparrow.io.Server;

public enum AmIExposed {
    AM_I_EXPOSED("http://am-i.exposed/#tx={0}"),
    NONE("http://none");

    private final Server server;

    AmIExposed(String url) {
        this.server = new Server(url);
    }

    public Server getServer() {
        return server;
    }
}
