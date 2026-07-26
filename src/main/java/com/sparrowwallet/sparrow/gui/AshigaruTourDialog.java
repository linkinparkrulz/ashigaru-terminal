package com.sparrowwallet.sparrow.gui;

import com.sparrowwallet.sparrow.AppServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;

import java.io.IOException;

/**
 * The welcome tour dialog: a multi-slide introduction shown on first launch (and
 * replayable from the sidebar). The result is {@code true} when the user chooses
 * "Take the tour" (proceed to the live coach-marks), {@code false} otherwise.
 */
public class AshigaruTourDialog extends Dialog<Boolean> {
    public AshigaruTourDialog() {
        setTitle("Welcome to Ashigaru");
        initModality(Modality.APPLICATION_MODAL);
        if (AshigaruGui.get() != null) {
            initOwner(AshigaruGui.get().getMainStage());
        }

        try {
            FXMLLoader loader = new FXMLLoader(AshigaruTourDialog.class.getResource("ashigaru-tour.fxml"));
            final DialogPane dialogPane = loader.load();
            setDialogPane(dialogPane);
            AshigaruTourController controller = loader.getController();
            controller.initializeView();

            final ButtonType backButtonType = new ButtonType("Back", ButtonBar.ButtonData.LEFT);
            final ButtonType nextButtonType = new ButtonType("Next", ButtonBar.ButtonData.NEXT_FORWARD);
            final ButtonType tourButtonType = new ButtonType("Take the tour", ButtonBar.ButtonData.OK_DONE);
            final ButtonType skipButtonType = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialogPane.getButtonTypes().addAll(backButtonType, nextButtonType, tourButtonType, skipButtonType);

            Button backButton = (Button) dialogPane.lookupButton(backButtonType);
            Button nextButton = (Button) dialogPane.lookupButton(nextButtonType);
            Button tourButton = (Button) dialogPane.lookupButton(tourButtonType);
            Button skipButton = (Button) dialogPane.lookupButton(skipButtonType);

            backButton.managedProperty().bind(backButton.visibleProperty());
            nextButton.managedProperty().bind(nextButton.visibleProperty());
            tourButton.managedProperty().bind(tourButton.visibleProperty());
            skipButton.managedProperty().bind(skipButton.visibleProperty());

            // "Take the tour" replaces "Next" on the final slide.
            backButton.setDisable(true);
            tourButton.visibleProperty().bind(nextButton.visibleProperty().not());

            nextButton.addEventFilter(ActionEvent.ACTION, event -> {
                boolean moreAfter = controller.next();
                if (!moreAfter) {
                    nextButton.setVisible(false);
                    tourButton.setDefaultButton(true);
                }
                backButton.setDisable(false);
                event.consume();
            });

            backButton.addEventFilter(ActionEvent.ACTION, event -> {
                nextButton.setVisible(true);
                boolean morePrev = controller.back();
                if (!morePrev) {
                    backButton.setDisable(true);
                }
                event.consume();
            });

            setOnShown(e -> {
                AppServices.setStageIcon(dialogPane.getScene().getWindow());
                AppServices.onEscapePressed(dialogPane.getScene(), this::close);
            });

            setResultConverter(dialogButton -> dialogButton == tourButtonType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
