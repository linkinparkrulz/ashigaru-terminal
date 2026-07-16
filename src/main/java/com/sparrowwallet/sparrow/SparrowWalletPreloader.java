package com.sparrowwallet.sparrow;

import javafx.application.Preloader;
import javafx.stage.Stage;

public class SparrowWalletPreloader extends Preloader {
    @Override
    public void start(Stage stage) {
        // Sets the JavaFX/glass application name, which on Linux becomes the window
        // WM_CLASS. This MUST match StartupWMClass in the packaged .desktop file
        // (src/main/deploy/package/linux/Ashigaru.desktop) so the desktop environment
        // associates our windows with the Ashigaru launcher. Leaving it as "Sparrow"
        // made the DE group Ashigaru under Sparrow Wallet in the taskbar/dock (issue #20).
        com.sun.glass.ui.Application.GetApplication().setName("Ashigaru");
    }
}
