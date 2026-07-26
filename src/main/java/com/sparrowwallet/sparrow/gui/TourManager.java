package com.sparrowwallet.sparrow.gui;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the "live" portion of the guided tour: a sequence of coach-marks, each a
 * ControlsFX {@link PopOver} anchored to a real UI node (resolved by fx:id via
 * {@code Scene#lookup}). Steps whose anchor is not currently present/visible are
 * skipped, so the same step list works on first launch (only the sidebar + status
 * bar are on screen) and on replay with a wallet open (the account/balance/mix
 * controls become anchorable too).
 */
public class TourManager {
    private static final String HIGHLIGHT_CLASS = "tour-highlight";

    /** A single coach-mark: the fx:id of the node to point at, plus its copy. */
    public record TourStep(String anchorId, String title, String body) {}

    private final Stage stage;
    private final List<TourStep> steps;
    private final Runnable onComplete;

    private List<TourStep> active = List.of();
    private int index = -1;
    private PopOver popOver;
    private Node highlighted;

    public TourManager(Stage stage, List<TourStep> steps) {
        this(stage, steps, null);
    }

    /**
     * @param onComplete run when the user finishes the last step (Done). Not invoked when the
     *                   tour is skipped or when there were no visible steps to show.
     */
    public TourManager(Stage stage, List<TourStep> steps, Runnable onComplete) {
        this.stage = stage;
        this.steps = steps;
        this.onComplete = onComplete;
    }

    /** Begin the coach-mark walkthrough, skipping steps whose anchor is not visible right now. */
    public void start() {
        Scene scene = stage.getScene();
        if (scene == null) {
            return;
        }

        active = new ArrayList<>();
        for (TourStep step : steps) {
            if (resolve(step.anchorId()) != null) {
                active.add(step);
            }
        }
        if (active.isEmpty()) {
            return;
        }

        showStep(0);
    }

    /** Resolve a node by fx:id, returning null unless it is present, in-scene, and visible. */
    private Node resolve(String anchorId) {
        Scene scene = stage.getScene();
        if (scene == null) {
            return null;
        }
        Node node = scene.lookup("#" + anchorId);
        if (node == null || node.getScene() == null || !node.isVisible()) {
            return null;
        }
        Bounds bounds = node.getBoundsInLocal();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return null;
        }
        return node;
    }

    private void showStep(int i) {
        hideCurrent();

        // The anchor set is fixed at start(), but re-check defensively and skip if it vanished.
        Node node = resolve(active.get(i).anchorId());
        if (node == null) {
            if (i < active.size() - 1) {
                showStep(i + 1);
            } else {
                finish(true);
            }
            return;
        }

        index = i;
        highlight(node);

        popOver = buildPopOver(active.get(i), i, node);
        popOver.show(node);

        // The popover lives in its own scene, so the skin's bubble (.popover > .border)
        // is only themed if the app stylesheet is attached at that scene's level.
        Scene poScene = popOver.getScene();
        if (poScene != null) {
            String css = getClass().getResource("ashigaru.css").toExternalForm();
            if (!poScene.getStylesheets().contains(css)) {
                poScene.getStylesheets().add(css);
            }
        }
    }

    private void next() {
        if (index < active.size() - 1) {
            showStep(index + 1);
        } else {
            finish(true);
        }
    }

    private void back() {
        if (index > 0) {
            showStep(index - 1);
        }
    }

    private void finish(boolean completed) {
        hideCurrent();
        index = active.size();
        if (completed && onComplete != null) {
            onComplete.run();
        }
    }

    private PopOver buildPopOver(TourStep step, int i, Node node) {
        Label title = new Label(step.title());
        title.getStyleClass().add("tour-pop-title");

        Label body = new Label(step.body());
        body.setWrapText(true);
        body.setMaxWidth(300);
        body.getStyleClass().add("tour-pop-body");

        Label counter = new Label((i + 1) + " / " + active.size());
        counter.getStyleClass().add("tour-pop-counter");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("action-btn");
        backButton.setDisable(i == 0);
        backButton.setOnAction(e -> back());

        Button skipButton = new Button("Skip");
        skipButton.getStyleClass().add("action-btn");
        skipButton.setOnAction(e -> finish(false));

        boolean last = i == active.size() - 1;
        Button nextButton = new Button(last ? "Done" : "Next");
        nextButton.getStyleClass().add("primary-btn");
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> next());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, counter, spacer, backButton, skipButton, nextButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, title, body, buttons);
        content.getStyleClass().add("tour-popover");
        content.setPrefWidth(320);
        content.getStylesheets().add(getClass().getResource("ashigaru.css").toExternalForm());

        PopOver po = new PopOver(content);
        po.setDetachable(false);
        po.setHeaderAlwaysVisible(false);
        po.setCloseButtonEnabled(false);
        po.setAutoHide(false);
        po.setArrowLocation(arrowLocationFor(node));
        return po;
    }

    /**
     * Choose which side of the popover the arrow sits on, from the node's position in
     * the scene: left-column nodes get a popover to their right, bottom nodes (status
     * bar) get one above, everything else gets one below.
     */
    private PopOver.ArrowLocation arrowLocationFor(Node node) {
        Scene scene = stage.getScene();
        Bounds inScene = node.localToScene(node.getBoundsInLocal());
        double cx = (inScene.getMinX() + inScene.getMaxX()) / 2.0;
        double cy = (inScene.getMinY() + inScene.getMaxY()) / 2.0;
        double w = scene.getWidth();
        double h = scene.getHeight();

        if (cx < w * 0.33) {
            return PopOver.ArrowLocation.LEFT_CENTER;
        } else if (cy > h * 0.66) {
            return PopOver.ArrowLocation.BOTTOM_CENTER;
        } else {
            return PopOver.ArrowLocation.TOP_CENTER;
        }
    }

    private void highlight(Node node) {
        if (!node.getStyleClass().contains(HIGHLIGHT_CLASS)) {
            node.getStyleClass().add(HIGHLIGHT_CLASS);
        }
        highlighted = node;
    }

    private void clearHighlight() {
        if (highlighted != null) {
            highlighted.getStyleClass().remove(HIGHLIGHT_CLASS);
            highlighted = null;
        }
    }

    private void hideCurrent() {
        clearHighlight();
        if (popOver != null) {
            popOver.hide();
            popOver = null;
        }
    }
}
