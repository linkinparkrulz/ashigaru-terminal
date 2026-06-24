package com.sparrowwallet.sparrow.gui;

import com.sparrowwallet.drongo.bip47.PaymentCode;
import com.sparrowwallet.sparrow.control.PayNymAvatar;
import com.sparrowwallet.sparrow.paynym.PayNymService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Card-style list cell for a transaction on the Ashigaru wallet dashboard.
 * Every card has a consistent 40px left icon column so rows line up: a send/receive
 * arrow for ordinary transactions, or the sender's PayNym avatar for an incoming BIP47
 * payment. For BIP47 receives the card also shows the sender's PayNym name (when
 * registered) above an abbreviated payment code whose copy button yields the full code.
 */
public class AshigaruTxnCell extends ListCell<AshigaruWalletController.TxnRow> {
    private static final double ICON_SIZE = 40;

    // Shared across cells: payment code string -> PayNym name ("" = looked up, none registered)
    private static final Map<String, String> NYM_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> NYM_FETCHING = ConcurrentHashMap.newKeySet();

    private final PayNymAvatar avatar = new PayNymAvatar();
    private final Circle avatarPlaceholder = new Circle(ICON_SIZE / 2);
    private final Label arrowIcon = new Label();
    private final StackPane iconHolder = new StackPane();

    private final Label nymNameLabel = new Label();
    private final Label amountLabel = new Label();
    private final Label dateLabel = new Label();
    private final TextField labelField = new TextField();
    private final Label paymentCodeLabel = new Label();
    private final HBox paymentCodeLine = new HBox(6);
    private final Label txidLabel = new Label();
    private final HBox txidLine = new HBox(6);
    private final HBox topLine = new HBox(8);
    private final VBox center = new VBox(4);
    private final HBox root = new HBox(12);

    public AshigaruTxnCell() {
        avatar.setForceLoad(true);
        avatar.setPrefSize(ICON_SIZE, ICON_SIZE);
        avatarPlaceholder.getStyleClass().add("avatar-placeholder");

        arrowIcon.getStyleClass().add("tx-icon");
        arrowIcon.setMinSize(ICON_SIZE, ICON_SIZE);
        arrowIcon.setPrefSize(ICON_SIZE, ICON_SIZE);
        arrowIcon.setAlignment(Pos.CENTER);

        iconHolder.setMinWidth(ICON_SIZE);
        iconHolder.setPrefWidth(ICON_SIZE);
        iconHolder.setAlignment(Pos.TOP_CENTER);

        nymNameLabel.getStyleClass().add("paynym-name");
        amountLabel.getStyleClass().add("card-value");
        dateLabel.getStyleClass().add("card-faint");

        labelField.getStyleClass().add("card-label-field");
        labelField.setPromptText("Add label…");
        labelField.setOnAction(e -> commitLabel());
        labelField.focusedProperty().addListener((o, was, is) -> { if (was && !is) commitLabel(); });

        paymentCodeLabel.getStyleClass().add("paynym-code");
        paymentCodeLine.setAlignment(Pos.CENTER_LEFT);

        txidLabel.getStyleClass().add("card-secondary");
        txidLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(txidLabel, Priority.ALWAYS);
        txidLine.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topLine.getChildren().setAll(amountLabel, spacer, dateLabel);
        topLine.setAlignment(Pos.CENTER_LEFT);

        center.setFillWidth(true);
        HBox.setHgrow(center, Priority.ALWAYS);

        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(iconHolder, center);
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

        PaymentCode paymentCode = row.paymentCode();
        if (paymentCode != null) {
            // BIP47 receive: avatar (with placeholder), optional PayNym name, abbreviated code
            avatar.setPaymentCode(paymentCode);
            iconHolder.getChildren().setAll(avatarPlaceholder, avatar);

            String code = paymentCode.toString();
            String nymName = NYM_NAME_CACHE.get(code);
            if (nymName != null && !nymName.isEmpty()) {
                nymNameLabel.setText(nymName);
                nymNameLabel.setVisible(true);
                nymNameLabel.setManaged(true);
            } else {
                nymNameLabel.setVisible(false);
                nymNameLabel.setManaged(false);
                if (nymName == null) {
                    fetchNymName(code);
                }
            }

            paymentCodeLabel.setText(paymentCode.toAbbreviatedString());
            paymentCodeLine.getChildren().setAll(paymentCodeLabel,
                    AshigaruWalletController.makeCopyButton(code));
            center.getChildren().setAll(nymNameLabel, topLine, paymentCodeLine);
        } else {
            avatar.clearPaymentCode();
            boolean received = row.amount() != null && row.amount().startsWith("+");
            arrowIcon.setText(received ? "↓" : "↑");
            arrowIcon.getStyleClass().removeAll("received", "sent");
            arrowIcon.getStyleClass().add(received ? "received" : "sent");
            iconHolder.getChildren().setAll(arrowIcon);

            if (!labelField.isFocused()) {
                labelField.setText(row.label() != null ? row.label() : "");
            }
            txidLine.getChildren().setAll(txidLabel);
            txidLabel.setText(row.txid());
            if (row.txid() != null && !row.txid().isEmpty()) {
                txidLine.getChildren().add(AshigaruWalletController.makeCopyButton(row.txid()));
            }
            center.getChildren().setAll(topLine, labelField, txidLine);
        }

        setText(null);
        setGraphic(root);
    }

    /**
     * Looks up the PayNym name for a payment code (regardless of the global usePayNym
     * preference, like the avatar) and caches it. The PayNymService callback runs on the
     * JavaFX thread, so on completion we refresh the list to surface the name.
     */
    private void fetchNymName(String code) {
        if (!NYM_FETCHING.add(code)) {
            return; // already in flight
        }
        try {
            PayNymService.getPayNym(code, true).subscribe(payNym -> {
                NYM_NAME_CACHE.put(code, payNym.nymName() == null ? "" : payNym.nymName());
                NYM_FETCHING.remove(code);
                if (getListView() != null) {
                    getListView().refresh();
                }
            }, error -> {
                NYM_NAME_CACHE.put(code, "");
                NYM_FETCHING.remove(code);
            });
        } catch (Exception e) {
            NYM_NAME_CACHE.put(code, "");
            NYM_FETCHING.remove(code);
        }
    }
}
