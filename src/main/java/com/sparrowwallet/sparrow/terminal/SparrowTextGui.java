package com.sparrowwallet.sparrow.terminal;

import com.google.common.eventbus.Subscribe;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.sparrowwallet.drongo.Network;
import com.sparrowwallet.drongo.wallet.Wallet;
import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.event.*;
import com.sparrowwallet.sparrow.io.Config;
import com.sparrowwallet.sparrow.net.ServerType;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

import static com.sparrowwallet.sparrow.terminal.CustomActionListBoxTheme.GREY;

public class SparrowTextGui extends MultiWindowTextGUI {
    private final BasicWindow mainWindow;

    private final Panel titleBar;
    private final Panel statusBar;

    private final ProxyStatusLabel proxyStatusLabel;
    private final Label connectedLabel;
    private final Label statusLabel;
    private final ProgressBar statusProgress;

    private PauseTransition wait;
    private Timeline statusTimeline;
    private final DoubleProperty progressProperty = new SimpleDoubleProperty();

    public SparrowTextGui(SparrowTerminal sparrowTerminal, Screen screen, WindowManager windowManager, Component background) {
        super(screen, windowManager, background);

        this.mainWindow = new MasterActionWindow(sparrowTerminal);
        addWindow(mainWindow);

        Panel panel = new Panel(new BorderLayout());

        titleBar = new Panel(new GridLayout(3));
        new Label("Ashigaru Terminal (v1.0.0)").setBackgroundColor(TextColor.ANSI.BLACK).setForegroundColor(TextColor.ANSI.WHITE).addTo(titleBar);
        titleBar.setFillColorOverride(TextColor.ANSI.BLACK);
        this.proxyStatusLabel = (ProxyStatusLabel) new ProxyStatusLabel().setBackgroundColor(TextColor.ANSI.BLACK).setForegroundColor(TextColor.ANSI.WHITE);
        titleBar.addComponent(proxyStatusLabel, GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, true, false));
        this.connectedLabel = new Label(Network.get().toDisplayString() + " - disconnected").setBackgroundColor(TextColor.ANSI.BLACK).setForegroundColor(TextColor.ANSI.WHITE);
        titleBar.addComponent(connectedLabel, GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, true, false));
        panel.addComponent(titleBar, BorderLayout.Location.TOP);

        panel.addComponent(new EmptySpace(GREY));

        statusBar = new Panel(new GridLayout(2));
        statusBar.setFillColorOverride(TextColor.ANSI.BLACK);
        this.statusLabel = new Label("").setBackgroundColor(TextColor.ANSI.BLACK).setForegroundColor(TextColor.ANSI.WHITE).addTo(statusBar);
        this.statusProgress = new ProgressBar(0, 100, 10);
        statusBar.addComponent(statusProgress, GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, true, false));
        statusProgress.setRenderer(new BackgroundProgressBarRenderer());
        statusProgress.setLabelFormat(null);
        progressProperty.addListener((observable, oldValue, newValue) -> statusProgress.setValue((int) (newValue.doubleValue() * 100)));

        panel.addComponent(statusBar, BorderLayout.Location.BOTTOM);
        getBackgroundPane().setComponent(panel);
        this.setTheme(new CustomActionListBoxTheme());

        AppServices.get().start();
    }

    public void handleResize() {
        titleBar.invalidate();
        statusBar.invalidate();
    }

    public BasicWindow getMainWindow() {
        return mainWindow;
    }

    private void setDisconnectedLabel() {
        setConnectedLabel(null);
    }

    private void setConnectedLabel(Integer height) {
        getGUIThread().invokeLater(() -> {
            connectedLabel.setText(height == null ? Network.get().toDisplayString() + " - disconnected" : Network.get().toDisplayString() + " - blockheight " + height);
        });
    }

    private void updateProxyStatusLabel() {
        Platform.runLater(proxyStatusLabel::update);
    }

    @Subscribe
    public void connectionStart(ConnectionStartEvent event) {
        statusUpdated(new StatusEvent(event.getStatus(), 120));
    }

    @Subscribe
    public void connectionFailed(ConnectionFailedEvent event) {
        setDisconnectedLabel();
        updateProxyStatusLabel();
        statusUpdated(new StatusEvent("Connection failed: " + event.getMessage()));
    }

    @Subscribe
    public void connection(ConnectionEvent event) {
        setConnectedLabel(event.getBlockHeight());
        updateProxyStatusLabel();
        statusUpdated(new StatusEvent("Connected to " + Config.get().getServerDisplayName() + " at height " + event.getBlockHeight()));
    }

    @Subscribe
    public void disconnection(DisconnectionEvent event) {
        if(!AppServices.isConnecting() && !AppServices.isConnected()) {
            setDisconnectedLabel();
            statusUpdated(new StatusEvent("Disconnected"));
        }
    }

    @Subscribe
    public void newBlock(NewBlockEvent event) {
        setConnectedLabel(event.getHeight());
    }

    @Subscribe
    public void torBootStatus(TorBootStatusEvent event) {
        updateProxyStatusLabel();
    }

    @Subscribe
    public void torFailedStatus(TorFailedStatusEvent event) {
        updateProxyStatusLabel();
    }

    @Subscribe
    public void torReadyStatus(TorReadyStatusEvent event) {
        updateProxyStatusLabel();
    }

    @Subscribe
    public void statusUpdated(StatusEvent event) {
        getGUIThread().invokeLater(() -> statusLabel.setText(event.getStatus()));

        if(wait != null && wait.getStatus() == Animation.Status.RUNNING) {
            wait.stop();
        }
        wait = new PauseTransition(Duration.seconds(event.getShowDuration()));
        wait.setOnFinished((e) -> {
            if(statusLabel.getText().equals(event.getStatus())) {
                getGUIThread().invokeLater(() -> statusLabel.setText(""));
            }
        });
        wait.play();
    }

    @Subscribe
    public void timedEvent(TimedEvent event) {
        if(event.getTimeMills() == 0) {
            getGUIThread().invokeLater(() -> {
                statusLabel.setText("");
                statusProgress.setValue(0);
            });
        } else if(event.getTimeMills() < 0) {
            getGUIThread().invokeLater(() -> {
                statusLabel.setText(event.getStatus());
                statusProgress.setValue(0);
            });
        } else {
            getGUIThread().invokeLater(() -> {
                statusLabel.setText(event.getStatus());
            });
            statusTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressProperty, 0)),
                    new KeyFrame(Duration.millis(event.getTimeMills()), e -> {
                        getGUIThread().invokeLater(() -> {
                            statusLabel.setText("");
                            statusProgress.setValue(0);
                        });
                    }, new KeyValue(progressProperty, 1))
            );
            statusTimeline.setCycleCount(1);
            statusTimeline.play();
        }
    }

    @Subscribe
    public void walletHistoryStarted(WalletHistoryStartedEvent event) {
        statusUpdated(new StatusEvent("Updating wallet history - please wait..."));
    }

    @Subscribe
    public void walletHistoryFinished(WalletHistoryFinishedEvent event) {
        if(statusLabel.getText().equals("Updating wallet history - please wait...")) {
            getGUIThread().invokeLater(() -> statusLabel.setText(""));
        }
    }

    @Subscribe
    public void walletHistoryFailed(WalletHistoryFailedEvent event) {
        walletHistoryFinished(new WalletHistoryFinishedEvent(event.getWallet()));
        statusUpdated(new StatusEvent("Error retrieving wallet history" + (Config.get().getServerType() == ServerType.PUBLIC_ELECTRUM_SERVER ? ", trying another server..." : "")));
    }

    @Subscribe
    public void childWalletsAdded(ChildWalletsAddedEvent event) {
        if(!event.getChildWallets().isEmpty()) {
            for(Wallet childWallet : event.getChildWallets()) {
                SparrowTerminal.addWallet(event.getStorage(), childWallet);
            }
        }
    }

    @Subscribe
    public void cormorantSyncStatusEvent(CormorantSyncStatusEvent event) {
        statusUpdated(new StatusEvent("Syncing... (" + event.getProgress() + "% complete, synced to " + event.getTipAsString() + ")"));
    }

    @Subscribe
    public void cormorantScanStatusEvent(CormorantScanStatusEvent event) {
        statusUpdated(new StatusEvent(event.isCompleted() ? "" : "Scanning... (" + event.getProgress() + "% complete" + (event.getRemainingAsString().isEmpty() ? ")" : ", " + event.getRemainingAsString() + " remaining)")));
    }

    @Subscribe
    public void cormorantPruneStatus(CormorantPruneStatusEvent event) {
        statusUpdated(new StatusEvent("Error importing wallet, pruned date after wallet birthday"));
    }
}
