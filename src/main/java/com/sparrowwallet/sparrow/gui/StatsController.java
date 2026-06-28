package com.sparrowwallet.sparrow.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sparrowwallet.sparrow.AppServices;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatsController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    private static final String API_BASE = "https://whirlpoolstats.xyz/api";
    private static final String SUMMARY_URL = API_BASE + "/summary";
    private static final String CHARTS_URL = API_BASE + "/charts";
    private static final String TXS_URL = API_BASE + "/txs";
    private static final String SOURCE_URL = "https://whirlpoolstats.xyz";

    @FXML private Button refreshBtn;
    @FXML private Label statusLabel;
    @FXML private Label totalBtcLabel;
    @FXML private Label blockRangeLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label pool025SizeBadge;
    @FXML private Label pool025EnteredLabel;
    @FXML private Label pool025CyclesLabel;
    @FXML private Label pool025Tx0CountLabel;
    @FXML private Label pool25SizeBadge;
    @FXML private Label pool25EnteredLabel;
    @FXML private Label pool25CyclesLabel;
    @FXML private Label pool25Tx0CountLabel;
    @FXML private LineChart<Number, Number> poolChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Hyperlink sourceLink;
    @FXML private ListView<TxRow> txListView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        poolChart.setCreateSymbols(false);
        poolChart.setAnimated(false);
        xAxis.setForceZeroInRange(false);
        xAxis.setTickUnit(10000);
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
        txListView.setCellFactory(lv -> new TxCell());
        fetchStats();
    }

    @FXML
    private void onRefresh() {
        fetchStats();
    }

    @FXML
    private void onOpenSource() {
        try {
            AppServices.get().getApplication().getHostServices().showDocument(SOURCE_URL);
        } catch(Exception e) {
            log.warn("Could not open browser", e);
        }
    }

    private void fetchStats() {
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.setText("Fetching statistics from whirlpoolstats.xyz…");
        refreshBtn.setDisable(true);

        Task<StatsData> task = new Task<>() {
            @Override
            protected StatsData call() throws Exception {
                String summaryJson = httpGet(SUMMARY_URL);
                String chartsJson = httpGet(CHARTS_URL);
                String txsJson = httpGet(TXS_URL);
                return parseStats(summaryJson, chartsJson, txsJson);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            StatsData data = task.getValue();
            renderStats(data);
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
            refreshBtn.setDisable(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            log.error("Failed to fetch whirlpool stats", ex);
            statusLabel.setText("Could not fetch stats: " + ex.getMessage());
            refreshBtn.setDisable(false);
        }));

        Thread t = new Thread(task, "whirlpool-stats-fetch");
        t.setDaemon(true);
        t.start();
    }

    private void renderStats(StatsData data) {
        totalBtcLabel.setText(String.format(Locale.ROOT, "%.3f BTC", data.totalBtc));
        blockRangeLabel.setText(data.startBlock + " – " + data.tipBlock);
        if(data.lastUpdatedTs > 0) {
            lastUpdatedLabel.setText(new SimpleDateFormat("HH:mm:ss 'UTC', MMM d, yyyy")
                    .format(new Date(data.lastUpdatedTs * 1000)));
        } else {
            lastUpdatedLabel.setText("—");
        }

        // Pool 0.025
        PoolSummary p025 = data.pools.stream().filter(p -> p.poolName.equals("0.025_BTC_Pool")).findFirst().orElse(null);
        if(p025 != null) {
            pool025SizeBadge.setText(String.format(Locale.ROOT, "%.3f BTC", p025.unspentBtc));
            pool025EnteredLabel.setText(String.format("Total entered: %.3f BTC", p025.enteredBtc));
            pool025CyclesLabel.setText("Cycles: " + p025.cycles + "  ·  UTXOs: " + p025.unspentUtxos);
            pool025Tx0CountLabel.setText(String.format("Tx0 count: %d  ·  Avg fee efficiency: %.1f%%", p025.tx0Count, p025.avgFeeEfficiencyPct));
        }

        // Pool 0.25
        PoolSummary p25 = data.pools.stream().filter(p -> p.poolName.equals("0.25_BTC_Pool")).findFirst().orElse(null);
        if(p25 != null) {
            pool25SizeBadge.setText(String.format(Locale.ROOT, "%.3f BTC", p25.unspentBtc));
            pool25EnteredLabel.setText(String.format("Total entered: %.3f BTC", p25.enteredBtc));
            pool25CyclesLabel.setText("Cycles: " + p25.cycles + "  ·  UTXOs: " + p25.unspentUtxos);
            pool25Tx0CountLabel.setText(String.format("Tx0 count: %d  ·  Avg fee efficiency: %.1f%%", p25.tx0Count, p25.avgFeeEfficiencyPct));
        }

        renderChart(data);
        txListView.getItems().setAll(data.recentTxs);
    }

    private void renderChart(StatsData data) {
        poolChart.getData().clear();
        if(data.capacityBlocks == null || data.capacityBlocks.isEmpty()) return;

        for(Map.Entry<String, List<Double>> entry : data.capacitySeries.entrySet()) {
            String name = entry.getKey().replace("_BTC_Pool", " BTC Pool");
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(name);
            List<Double> values = entry.getValue();
            for(int i = 0; i < data.capacityBlocks.size() && i < values.size(); i++) {
                series.getData().add(new XYChart.Data<>(data.capacityBlocks.get(i), values.get(i)));
            }
            poolChart.getData().add(series);
        }
    }

    private static String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        Proxy proxy = AppServices.getProxy();
        HttpURLConnection conn = (HttpURLConnection) (proxy != null
                ? url.openConnection(proxy)
                : url.openConnection());
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Ashigaru-Desktop");
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if(code != 200) {
            throw new IOException("HTTP " + code + " for " + urlStr);
        }

        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static StatsData parseStats(String summaryJson, String chartsJson, String txsJson) {
        StatsData data = new StatsData();
        JsonObject summary = JsonParser.parseString(summaryJson).getAsJsonObject();

        data.startBlock = summary.get("start_block_height").getAsInt();
        data.tipBlock = summary.get("tip_height").getAsInt();
        data.lastUpdatedTs = summary.has("last_report_refresh_ts")
                ? summary.get("last_report_refresh_ts").getAsLong() : 0;

        double total = 0;
        JsonArray poolsArr = summary.getAsJsonArray("pools");
        for(JsonElement el : poolsArr) {
            JsonObject pObj = el.getAsJsonObject();
            PoolSummary pool = new PoolSummary(
                    pObj.get("label").getAsString(),
                    pObj.get("pool").getAsString(),
                    pObj.get("unspent_btc").getAsDouble(),
                    pObj.get("entered_btc").getAsDouble(),
                    pObj.get("cycles").getAsInt(),
                    pObj.has("tx0_count") ? pObj.get("tx0_count").getAsInt() : 0,
                    pObj.has("unspent_utxos") ? pObj.get("unspent_utxos").getAsInt() : 0,
                    pObj.has("avg_fee_efficiency_pct") ? pObj.get("avg_fee_efficiency_pct").getAsDouble() : 0,
                    pObj.has("color") ? pObj.get("color").getAsString() : "#8e8e93"
            );
            data.pools.add(pool);
            total += pool.unspentBtc;
        }
        data.totalBtc = total;

        // Parse charts — capacity series
        JsonObject charts = JsonParser.parseString(chartsJson).getAsJsonObject();
        if(charts.has("capacity")) {
            JsonObject cap = charts.getAsJsonObject("capacity");
            JsonArray blocksArr = cap.getAsJsonArray("blocks");
            data.capacityBlocks = new ArrayList<>();
            for(JsonElement b : blocksArr) {
                data.capacityBlocks.add(b.getAsInt());
            }
            JsonObject seriesObj = cap.getAsJsonObject("series");
            data.capacitySeries = new LinkedHashMap<>();
            for(Map.Entry<String, JsonElement> entry : seriesObj.entrySet()) {
                List<Double> values = new ArrayList<>();
                for(JsonElement v : entry.getValue().getAsJsonArray()) {
                    values.add(v.getAsDouble());
                }
                data.capacitySeries.put(entry.getKey(), values);
            }
        }

        // Parse recent txs
        if(txsJson != null && !txsJson.isEmpty()) {
            JsonObject txsObj = JsonParser.parseString(txsJson).getAsJsonObject();
            if(txsObj.has("items")) {
                JsonArray items = txsObj.getAsJsonArray("items");
                for(JsonElement el : items) {
                    JsonObject tx = el.getAsJsonObject();
                    String txid = tx.get("txid").getAsString();
                    int blockHeight = tx.get("block_height").getAsInt();
                    String poolLabel = tx.has("pool_label") ? tx.get("pool_label").getAsString() : "—";
                    String poolColor = tx.has("pool_color") ? tx.get("pool_color").getAsString() : "#8e8e93";

                    List<TxInput> inputs = new ArrayList<>();
                    if(tx.has("tx0_inputs")) {
                        for(JsonElement in : tx.getAsJsonArray("tx0_inputs")) {
                            JsonObject inObj = in.getAsJsonObject();
                            inputs.add(new TxInput(
                                    inObj.get("txid").getAsString(),
                                    inObj.has("fee_efficiency_pct") ? inObj.get("fee_efficiency_pct").getAsString() : "—"
                            ));
                        }
                    }
                    String amIExposed = tx.has("am_i_exposed_url") ? tx.get("am_i_exposed_url").getAsString() : null;
                    data.recentTxs.add(new TxRow(txid, blockHeight, poolLabel, poolColor, inputs, amIExposed));
                }
            }
        }

        return data;
    }

    // -------------------------------------------------------------------------
    // Data models
    // ----------------------------------------------------------------

    private static class StatsData {
        double totalBtc;
        int startBlock;
        int tipBlock;
        long lastUpdatedTs;
        List<PoolSummary> pools = new ArrayList<>();
        List<Integer> capacityBlocks;
        Map<String, List<Double>> capacitySeries;
        List<TxRow> recentTxs = new ArrayList<>();
    }

    private record PoolSummary(String label, String poolName, double unspentBtc, double enteredBtc,
                               int cycles, int tx0Count, int unspentUtxos, double avgFeeEfficiencyPct,
                               String color) {}

    private record TxInput(String txid, String feeEfficiencyPct) {}

    private record TxRow(String txid, int blockHeight, String poolLabel, String poolColor,
                         List<TxInput> inputs, String amIExposedUrl) {}

    // -------------------------------------------------------------------------
    // Transaction cell
    // ----------------------------------------------------------------

    private static class TxCell extends ListCell<TxRow> {
        private final Label txidLabel = new Label();
        private final Label poolBadge = new Label();
        private final Label blockLabel = new Label();
        private final Label feeLabel = new Label();
        private final HBox topLine = new HBox(8, poolBadge, blockLabel);
        private final VBox root = new VBox(4, topLine, txidLabel, feeLabel);

        @Override
        protected void updateItem(TxRow row, boolean empty) {
            super.updateItem(row, empty);
            if(empty || row == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            poolBadge.setText(row.poolLabel);
            poolBadge.setStyle("-fx-background-color: " + row.poolColor + "; -fx-text-fill: white; -fx-padding: 2 8 2 8; -fx-background-radius: 10;");

            blockLabel.setText("Block " + row.blockHeight);
            blockLabel.getStyleClass().add("card-faint");

            String shortTxid = row.txid.substring(0, 12) + "…" + row.txid.substring(row.txid.length() - 8);
            txidLabel.setText(shortTxid);
            txidLabel.getStyleClass().add("card-secondary");

            if(!row.inputs.isEmpty()) {
                String fees = row.inputs.stream()
                        .map(i -> i.feeEfficiencyPct + "%")
                        .reduce((a, b) -> a + " · " + b)
                        .orElse("—");
                feeLabel.setText("Fee efficiency: " + fees);
                feeLabel.getStyleClass().add("card-faint");
                feeLabel.setVisible(true);
                feeLabel.setManaged(true);
            } else {
                feeLabel.setVisible(false);
                feeLabel.setManaged(false);
            }

            setText(null);
            setGraphic(root);
        }
    }
}
