package com.sparrowwallet.sparrow.gui;

import javafx.application.Platform;
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
 * {@code Scene#lookup}).
 *
 * <p>A step may carry an {@code onEnter} action that runs before its popover shows — used to
 * establish the right view first (e.g. open the Settings screen). Steps with an {@code onEnter}
 * are always kept; steps without one are skipped when their anchor is not currently visible. This
 * lets the same list work on first launch (only sidebar + status bar on screen), on replay with a
 * wallet open, and for the Settings walk-through whose anchors do not exist until entered.
 */
public class TourManager {
    private static final String HIGHLIGHT_CLASS = "tour-highlight";

    /**
     * A single coach-mark: the fx:id of the node to point at, its copy, and an optional
     * {@code onEnter} action run just before the popover is shown (to set up the view).
     */
    public record TourStep(String anchorId, String title, String body, Runnable onEnter) {
        public TourStep(String anchorId, String title, String body) {
            this(anchorId, title, body, null);
        }
    }

    private final Stage stage;
    private final List<TourStep> steps;
    private final Runnable onComplete;
    private final Runnable onExit;

    private List<TourStep> active = List.of();
    private int index = -1;
    private PopOver popOver;
    private Node highlighted;

    public TourManager(Stage stage, List<TourStep> steps) {
        this(stage, steps, null, null);
    }

    public TourManager(Stage stage, List<TourStep> steps, Runnable onComplete) {
        this(stage, steps, onComplete, null);
    }

    /**
     * @param onComplete run when the user finishes the last step (Done); not on Skip.
     * @param onExit     run on any termination (finish or Skip); use to restore the underlying view.
     */
    public TourManager(Stage stage, List<TourStep> steps, Runnable onComplete, Runnable onExit) {
        this.stage = stage;
        this.steps = steps;
        this.onComplete = onComplete;
        this.onExit = onExit;
    }

    /** Begin the walkthrough. Keeps steps that have an onEnter action or whose anchor is visible now. */
    public void start() {
        Scene scene = stage.getScene();
        if (scene == null) {
            return;
        }

        active = new ArrayList<>();
        for (TourStep step : steps) {
            if (step.onEnter() != null || resolve(step.anchorId()) != null) {
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
        index = i;

        Runnable onEnter = active.get(i).onEnter();
        if (onEnter != null) {
            onEnter.run();
        }
        // Defer so any view change from onEnter (and normal layout) settles before anchoring.
        Platform.runLater(() -> present(i));
    }

    private void present(int i) {
        if (i != index) {
            return;
        }

        Node node = resolve(active.get(i).anchorId());
        if (node == null) {
            // onEnter did not (yet) reveal the anchor — skip forward rather than stall.
            if (i < active.size() - 1) {
                showStep(i + 1);
            } else {
                finish(true);
            }
            return;
        }

        highlight(node);
        popOver = buildPopOver(active.get(i), i, node);
        popOver.show(node);
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
        if (onExit != null) {
            onExit.run();
        }
    }

    private PopOver buildPopOver(TourStep step, int i, Node node) {
        Label title = new Label(step.title());
        title.getStyleClass().add("tour-pop-title");

        Label body = new Label(step.body());
        body.setWrapText(true);
        body.setMaxWidth(300);
        // Wrapped labels report a single line's preferred height unless pinned, which clips
        // the longer step bodies at the 300px cap.
        body.setMinHeight(Region.USE_PREF_SIZE);
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

        String css = getClass().getResource("ashigaru.css").toExternalForm();
        content.getStylesheets().add(css);

        PopOver po = new PopOver(content);
        po.setDetachable(false);
        po.setHeaderAlwaysVisible(false);
        po.setCloseButtonEnabled(false);
        po.setAutoHide(false);
        po.setArrowLocation(arrowLocationFor(node));
        // The popover lives in its own scene (default light theme), so the bubble skin
        // (.popover > .border) is only themed once the app stylesheet is attached there.
        po.setOnShown(e -> {
            Scene poScene = po.getScene();
            if (poScene != null && !poScene.getStylesheets().contains(css)) {
                poScene.getStylesheets().add(css);
            }
        });
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
