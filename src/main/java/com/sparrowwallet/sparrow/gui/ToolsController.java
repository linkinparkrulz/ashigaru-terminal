package com.sparrowwallet.sparrow.gui;

import com.sparrowwallet.sparrow.AppServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class ToolsController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ToolsController.class);

    @FXML private ToggleGroup toolsMenu;
    @FXML private ToggleButton bip47VerifierButton;
    @FXML private ToggleButton statsButton;
    @FXML private ToggleButton tourButton;
    @FXML private StackPane toolsPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toolsMenu.selectedToggleProperty().addListener((observable, oldValue, selectedToggle) -> {
            if(selectedToggle == null) {
                oldValue.setSelected(true);
                return;
            }

            if(selectedToggle == bip47VerifierButton) {
                setToolPane("bip47-message-verifier.fxml");
            } else if(selectedToggle == statsButton) {
                setToolPane("stats.fxml");
            } else if(selectedToggle == tourButton) {
                setToolPane("tour-launcher.fxml");
            }
        });

        Platform.runLater(() -> toolsMenu.selectToggle(bip47VerifierButton));
    }

    private void setToolPane(String fxmlName) {
        toolsPane.getChildren().clear();

        try {
            FXMLLoader toolLoader = new FXMLLoader(AppServices.class.getResource("gui/" + fxmlName));
            Node toolNode = toolLoader.load();
            toolsPane.getChildren().add(toolNode);
        } catch(Exception e) {
            log.error("Could not load tool pane: " + fxmlName, e);
            throw new IllegalStateException("Can't find tool pane: " + fxmlName, e);
        }
    }
}
