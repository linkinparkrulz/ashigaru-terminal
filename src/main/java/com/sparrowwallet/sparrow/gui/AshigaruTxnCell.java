package com.sparrowwallet.sparrow.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Card-style list cell for a transaction on the Ashigaru wallet dashboard.
 * Amount + date stay visible at any width; the txid (with copy) and an inline,
 * editable label sit below. Label edits write through to the transaction entry.
 */
public class AshigaruTxnCell extends ListCell<AshigaruWalletController.TxnRow> {
    private final Label amountLabel = new Label();
    private final Label dateLabel = new Label();
    private final TextField labelField = new TextField();
    private final Label txidLabel = new Label();
    private final HBox txidLine = new HBox(6);
    private final HBox root = new HBox(12);

    public AshigaruTxnCell() {
        amountLabel.getStyleClass().add("card-value");
        dateLabel.getStyleClass().add("card-faint");

        labelField.getStyleClass().add("card-label-field");
        labelField.setPromptText("Add label…");
        labelField.setOnAction(e -> commitLabel());
        labelField.focusedProperty().addListener((o, was, is) -> { if (was && !is) commitLabel(); });

        txidLabel.getStyleClass().add("card-secondary");
        txidLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(txidLabel, Priority.ALWAYS);
        txidLine.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topLine = new HBox(8, amountLabel, spacer, dateLabel);
        topLine.setAlignment(Pos.CENTER_LEFT);

        VBox center = new VBox(4, topLine, labelField, txidLine);
        center.setFillWidth(true);
        HBox.setHgrow(center, Priority.ALWAYS);

        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().add(center);
        root.setMaxWidth(Double.MAX_VALUE);
    }

    private void commitLabel() {
        AshigaruWalletController.TxnRow row = getItem();
        if (row != null && row.txnEntry() != null) {
            row.txnEntry().labelProperty().set(labelField.getText() == null ? "" : labelField.getText());
        }
    }

    @Override
    protected void updateItem(AshigaruWalletController.TxnRow row, boolean empty) {
        super.updateItem(row, empty);
        if (empty || row == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        amountLabel.setText(row.amount());
        dateLabel.setText(row.date());
        if (!labelField.isFocused()) {
            labelField.setText(row.label() != null ? row.label() : "");
        }

        txidLine.getChildren().setAll(txidLabel);
        txidLabel.setText(row.txid());
        if (row.txid() != null && !row.txid().isEmpty()) {
            txidLine.getChildren().add(AshigaruWalletController.makeCopyButton(row.txid()));
        }

        setText(null);
        setGraphic(root);
    }
}
