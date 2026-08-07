package com.sparrowwallet.sparrow.control;

import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.glyphfont.FontAwesome5;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.Glyph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A helper for building a BIP39 passphrase from a <em>diceware</em> word list using <strong>physical
 * dice</strong>. For each word the user rolls five real dice and types the five digits (each 1-6); the
 * dialog looks up the corresponding EFF word ({@link DicewareWordList}) and assembles a space-joined
 * passphrase. A live readout shows the exact entropy (words &times; {@code log2(7776)} bits).
 *
 * <p>There is deliberately <strong>no software randomness</strong> here — the entropy comes only from
 * the dice the user physically rolls. The dialog is an entry aid, not a generator.
 */
public class DicewareDialog extends Dialog<String> {
    private static final int INITIAL_WORDS = 8;
    private static final int MIN_WORDS = 1;

    private final VBox rowsBox = new VBox(6);
    private final List<WordRow> rows = new ArrayList<>();
    private final Label entropyLabel = new Label();
    private final Label passphraseLabel = new Label();
    private ButtonType okButtonType;

    public DicewareDialog() {
        final DialogPane dialogPane = getDialogPane();
        setTitle("Diceware Passphrase");
        dialogPane.setHeaderText("Roll five physical dice for each word, then enter the five digits (1-6):");
        dialogPane.getStylesheets().add(AppServices.class.getResource("general.css").toExternalForm());
        AppServices.addAshigaruStylesheets(dialogPane.getStylesheets());
        AppServices.setStageIcon(dialogPane.getScene().getWindow());
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        dialogPane.setPrefWidth(500);
        dialogPane.setPrefHeight(560);
        AppServices.moveToActiveWindowScreen(this);

        Glyph dice = new Glyph(FontAwesome5.FONT_NAME, FontAwesome5.Glyph.RANDOM);
        dice.setFontSize(50);
        dialogPane.setGraphic(dice);

        final VBox content = new VBox(12);

        Label instruction = new Label("Each word adds " + String.format("%.2f", DicewareWordList.BITS_PER_WORD)
                + " bits of entropy. Use at least " + INITIAL_WORDS + " words for a strong passphrase.");
        instruction.setWrapText(true);
        instruction.getStyleClass().add("diceware-instruction");

        ScrollPane scrollPane = new ScrollPane(rowsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(280);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addWordButton = new Button("Add word");
        addWordButton.setGraphic(new Glyph(FontAwesome5.FONT_NAME, FontAwesome5.Glyph.PLUS_CIRCLE));
        addWordButton.setOnAction(e -> {
            addRow();
            recompute();
        });

        entropyLabel.getStyleClass().add("diceware-entropy");
        passphraseLabel.setWrapText(true);
        passphraseLabel.getStyleClass().add("diceware-passphrase");

        VBox summary = new VBox(4, entropyLabel, passphraseLabel);
        summary.setPadding(new Insets(6, 0, 0, 0));

        content.getChildren().addAll(instruction, scrollPane, addWordButton, new Separator(), summary);
        dialogPane.setContent(content);

        for(int i = 0; i < INITIAL_WORDS; i++) {
            addRow();
        }

        okButtonType = new ButtonType("Use Passphrase", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().add(okButtonType);

        recompute();

        setResultConverter(dialogButton -> dialogButton == okButtonType ? buildPassphrase() : null);

        Platform.runLater(() -> {
            if(!rows.isEmpty()) {
                rows.get(0).digits.requestFocus();
            }
        });
    }

    private void addRow() {
        WordRow row = new WordRow(rows.size() + 1);
        rows.add(row);
        rowsBox.getChildren().add(row);
    }

    private void removeRow(WordRow row) {
        if(rows.size() <= MIN_WORDS) {
            return;
        }
        rows.remove(row);
        rowsBox.getChildren().remove(row);
        for(int i = 0; i < rows.size(); i++) {
            rows.get(i).setIndex(i + 1);
        }
        recompute();
    }

    private List<String> resolvedWords() {
        List<String> words = new ArrayList<>();
        for(WordRow row : rows) {
            row.word().ifPresent(words::add);
        }
        return words;
    }

    private String buildPassphrase() {
        return String.join(" ", resolvedWords());
    }

    private void recompute() {
        List<String> words = resolvedWords();
        int count = words.size();
        long bits = Math.round(count * DicewareWordList.BITS_PER_WORD);
        entropyLabel.setText(count + (count == 1 ? " word" : " words") + "  ≈  " + bits + " bits of entropy");
        passphraseLabel.setText(words.isEmpty() ? "Roll dice and enter digits above to build your passphrase."
                : String.join(" ", words));

        Button okButton = (Button)getDialogPane().lookupButton(okButtonType);
        if(okButton != null) {
            okButton.setDisable(count < 1);
        }
    }

    /**
     * A single word: an index label, a five-digit dice-roll field, an arrow, the resolved word, and a
     * remove button. Any change re-evaluates the passphrase; an incomplete/invalid roll is simply
     * ignored (its word is absent) until five valid digits are entered.
     */
    private class WordRow extends HBox {
        private final Label indexLabel = new Label();
        private final TextField digits = new TextField();
        private final Label wordLabel = new Label();

        WordRow(int index) {
            super(8);
            setAlignment(Pos.CENTER_LEFT);

            indexLabel.setMinWidth(28);
            indexLabel.getStyleClass().add("diceware-index");
            setIndex(index);

            digits.setPromptText("e.g. 42613");
            digits.setPrefWidth(110);
            // Accept only the digits 1-6, at most five of them.
            digits.setTextFormatter(new TextFormatter<>(change -> {
                String proposed = change.getControlNewText();
                if(proposed.length() > 5 || !proposed.matches("[1-6]*")) {
                    return null;
                }
                return change;
            }));
            digits.textProperty().addListener((obs, old, val) -> {
                wordLabel.setText(DicewareWordList.INSTANCE.wordForRoll(val).orElse(""));
                recompute();
            });
            digits.setOnAction(e -> focusNextRow());

            Label arrow = new Label("→");
            arrow.getStyleClass().add("diceware-arrow");

            wordLabel.getStyleClass().add("diceware-word");
            HBox.setHgrow(wordLabel, Priority.ALWAYS);
            wordLabel.setMaxWidth(Double.MAX_VALUE);

            Button removeButton = new Button();
            removeButton.setGraphic(new Glyph(FontAwesome5.FONT_NAME, FontAwesome5.Glyph.MINUS_CIRCLE));
            removeButton.getStyleClass().add("diceware-remove");
            removeButton.setOnAction(e -> removeRow(this));

            getChildren().addAll(indexLabel, digits, arrow, wordLabel, removeButton);
        }

        void setIndex(int index) {
            indexLabel.setText(index + ".");
        }

        Optional<String> word() {
            return DicewareWordList.INSTANCE.wordForRoll(digits.getText());
        }

        private void focusNextRow() {
            int idx = rows.indexOf(this);
            if(idx >= 0 && idx + 1 < rows.size()) {
                rows.get(idx + 1).digits.requestFocus();
            }
        }
    }
}
