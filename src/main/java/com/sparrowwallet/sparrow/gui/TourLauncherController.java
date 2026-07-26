package com.sparrowwallet.sparrow.gui;

import javafx.fxml.FXML;

/**
 * The "Guided Tour" tool pane. Its Start button hands off to the main controller, which leaves the
 * Tools view (so the tour's anchors are on screen) and runs the tour.
 */
public class TourLauncherController {

    @FXML
    private void onStart() {
        AshigaruMainController controller = AshigaruGui.get().getMainController();
        if (controller != null) {
            controller.startGuidedTour();
        }
    }
}
