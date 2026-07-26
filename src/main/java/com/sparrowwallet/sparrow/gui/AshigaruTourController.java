package com.sparrowwallet.sparrow.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Controller for the welcome tour dialog (ashigaru-tour.fxml). Shows one step
 * VBox at a time, mirroring the legacy WelcomeController visibility-toggle
 * pattern. Slide navigation is driven by {@link AshigaruTourDialog}.
 */
public class AshigaruTourController {
    @FXML private VBox tourBox;
    @FXML private VBox step1;
    @FXML private VBox step2;
    @FXML private VBox step3;
    @FXML private VBox step4;
    @FXML private VBox step5;
    @FXML private Label progressLabel;

    private VBox[] steps;
    private int index = 0;

    public void initializeView() {
        steps = new VBox[]{step1, step2, step3, step4, step5};
        for (VBox step : steps) {
            step.managedProperty().bind(step.visibleProperty());
            step.setVisible(false);
        }
        index = 0;
        showStep(0);
    }

    private void showStep(int i) {
        for (int j = 0; j < steps.length; j++) {
            steps[j].setVisible(j == i);
        }
        progressLabel.setText("Step " + (i + 1) + " of " + steps.length);
    }

    /** Advance one step; returns true if a further step still remains (i.e. not yet on the last). */
    public boolean next() {
        if (index < steps.length - 1) {
            index++;
            showStep(index);
        }
        return index < steps.length - 1;
    }

    /** Go back one step; returns true if a further step remains behind (i.e. not yet on the first). */
    public boolean back() {
        if (index > 0) {
            index--;
            showStep(index);
        }
        return index > 0;
    }
}
