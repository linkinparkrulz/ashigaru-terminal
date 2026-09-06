package com.sparrowwallet.sparrow.control;

import com.sparrowwallet.sparrow.glyphfont.FontAwesome5;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.controlsfx.glyphfont.Glyph;

public class HelpLabel extends Label {
    private final Tooltip tooltip;

    public HelpLabel() {
        super("", getHelpGlyph());
        tooltip = new Tooltip();
        tooltip.textProperty().bind(helpTextProperty());
        tooltip.graphicProperty().bind(helpGraphicProperty());
        tooltip.setShowDuration(Duration.seconds(15));
        tooltip.setShowDelay(Duration.millis(500));
        //Help text is prose, and a Tooltip does not wrap by default - the longest of these runs to
        //310 characters, which lays out as one line roughly 2100px wide and leaves the screen.
        //
        //This cannot move to a .tooltip stylesheet rule. Tooltip exposes -fx-wrap-text but neither
        //it nor PopupControl exposes any width property to CSS, so a rule could turn wrapping on
        //while giving it no width to wrap against, which changes nothing. Both have to be set here.
        //TooltipSkin binds its inner Label's wrapText and maxWidth to the Tooltip's own, so setting
        //them on the Tooltip reaches the node that renders the text.
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400);
        getStyleClass().add("help-label");

        Platform.runLater(() -> setTooltip(tooltip));
    }

    private static Glyph getHelpGlyph() {
        Glyph glyph = new Glyph("Font Awesome 5 Free Solid", FontAwesome5.Glyph.QUESTION_CIRCLE);
        glyph.getStyleClass().add("help-icon");
        glyph.setFontSize(11);
        return glyph;
    }

    public final StringProperty helpTextProperty() {
        if(helpText == null) {
            helpText = new SimpleStringProperty(this, "helpText", "");
        }

        return helpText;
    }

    private StringProperty helpText;

    public final void setHelpText(String value) {
        helpTextProperty().setValue(value.replace("\\n", "\n"));
    }

    public final String getHelpText() {
        return helpText == null ? "" : helpText.getValue();
    }

    public ObjectProperty<Node> helpGraphicProperty() {
        if(helpGraphicProperty == null) {
            helpGraphicProperty = new SimpleObjectProperty<Node>(this, "helpGraphic", null);
        }

        return helpGraphicProperty;
    }

    private ObjectProperty<Node> helpGraphicProperty;

    public final void setHelpGraphic(Node graphic) {
        helpGraphicProperty().setValue(graphic);
    }
}
