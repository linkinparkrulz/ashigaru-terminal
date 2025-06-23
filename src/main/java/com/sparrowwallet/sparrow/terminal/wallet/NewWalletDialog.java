package com.sparrowwallet.sparrow.terminal.wallet;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.sparrowwallet.drongo.SecureString;
import com.sparrowwallet.drongo.crypto.ECKey;
import com.sparrowwallet.drongo.crypto.EncryptionType;
import com.sparrowwallet.drongo.crypto.Key;
import com.sparrowwallet.drongo.wallet.MnemonicException;
import com.sparrowwallet.drongo.wallet.StandardAccount;
import com.sparrowwallet.drongo.wallet.Wallet;
import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.EventManager;
import com.sparrowwallet.sparrow.event.StorageEvent;
import com.sparrowwallet.sparrow.event.TimedEvent;
import com.sparrowwallet.sparrow.io.ImportException;
import com.sparrowwallet.sparrow.io.Storage;
import com.sparrowwallet.sparrow.io.StorageException;
import com.sparrowwallet.sparrow.net.ElectrumServer;
import com.sparrowwallet.sparrow.terminal.ModalDialog;
import com.sparrowwallet.sparrow.terminal.SparrowTerminal;
import com.sparrowwallet.sparrow.wallet.WalletForm;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.sparrowwallet.drongo.wallet.StandardAccount.WHIRLPOOL_ACCOUNTS;
import static com.sparrowwallet.sparrow.AppServices.showErrorDialog;

public abstract class NewWalletDialog extends WalletDialog {
    private static final Logger log = LoggerFactory.getLogger(NewWalletDialog.class);

    protected Wallet wallet;

    protected final String walletName;

    public NewWalletDialog(String title, String walletName) {
        super(title, null);
        this.walletName = walletName;
    }

    protected void createWallet() {
        close();

        try {
            discoverAndSaveWallet(getWallets());
        } catch (ImportException e) {
            log.error("Cannot import wallet", e);
        }
    }

    /**
     * Returns a list of wallets for discovery.
     * If no existing wallets are discovered, the first wallet is used.
     *
     * @return a list of wallet candidates
     */
    protected abstract List<Wallet> getWallets() throws ImportException;

    protected void onCancel() {
        close();
    }

    @Override
    public Wallet showDialog(WindowBasedTextGUI textGUI) {
        super.showDialog(textGUI);
        return wallet;
    }

    protected void discoverAndSaveWallet(List<Wallet> wallets) {
        if (wallets.isEmpty()) {
            return;
        }

        if (AppServices.onlineProperty().get()) {
            addWhirlpoolAccountsAndDiscoverAccounts(wallets);
        } else {
            addWhirlpoolAccounts(wallets.get(0));
            saveWallet(wallets.get(0));
        }
    }

