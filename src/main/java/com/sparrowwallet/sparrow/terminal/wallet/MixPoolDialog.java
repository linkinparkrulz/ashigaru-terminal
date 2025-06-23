package com.sparrowwallet.sparrow.terminal.wallet;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.tx0.Tx0Previews;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.sparrowwallet.drongo.BitcoinUnit;
import com.sparrowwallet.drongo.wallet.MixConfig;
import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.EventManager;
import com.sparrowwallet.sparrow.UnitFormat;
import com.sparrowwallet.sparrow.event.WalletMasterMixConfigChangedEvent;
import com.sparrowwallet.sparrow.io.Config;
import com.sparrowwallet.sparrow.terminal.SparrowTerminal;
import com.sparrowwallet.sparrow.wallet.Entry;
import com.sparrowwallet.sparrow.wallet.UtxoEntry;
import com.sparrowwallet.sparrow.wallet.WalletForm;
import com.sparrowwallet.sparrow.whirlpool.Whirlpool;
import com.sparrowwallet.sparrow.whirlpool.dataSource.SparrowMinerFeeSupplier;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ButtonType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public class MixPoolDialog extends WalletDialog {
    private static final DisplayPool NULL_POOL = new DisplayPool(null);

    private final String walletId;
    private final List<UtxoEntry> utxoEntries;
    private final Tx0FeeTarget tx0FeeTarget;

    private final ComboBox<DisplayPool> pool;

    private final Label poolFeeLabel;
    private final Label poolFee;
    private final Label premixOutputs;
    private final Label amountToWhirlpool;
    private final Label unmixedChange;
    private final Label txMinerFee;
    private final Label totalFees;
    private final Button broadcast;

    private Tx0Previews tx0Previews;
    private final ObjectProperty<Tx0Preview> tx0PreviewProperty = new SimpleObjectProperty<>(null);
    private Pool mixPool;
    UnitFormat format = Config.get().getUnitFormat() == null ? UnitFormat.DOT : Config.get().getUnitFormat();


    public MixPoolDialog(String walletId, WalletForm walletForm, List<UtxoEntry> utxoEntries, Tx0FeeTarget tx0FeeTarget) {
        super(walletForm.getWallet().getFullDisplayName(), walletForm);

        this.walletId = walletId;
        this.utxoEntries = utxoEntries;
        this.tx0FeeTarget = tx0FeeTarget;

        setHints(List.of(Hint.CENTERED));

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(2).setVerticalSpacing(1));

        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        //mainPanel.addComponent(new Label("Premix fee rate"));
        Label premixFeeRate = new Label("");
        //mainPanel.addComponent(premixFeeRate);
        MixDialog.FeePriority feePriority = new MixDialog.FeePriority("High", Tx0FeeTarget.BLOCKS_2);
        Tx0FeeTarget tx0FeePriority = feePriority.getTx0FeeTarget();
        MinerFeeTarget feeTarget = tx0FeePriority.getFeeTarget();
        final int feeRate = SparrowMinerFeeSupplier.getFee(Integer.parseInt(feeTarget.getValue()));
        premixFeeRate.setText(Math.max(2, feeRate) + " sats/vB");


        mainPanel.addComponent(new Label("WHIRLPOOL TRANSACTION ZERO"));
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        mainPanel.addComponent(new Label("Pool"));
        pool = new ComboBox<>();
        pool.addItem(NULL_POOL);
        pool.setEnabled(false);
        mainPanel.addComponent(pool);

        poolFeeLabel = new Label("Anti-Sybil fee");
        poolFeeLabel.setPreferredSize(new TerminalSize(21, 1));
        mainPanel.addComponent(poolFeeLabel);
        poolFee = new Label("");
        mainPanel.addComponent(poolFee);

        mainPanel.addComponent(new Label("──────────────────────────────"));
        mainPanel.addComponent(new Label("──────────────────────────────"));

        mainPanel.addComponent(new Label("UTXOs created"));
        premixOutputs = new Label("");
        mainPanel.addComponent(premixOutputs);

        mainPanel.addComponent(new Label("Amount to Whirlpool"));
        amountToWhirlpool = new Label("");
        mainPanel.addComponent(amountToWhirlpool);

        mainPanel.addComponent(new Label("Unmixed change"));
        unmixedChange = new Label("");
        mainPanel.addComponent(unmixedChange);

        mainPanel.addComponent(new Label("Transaction miner fees"));
        txMinerFee = new Label("");
        mainPanel.addComponent(txMinerFee);

        mainPanel.addComponent(new Label("──────────────────────────────"));
        mainPanel.addComponent(new Label("──────────────────────────────"));

        mainPanel.addComponent(new Label("Total fees"));
        totalFees = new Label("");
        mainPanel.addComponent(totalFees);

        Panel buttonPanel = new Panel();
        buttonPanel.setLayoutManager(new GridLayout(2).setHorizontalSpacing(1));
        buttonPanel.addComponent(new Button("Cancel", this::onCancel));
        broadcast = new Button("Broadcast", this::onBroadcast).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false));
        buttonPanel.addComponent(broadcast);
        broadcast.setEnabled(false);
        broadcast.setVisible(false);

        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        buttonPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER,false,false)).addTo(mainPanel);
        setComponent(mainPanel);

        pool.addListener((selectedIndex, previousSelection, changedByUserInteraction) -> {
            DisplayPool selectedPool = pool.getSelectedItem();
            if(selectedPool != NULL_POOL) {
                UnitFormat format = Config.get().getUnitFormat() == null ? UnitFormat.DOT : Config.get().getUnitFormat();
                poolFee.setText(format.formatBtcValue(selectedPool.pool.getFeeValue()) + " BTC");
                fetchTx0Preview(selectedPool.pool);
            }
        });

        tx0PreviewProperty.addListener((observable, oldValue, tx0Preview) -> {
            SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                if(tx0Preview == null) {
                    premixOutputs.setText("Calculating...");
                    amountToWhirlpool.setText("Calculating...");
                    unmixedChange.setText("Calculating...");
                    txMinerFee.setText("Calculating...");
                    totalFees.setText("Calculating...");
                    broadcast.setEnabled(false);
                    broadcast.setVisible(false);
                } else {
                    if(tx0Preview.getPool().getFeeValue() != tx0Preview.getTx0Data().getFeeValue()) {
                        poolFeeLabel.setText("Anti-Sybil fee (discounted)");
                    } else {
                        poolFeeLabel.setText("Anti-Sybil fee");
                    }

                    Long minerFee = (tx0Preview.getNbPremix() * tx0Preview.getPremixMinerFee())+ tx0Preview.getTx0MinerFee();;
                    Long poolFeeLong = tx0Preview.getTx0Data().getFeeValue();

                    UnitFormat format = Config.get().getUnitFormat() == null ? UnitFormat.DOT : Config.get().getUnitFormat();
                    poolFee.setText(format.formatBtcValue(poolFeeLong) + " BTC");
                    premixOutputs.setText(String.valueOf(tx0Preview.getNbPremix()));
                    amountToWhirlpool.setText(format.formatBtcValue(tx0Preview.getNbPremix() * pool.getSelectedItem().pool.getDenomination()) + " BTC");
                    unmixedChange.setText(format.tableFormatBtcValue(tx0Preview.getChangeValue()) + " BTC");
                    txMinerFee.setText(format.tableFormatBtcValue(minerFee) + " BTC");
                    totalFees.setText(format.tableFormatBtcValue(minerFee+poolFeeLong) + " BTC");
                    broadcast.setEnabled(true);
                    broadcast.setVisible(true);

                }
            });
        });

        Platform.runLater(() -> fetchPools(false));
    }

    private void onBroadcast() {
        mixPool = tx0PreviewProperty.get() == null ? null : tx0PreviewProperty.get().getPool();
        close();
    }

    private void onCancel() {
        close();
    }

    @Override
    public Pool showDialog(WindowBasedTextGUI textGUI) {
        super.showDialog(textGUI);
        return mixPool;
    }

    private void fetchPools(boolean refresh) {
        long totalUtxoValue = utxoEntries.stream().mapToLong(Entry::getValue).sum();
        Whirlpool.PoolsService poolsService = new Whirlpool.PoolsService(AppServices.getWhirlpoolServices().getWhirlpool(walletId), totalUtxoValue, refresh);
        poolsService.setOnSucceeded(workerStateEvent -> {
            List<Pool> availablePools = poolsService.getValue().stream().toList();
            if(availablePools.isEmpty()) {
                SparrowTerminal.get().getGuiThread().invokeLater(() -> pool.setEnabled(false));

                Whirlpool.PoolsService allPoolsService = new Whirlpool.PoolsService(AppServices.getWhirlpoolServices().getWhirlpool(walletId), null, refresh);
                allPoolsService.setOnSucceeded(poolsStateEvent -> {
                    OptionalLong optMinValue = allPoolsService.getValue().stream().mapToLong(pool1 -> pool1.getPremixValueMin() + pool1.getFeeValue()).min();
                    if(optMinValue.isPresent() && totalUtxoValue < optMinValue.getAsLong()) {
                        UnitFormat format = Config.get().getUnitFormat() == null ? UnitFormat.DOT : Config.get().getUnitFormat();
                        String satsValue = format.formatSatsValue(optMinValue.getAsLong()) + " sats";
                        String btcValue = format.formatBtcValue(optMinValue.getAsLong()) + " BTC";
                        AppServices.showErrorDialog("Insufficient UTXO Value", "No available pools. Select a value over " + (Config.get().getBitcoinUnit() == BitcoinUnit.BTC ? btcValue : satsValue) + ".");
                        SparrowTerminal.get().getGuiThread().invokeLater(this::close);
                    }
                });
                allPoolsService.start();
            } else {
                SparrowTerminal.get().getGuiThread().invokeLater(() -> {
                    pool.setEnabled(true);
                    pool.clearItems();
                    availablePools.stream().map(DisplayPool::new).forEach(pool::addItem);
                    pool.setSelectedIndex(0);
                });
            }
        });
        poolsService.setOnFailed(workerStateEvent -> {
            Throwable exception = workerStateEvent.getSource().getException();
            while(exception.getCause() != null) {
                exception = exception.getCause();
            }

            Optional<ButtonType> optResponse = AppServices.showWarningDialog("Error fetching pools", "Try again?", ButtonType.CANCEL, ButtonType.OK);

            if(optResponse != null && optResponse.isPresent() && optResponse.get().equals(ButtonType.OK))
                Platform.runLater(() -> fetchPools(true));
            else
                SparrowTerminal.get().getGuiThread().invokeLater(() -> pool.setEnabled(false));
        });
        poolsService.start();
    }

    private void fetchTx0Preview(Pool pool) {
        MixConfig mixConfig = getWalletForm().getWallet().getMasterMixConfig();
        if(mixConfig.getScode() == null) {
            mixConfig.setScode("");
            EventManager.get().post(new WalletMasterMixConfigChangedEvent(getWalletForm().getWallet()));
        }

        Whirlpool whirlpool = AppServices.getWhirlpoolServices().getWhirlpool(walletId);
        if(tx0Previews != null && mixConfig.getScode().equals(whirlpool.getScode()) && tx0FeeTarget == whirlpool.getTx0FeeTarget()) {
            Tx0Preview tx0Preview = tx0Previews.getTx0Preview(pool.getPoolId());
            tx0PreviewProperty.set(tx0Preview);
        } else {
            tx0Previews = null;
            whirlpool.setScode(mixConfig.getScode());
            whirlpool.setTx0FeeTarget(tx0FeeTarget);
            whirlpool.setMixFeeTarget(tx0FeeTarget);

            Whirlpool.Tx0PreviewsService tx0PreviewsService = new Whirlpool.Tx0PreviewsService(whirlpool, utxoEntries);
            tx0PreviewsService.setOnRunning(workerStateEvent -> {
                premixOutputs.setText("Calculating...");
                amountToWhirlpool.setText("Calculating...");
                amountToWhirlpool.setText("Calculating...");
                unmixedChange.setText("Calculating...");
                txMinerFee.setText("Calculating...");
                totalFees.setText("Calculating...");
                tx0PreviewProperty.set(null);
            });
            tx0PreviewsService.setOnSucceeded(workerStateEvent -> {
                tx0Previews = tx0PreviewsService.getValue();
                Tx0Preview tx0Preview = tx0Previews.getTx0Preview(pool.getPoolId());
                tx0PreviewProperty.set(tx0Preview);
            });
            tx0PreviewsService.setOnFailed(workerStateEvent -> {
                Throwable exception = workerStateEvent.getSource().getException();
                while(exception.getCause() != null) {
                    exception = exception.getCause();
                }

                AppServices.showErrorDialog("Error fetching Tx0","Error fetching Tx0: " + exception.getMessage());
            });
            tx0PreviewsService.start();
        }
    }

    private static final class DisplayPool {
        private final Pool pool;

        public DisplayPool(Pool pool) {
            this.pool = pool;
        }

        @Override
        public String toString() {
            if(pool == null) {
                return "Fetching pools...";
            }

            UnitFormat format = Config.get().getUnitFormat() == null ? UnitFormat.DOT : Config.get().getUnitFormat();
            return format.formatBtcValue(pool.getDenomination()) + " BTC";
        }
    }
}
