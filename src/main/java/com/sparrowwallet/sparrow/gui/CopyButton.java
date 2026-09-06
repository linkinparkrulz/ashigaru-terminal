package com.sparrowwallet.sparrow.gui;

import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/** Small icon-only button that copies text to the clipboard and flashes a green check briefly. */
public class CopyButton extends Button {
    public static final String GLYPH_COPY = "⎘";
    public static final String GLYPH_SUCCESS = "✓";
    private static final String SUCCESS_STYLE_CLASS = "success";

    public CopyButton() {
        super(GLYPH_COPY);
        getStyleClass().add("copy-icon-btn");
        setMinSize(28, 28);
        setPrefSize(28, 28);
        setMaxSize(28, 28);
    }

    /** Copies the given text to the system clipboard and flashes the success state. */
    public final void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        flashSuccess();
    }

    /** Briefly shows the green check glyph, then reverts to the copy glyph. */
    public final void flashSuccess() {
        setText(GLYPH_SUCCESS);
        if(!getStyleClass().contains(SUCCESS_STYLE_CLASS)) {
            getStyleClass().add(SUCCESS_STYLE_CLASS);
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            setText(GLYPH_COPY);
            getStyleClass().remove(SUCCESS_STYLE_CLASS);
        });
        pause.play();
    }
}
