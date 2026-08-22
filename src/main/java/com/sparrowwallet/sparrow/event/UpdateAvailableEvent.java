package com.sparrowwallet.sparrow.event;

import com.sparrowwallet.sparrow.net.update.ReleaseFetcher;

/** Posted when a check finds a published release newer than the running version. */
public class UpdateAvailableEvent {
    private final String version;
    private final ReleaseFetcher.Release release;

    public UpdateAvailableEvent(String version, ReleaseFetcher.Release release) {
        this.version = version;
        this.release = release;
    }

    public String getVersion() {
        return version;
    }

    public ReleaseFetcher.Release getRelease() {
        return release;
    }
}