    private void addWhirlpoolAccountsAndDiscoverAccounts(List<Wallet> wallets) {
        ModalDialog discoveringDialog = new ModalDialog(walletName, "Discovering accounts...");
        SparrowTerminal.get().getGui().addWindow(discoveringDialog);

        Platform.runLater(() -> {
            ElectrumServer.WalletDiscoveryService walletDiscoveryService = new ElectrumServer.WalletDiscoveryService(wallets);
            walletDiscoveryService.setOnSucceeded(successEvent -> {
                Optional<Wallet> optWallet = walletDiscoveryService.getValue();
                wallet = optWallet.orElseGet(() -> wallets.get(0));
                SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                    addWhirlpoolAccounts(wallet);
                    SparrowTerminal.get().getGui().removeWindow(discoveringDialog);
                    saveWallet(wallet);
                });
            });
            walletDiscoveryService.setOnFailed(failedEvent -> {
                wallet = wallets.get(0);
                SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                    addWhirlpoolAccounts(wallet);
                    SparrowTerminal.get().getGui().removeWindow(discoveringDialog);
                    saveWallet(wallet);
                });
                log.error("Failed to discover accounts", failedEvent.getSource().getException());
            });
            walletDiscoveryService.start();
        });
    }

    private void addWhirlpoolAccounts(final Wallet wallet) {
        setWalletForm(new WalletForm(new Storage(Storage.getWalletFile(wallet.getName())), wallet));
        for (StandardAccount account : WHIRLPOOL_ACCOUNTS) {
            addAccount(wallet, account, null, true);
        }
    }

    private void saveWallet(Wallet wallet) {
        Storage storage = new Storage(Storage.getWalletFile(wallet.getName()));

        TextInputDialogBuilder builder = new TextInputDialogBuilder().setTitle("Wallet Password");
        builder.setDescription(SettingsDialog.PasswordRequirement.UPDATE_NEW.getDescription());
        builder.setPasswordInput(true);

        String password = builder.build().showDialog(SparrowTerminal.get().getGui());
        if (password != null) {
            ModalDialog savingDialog = new ModalDialog(walletName, "Saving wallet...");
            SparrowTerminal.get().getGui().addWindow(savingDialog);

            if (password.length() == 0) {
                new Thread(new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        try {
                            storage.setEncryptionPubKey(Storage.NO_PASSWORD_KEY);
                            storage.saveWallet(wallet);
                            storage.restorePublicKeysFromSeed(wallet, null);
                            SparrowTerminal.addWallet(storage, wallet);

                            for (Wallet childWallet : wallet.getChildWallets()) {
                                storage.saveWallet(childWallet);
                                storage.restorePublicKeysFromSeed(childWallet, null);
                                SparrowTerminal.addWallet(storage, childWallet);
                            }
                        } catch (IOException | StorageException | MnemonicException e) {
                            log.error("Error saving imported wallet", e);
                            throw e;
                        }
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                            SparrowTerminal.get().getGui().removeWindow(savingDialog);
                            LoadWallet.getOpeningDialog(storage, wallet).showDialog(SparrowTerminal.get().getGui());
                        });
                    }

                    @Override
                    protected void failed() {
                        SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                            SparrowTerminal.get().getGui().removeWindow(savingDialog);
                        });
                    }
                }).start();

            } else {
                final Task<Void> passwordTask = new Task<>() {

                    @Override
                    protected Void call() throws Exception {

                        Storage.KeyDerivationService keyDerivationService = new Storage.KeyDerivationService(storage, new SecureString(password));
                        keyDerivationService.setOnSucceeded(workerStateEvent -> Platform.runLater(() -> {
                            EventManager.get().post(new StorageEvent(Storage.getWalletFile(wallet.getName()).getAbsolutePath(), TimedEvent.Action.END, "Done"));
                            ECKey encryptionFullKey = keyDerivationService.getValue();
                            new Thread(new Task<Void>() {

                                @Override
                                protected Void call() throws Exception {
                                    Key key = null;
                                    try {
                                        ECKey encryptionPubKey = ECKey.fromPublicOnly(encryptionFullKey);
                                        key = new Key(encryptionFullKey.getPrivKeyBytes(), storage.getKeyDeriver().getSalt(), EncryptionType.Deriver.ARGON2);
                                        wallet.encrypt(key);
                                        storage.setEncryptionPubKey(encryptionPubKey);
                                        storage.saveWallet(wallet);
                                        storage.restorePublicKeysFromSeed(wallet, key);
                                        SparrowTerminal.addWallet(storage, wallet);

                                        for (Wallet childWallet : wallet.getChildWallets()) {
                                            if (!childWallet.isNested()) {
                                                childWallet.encrypt(key);
                                            }
                                            storage.saveWallet(childWallet);
                                            storage.restorePublicKeysFromSeed(childWallet, key);
                                            SparrowTerminal.addWallet(storage, childWallet);
                                        }
                                    } catch (IOException | StorageException | MnemonicException e) {
                                        log.error("Error saving imported wallet", e);
                                        throw e;
                                    } finally {
                                        encryptionFullKey.clear();
                                        if (key != null) {
                                            key.clear();
                                        }
                                    }
                                    return null;
                                }

                                @Override
                                protected void succeeded() {
                                    SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                                        SparrowTerminal.get().getGui().removeWindow(savingDialog);
                                        LoadWallet.getOpeningDialog(storage, wallet).showDialog(SparrowTerminal.get().getGui());
                                    });
                                }

                                @Override
                                protected void failed() {
                                    SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                                        SparrowTerminal.get().getGui().removeWindow(savingDialog);
                                    });
                                }
                            }).start();
                        }));
                        keyDerivationService.setOnFailed(workerStateEvent -> {
                            Platform.runLater(() -> {
                                SparrowTerminal.get().getGuiThread().invokeLater(() -> SparrowTerminal.get().getGui().removeWindow(savingDialog));
                                EventManager.get().post(new StorageEvent(Storage.getWalletFile(wallet.getName()).getAbsolutePath(), TimedEvent.Action.END, "Failed"));
                                showErrorDialog("Error encrypting wallet", keyDerivationService.getException().getMessage());
                            });
                        });

                        Platform.runLater(() -> {
                            EventManager.get().post(new StorageEvent(Storage.getWalletFile(wallet.getName()).getAbsolutePath(), TimedEvent.Action.START, "Encrypting wallet..."));
                            keyDerivationService.start();
                        });

                        return null;
                    }

                };
                new Thread(passwordTask).start();
            }
        }
    }

}
