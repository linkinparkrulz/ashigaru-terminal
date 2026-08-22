package com.sparrowwallet.sparrow.gui;

import com.sparrowwallet.drongo.address.Address;
import com.sparrowwallet.drongo.bip47.PaymentCode;
import com.sparrowwallet.sparrow.net.dojo.SignedMessageVerifier;
import com.sparrowwallet.sparrow.net.update.ReleaseTrust;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.security.SignatureException;

public class Bip47MessageVerifierController {

    @FXML private TextArea paymentCodeArea;
    @FXML private TextArea signedBlockArea;
    @FXML private TextArea messageArea;
    @FXML private TextArea signatureArea;
    @FXML private VBox resultBox;
    @FXML private Label resultTitleLabel;
    @FXML private Label notificationAddressLabel;
    @FXML private Label signerAddressLabel;

    @FXML
    private void onUseReleaseCode() {
        paymentCodeArea.setText(ReleaseTrust.RELEASE_SIGNING_PAYMENT_CODE);
    }

    @FXML
    private void onParseSignedBlock() {
        try {
            SignedMessageVerifier.ParsedBlock parsed = SignedMessageVerifier.parse(signedBlockArea.getText());
            messageArea.setText(parsed.message());
            signatureArea.setText(parsed.signature());
            showInfo("Signed message block parsed", null, null);
        } catch(Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onVerify() {
        try {
            String paymentCodeText = paymentCodeArea.getText() == null ? "" : paymentCodeArea.getText().trim();
            if(paymentCodeText.isEmpty()) {
                throw new IllegalArgumentException("BIP47 payment code is required");
            }

            String message = messageArea.getText();
            String signature = signatureArea.getText() == null ? "" : signatureArea.getText().trim();
            if((message == null || message.isEmpty()) && signedBlockArea.getText() != null && !signedBlockArea.getText().isBlank()) {
                SignedMessageVerifier.ParsedBlock parsed = SignedMessageVerifier.parse(signedBlockArea.getText());
                message = parsed.message();
                signature = parsed.signature();
                messageArea.setText(message);
                signatureArea.setText(signature);
            }

            if(message == null || message.isEmpty()) {
                throw new IllegalArgumentException("Message is required");
            }
            if(signature.isEmpty()) {
                throw new IllegalArgumentException("Base64 signature is required");
            }

            PaymentCode paymentCode = new PaymentCode(paymentCodeText);
            Address notificationAddress = paymentCode.getNotificationAddress();
            Address recoveredAddress = SignedMessageVerifier.recoverAddress(message, signature);

            if(notificationAddress.equals(recoveredAddress)) {
                showSuccess(notificationAddress.toString(), recoveredAddress.toString());
            } else {
                showInvalid(notificationAddress.toString(), recoveredAddress.toString());
            }
        } catch(SignatureException e) {
            showError("Signature could not be verified: " + e.getMessage());
        } catch(Exception e) {
            showError(e.getMessage());
        }
    }

    private void showSuccess(String notificationAddress, String signerAddress) {
        resultBox.getStyleClass().removeAll("verifier-result-error", "verifier-result-info");
        if(!resultBox.getStyleClass().contains("verifier-result-success")) {
            resultBox.getStyleClass().add("verifier-result-success");
        }
        showInfo("Signature verified", notificationAddress, signerAddress);
    }

    private void showInvalid(String notificationAddress, String signerAddress) {
        resultBox.getStyleClass().removeAll("verifier-result-success", "verifier-result-info");
        if(!resultBox.getStyleClass().contains("verifier-result-error")) {
            resultBox.getStyleClass().add("verifier-result-error");
        }
        showInfo("Signature does not match this payment code", notificationAddress, signerAddress);
    }

    private void showError(String error) {
        resultBox.getStyleClass().removeAll("verifier-result-success", "verifier-result-info");
        if(!resultBox.getStyleClass().contains("verifier-result-error")) {
            resultBox.getStyleClass().add("verifier-result-error");
        }
        showInfo("Verification error: " + error, null, null);
    }

    private void showInfo(String title, String notificationAddress, String signerAddress) {
        resultTitleLabel.setText(title);
        notificationAddressLabel.setText(notificationAddress == null ? "" : "Notification address: " + notificationAddress);
        signerAddressLabel.setText(signerAddress == null ? "" : "Recovered signer address: " + signerAddress);
        resultBox.setVisible(true);
        resultBox.setManaged(true);
    }
}
