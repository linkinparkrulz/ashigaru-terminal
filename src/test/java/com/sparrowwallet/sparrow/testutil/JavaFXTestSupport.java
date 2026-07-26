package com.sparrowwallet.sparrow.testutil;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal JavaFX toolkit bootstrap for tests that need to run code on the JavaFX Application
 * Thread (e.g. anything wrapped in Platform.runLater, or touching javafx.beans properties) but
 * don't need a full UI test framework. No Stage/Scene is ever shown.
 */
public final class JavaFXTestSupport {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFXTestSupport() {
    }

    public static void ensureStarted() {
        if (STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException e) {
                // Toolkit already started by another test class in this JVM - fine.
            }
        }
    }

    /**
     * Runs action on the JavaFX Application Thread and blocks until it completes. Also useful
     * as a no-op "flush" after calling production code that itself does Platform.runLater(...):
     * since the FX Application Thread processes its queue in submission order, waiting on a
     * runnable queued after that call guarantees the earlier one has already finished.
     */
    public static void runAndWait(Runnable action) throws InterruptedException {
        ensureStarted();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for the JavaFX Application Thread");
        }
    }
}
