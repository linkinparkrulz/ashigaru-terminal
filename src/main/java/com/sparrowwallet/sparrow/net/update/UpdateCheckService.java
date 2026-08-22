package com.sparrowwallet.sparrow.net.update;

import com.sparrowwallet.sparrow.AshigaruTerminal;
import com.sparrowwallet.sparrow.event.UpdateAvailableEvent;
import com.sparrowwallet.sparrow.net.Version;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Asks GitHub, through the configured proxy, whether a release newer than this build exists.
 *
 * <p>This only decides whether to offer an update. It establishes no trust: the answer names a tag,
 * and nothing is acted on until {@link ReleaseVerifier} has checked the release against the pinned
 * signing key. A hostile answer can at worst point at a release that then fails verification, or
 * withhold the news of an update - which is why the check is a convenience and the release page
 * remains the fallback.
 */
public class UpdateCheckService extends ScheduledService<UpdateAvailableEvent> {
    private static final Logger log = LoggerFactory.getLogger(UpdateCheckService.class);

    private static ReleaseFetcher.Release latestRelease;

    @Override
    protected Task<UpdateAvailableEvent> createTask() {
        return new Task<>() {
            @Override
            protected UpdateAvailableEvent call() {
                try {
                    ReleaseFetcher.Release release = ReleaseFetcher.fetchLatestRelease();
                    latestRelease = release;
                    String version = ReleaseVerifier.stripLeadingV(release.tag());
                    if(isNewer(version)) {
                        return new UpdateAvailableEvent(version, release);
                    }
                } catch(IOException e) {
                    //Expected whenever the network or Tor is unavailable; not worth alarming the user
                    log.info("Could not check for updates: " + e.getMessage());
                } catch(Exception e) {
                    log.warn("Unexpected error checking for updates", e);
                }

                return null;
            }
        };
    }

    static boolean isNewer(String candidate) {
        try {
            return new Version(candidate).compareTo(new Version(AshigaruTerminal.APP_VERSION)) > 0;
        } catch(IllegalArgumentException e) {
            log.warn("Could not compare release version " + candidate + " to " + AshigaruTerminal.APP_VERSION);
            return false;
        }
    }

    /** The most recent release seen by any check, for the Settings pane to show without refetching. */
    public static ReleaseFetcher.Release getLatestRelease() {
        return latestRelease;
    }

    /** The latest published version seen by any check, or null if none has succeeded yet. */
    public static String getVersion() {
        return latestRelease == null ? null : ReleaseVerifier.stripLeadingV(latestRelease.tag());
    }
}
