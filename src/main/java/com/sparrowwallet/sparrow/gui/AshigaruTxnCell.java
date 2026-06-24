package com.sparrowwallet.sparrow.gui;

import com.sparrowwallet.drongo.bip47.PaymentCode;
import com.sparrowwallet.sparrow.control.PayNymAvatar;
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
 * editable label sit below. For an incoming BIP47 payment the card instead shows the
 * sender's PayNym avatar on the left and the full payment code as a read-only,
 * copyable identity line.
 */
public class AshigaruTxnCell extends ListCell<AshigaruWalletController.TxnRow> {
    private final PayNymAvatar avatar = new PayNymAvatar();
    private final Label amountLabel = new Label();
    private final Label dateLabel = new Label();
    private final TextField labelField = new TextField();
    private final Label paymentCodeLabel = new Label();
    private final HBox paymentCodeLine = new HBox(6);
    private final Label txidLabel = new Label();
    private final HBox txidLine = new HBox(6);
    private final VBox center = new VBox(4);
    private final HBox root = new HBox(12);

    public AshigaruTxnCell() {
        avatar.setPrefWidth(40);
        avatar.setPrefHeight(40);
        avatar.setMinWidth(40);
        avatar.setAlignment(Pos.TOP_LEFT);

        amountLabel.getStyleClass().add("card-value");
        dateLabel.getStyleClass().add("card-faint");

        labelField.getStyleClass().add("card-label-field");
        labelField.setPromptText("Add label…");
        labelField.setOnAction(e -> commitLabel());
        labelField.focusedProperty().addListener((o, was, is) -> { if (was && !is) commitLabel(); });

        paymentCodeLabel.getStyleClass().add("paynym-code");
        paymentCodeLabel.setWrapText(true);
        paymentCodeLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(paymentCodeLabel, Priority.ALWAYS);
        paymentCodeLine.setAlignment(Pos.TOP_LEFT);

        txidLabel.getStyleClass().add("card-secondary");
        txidLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(txidLabel, Priority.ALWAYS);
        txidLine.setAlignment(Pos.CENTER_LEFT);

        center.setFillWidth(true);
        HBox.setHgrow(center, Priority.ALWAYS);

        root.setAlignment(Pos.TOP_LEFT);
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topLine = new HBox(8, amountLabel, spacer, dateLabel);
        topLine.setAlignment(Pos.CENTER_LEFT);

        PaymentCode paymentCode = row.paymentCode();
        root.getChildren().clear();
        if (paymentCode != null) {
            // BIP47 receive: avatar on the left + the full payment code as identity
            avatar.setPaymentCode(paymentCode);
            paymentCodeLabel.setText("From " + paymentCode.toString());
            paymentCodeLine.getChildren().setAll(paymentCodeLabel,
                    AshigaruWalletController.makeCopyButton(paymentCode.toString()));
            center.getChildren().setAll(topLine, paymentCodeLine);
            root.getChildren().addAll(avatar, center);
        } else {
            avatar.clearPaymentCode();
            if (!labelField.isFocused()) {
                labelField.setText(row.label() != null ? row.label() : "");
            }
            txidLine.getChildren().setAll(txidLabel);
            txidLabel.setText(row.txid());
            if (row.txid() != null && !row.txid().isEmpty()) {
                txidLine.getChildren().add(AshigaruWalletController.makeCopyButton(row.txid()));
            }
            center.getChildren().setAll(topLine, labelField, txidLine);
            root.getChildren().add(center);
        }

        setText(null);
        setGraphic(root);
    }
}
