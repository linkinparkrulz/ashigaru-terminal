package com.sparrowwallet.sparrow.net.dojo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sparrowwallet.drongo.Network;
import com.sparrowwallet.sparrow.AppServices;
import com.sparrowwallet.sparrow.EventManager;
import com.sparrowwallet.sparrow.event.DiscoveredServersChangedEvent;
import com.sparrowwallet.sparrow.io.Config;
import com.sparrowwallet.sparrow.io.Server;
import com.sparrowwallet.sparrow.net.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Discovers the Electrum (Fulcrum) servers — and block explorers — running on Dojos listed in the
 * public dojobay.pw directory.
 *
 * <p>The directory lists Dojos, each carrying a {@code dojo.api} pairing payload ({@code url},
 * {@code apikey}). For Dojos running version 1.28+ we authenticate against the Dojo backend and read
 * its {@code /support/services} endpoint, which exposes the Dojo's indexer (a Fulcrum instance
 * speaking the Electrum protocol) and its block explorer.
 *
 * <p>Flow per Dojo (matches the manual curl flow):
 * <ol>
 *     <li>{@code POST <base>/auth/login} form-encoded {@code apikey=<key>} &rarr; {@code access_token}</li>
 *     <li>{@code GET  <base>/support/services?at=<token>} &rarr; the {@code indexer} and {@code explorer} urls</li>
 * </ol>
 *
 * All requests go through {@link HttpClientService}, which routes over the configured Tor proxy
 * (the directory and every Dojo are onion services). Dojos are probed in parallel with a per-Dojo
 * timeout so one unreachable Dojo cannot stall discovery.
 */
public class DojoNodeDiscovery {
    private static final Logger log = LoggerFactory.getLogger(DojoNodeDiscovery.class);

    public static final String DIRECTORY_URL = "http://dojobayeryasshgghz537de5ckgd5hhi4z5sdeil3roeh65fwhdnu2yd.onion/data/dojos.json";

    private static final int MIN_MAJOR = 1;
    private static final int MIN_MINOR = 28;
    private static final int MAX_THREADS = 3;
    private static final int PER_NODE_TIMEOUT_SECONDS = 60;

    public static class DiscoveredNode {
        private final Server indexerServer;
        private final Server explorerServer;
        private final String name;
        private final boolean verified;

        public DiscoveredNode(Server indexerServer, Server explorerServer, String name, boolean verified) {
            this.indexerServer = indexerServer;
            this.explorerServer = explorerServer;
            this.name = name;
            this.verified = verified;
        }

        public Server getIndexerServer() {
            return indexerServer;
        }

        public Server getExplorerServer() {
            return explorerServer;
        }

        public String getName() {
            return name;
        }

        /**
         * True if the Dojo's {@code signed} block cryptographically verifies against its embedded
         * BIP47 payment code and references this Dojo's pairing url.
         */
        public boolean isVerified() {
            return verified;
        }
    }

    public static class DiscoveryResult {
        private final List<DiscoveredNode> nodes;
        private final int attempted;
        private final int unverified;
        private final int unreachable;

        public DiscoveryResult(List<DiscoveredNode> nodes, int attempted, int unverified, int unreachable) {
            this.nodes = nodes;
            this.attempted = attempted;
            this.unverified = unverified;
            this.unreachable = unreachable;
        }

        public List<DiscoveredNode> getNodes() {
            return nodes;
        }

        public int getAttempted() {
            return attempted;
        }

        /**
         * Number of candidates whose signed block failed verification (bad/missing signature, or the
         * signed identity did not match the advertised payment code). These are never authenticated.
         */
        public int getUnverified() {
            return unverified;
        }

        public int getUnreachable() {
            return unreachable;
        }
    }

    private record Candidate(String name, String version, String url, String apikey, String signedBlock, String paymentCode) { }

    private record Services(String indexerUrl, String explorerUrl) { }

    /**
     * Runs discovery on a daemon thread, persists each verified Dojo's Electrum server and block
     * explorer to {@link Config}, and posts a {@link DiscoveredServersChangedEvent} if anything
     * changed. Never throws — failures are logged. Safe to call from the UI thread.
     */
    public static void discoverInBackground() {
        Thread thread = new Thread(() -> {
            try {
                DiscoveryResult result = discover(AppServices.getHttpClientService());
                boolean changed = false;
                for(DiscoveredNode node : result.getNodes()) {
                    if(node.isVerified()) {
                        changed |= Config.get().addDiscoveredServer(node.getIndexerServer());
                        if(node.getExplorerServer() != null) {
                            changed |= Config.get().addDiscoveredExplorer(node.getExplorerServer());
                        }
                    }
                }
                if(changed) {
                    EventManager.get().post(new DiscoveredServersChangedEvent());
                }
                log.info("Dojo discovery: " + result.getNodes().size() + " verified+reachable, " + result.getUnverified()
                        + " unverified, " + result.getUnreachable() + " unreachable of " + result.getAttempted() + " candidates");
            } catch(Exception e) {
                log.warn("Background Dojo discovery failed: " + e.getMessage());
            }
        }, "dojo-discovery-bg");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Fetches the directory and derives an Electrum server (and block explorer) for each eligible
     * Dojo (version &ge; 1.28, matching the current network). Dojos are probed in parallel;
     * reachability is decided by a successful login + services fetch, so the directory's own
     * (10-minute snapshot) active flag is not used. Unreachable Dojos are counted, not surfaced.
     *
     * <p>Performs blocking network I/O over Tor — must be called off the UI thread.
     */
    public static DiscoveryResult discover(HttpClientService http) throws Exception {
        String directoryJson = http.requestJson(DIRECTORY_URL, String.class, null);
        JsonObject directory = JsonParser.parseString(directoryJson).getAsJsonObject();
        JsonArray nodes = directory.getAsJsonArray("nodes");

        List<Candidate> candidates = new ArrayList<>();
        if(nodes != null) {
            for(JsonElement element : nodes) {
                try {
                    JsonObject node = element.getAsJsonObject();
                    if(!versionAtLeast(optString(node, "version"), MIN_MAJOR, MIN_MINOR)) {
                        continue;
                    }
                    if(!matchesNetwork(optString(node, "network"))) {
                        continue;
                    }

                    JsonObject payload = node.getAsJsonObject("payload");
                    JsonObject pairing = payload == null ? null : payload.getAsJsonObject("pairing");
                    if(pairing == null) {
                        continue;
                    }

                    String rawUrl = optString(pairing, "url");
                    String apikey = optString(pairing, "apikey");
                    if(rawUrl == null || rawUrl.isEmpty() || apikey == null || apikey.isEmpty()) {
                        continue;
                    }

                    candidates.add(new Candidate(displayName(node), optString(node, "version"), rawUrl, apikey, optString(node, "signed"), optString(node, "paymentCode")));
                } catch(Exception e) {
                    log.warn("Skipping malformed dojo directory entry: " + e.getMessage());
                }
            }
        }

        // Step 2 (verify) before step 3 (authenticate): the signature check is pure crypto with no
        // network I/O, so gate on it first. Only candidates whose signed block verifies — and whose
        // signed identity matches the advertised payment code — are ever authenticated, so no apikey
        // or onion circuit is spent on an unverified or spoofed listing.
        List<Candidate> verifiedCandidates = new ArrayList<>();
        int unverified = 0;
        for(Candidate candidate : candidates) {
            if(SignedMessageVerifier.verify(candidate.signedBlock(), candidate.url(), candidate.paymentCode())) {
                verifiedCandidates.add(candidate);
            } else {
                unverified++;
                log.info("Skipping unverified dojo directory entry: " + candidate.name());
            }
        }

        List<DiscoveredNode> discovered = new ArrayList<>();
        int unreachable = 0;
        if(!verifiedCandidates.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(MAX_THREADS, verifiedCandidates.size()), r -> {
                Thread t = new Thread(r, "dojo-discovery");
                t.setDaemon(true);
                return t;
            });
            try {
                List<Future<DiscoveredNode>> futures = new ArrayList<>();
                for(Candidate candidate : verifiedCandidates) {
                    futures.add(executor.submit(() -> resolveNode(http, candidate)));
                }

                Set<String> seenHosts = new HashSet<>();
                for(Future<DiscoveredNode> future : futures) {
                    DiscoveredNode node;
                    try {
                        node = future.get(PER_NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch(Exception e) {
                        node = null;
                    }

                    if(node == null) {
                        unreachable++;
                    } else if(seenHosts.add(node.getIndexerServer().getHost())) {
                        discovered.add(node);
                    }
                }
            } finally {
                executor.shutdownNow();
            }
        }

        return new DiscoveryResult(discovered, candidates.size(), unverified, unreachable);
    }

    /**
     * Authenticates to an already-verified Dojo and derives its Electrum server (and block explorer)
     * from the {@code /support/services} endpoint. Callers must only pass candidates that have passed
     * {@link SignedMessageVerifier#verify(String, String, String)}; returned nodes are therefore
     * always verified. Returns null if the Dojo is unreachable.
     */
    private static DiscoveredNode resolveNode(HttpClientService http, Candidate candidate) {
        try {
            String base = candidate.url();
            while(base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }

            Services services = fetchServices(http, base, candidate.apikey());
            if(services == null || services.indexerUrl() == null) {
                return null;
            }

            Server indexerServer = new Server(services.indexerUrl(), candidate.name());
            Server explorerServer = services.explorerUrl() == null ? null : new Server(services.explorerUrl(), candidate.name());
            return new DiscoveredNode(indexerServer, explorerServer, candidate.name(), true);
        } catch(Exception e) {
            log.warn("Dojo unreachable during discovery (" + candidate.name() + "): " + e.getMessage());
            return null;
        }
    }

    private static Services fetchServices(HttpClientService http, String base, String apikey) throws Exception {
        String loginBody = http.postString(base + "/auth/login", null,
                "application/x-www-form-urlencoded",
                "apikey=" + URLEncoder.encode(apikey, "UTF-8"));
        JsonObject loginObj = JsonParser.parseString(loginBody).getAsJsonObject();
        JsonObject auth = loginObj.getAsJsonObject("authorizations");
        if(auth == null || !auth.has("access_token") || auth.get("access_token").isJsonNull()) {
            return null;
        }
        String token = auth.get("access_token").getAsString();

        String servicesJson = http.requestJson(base + "/support/services?at=" + token, String.class, null);
        JsonObject servicesObj = JsonParser.parseString(servicesJson).getAsJsonObject();
        JsonArray services = servicesObj.getAsJsonArray("services");
        if(services == null) {
            return null;
        }

        String indexerUrl = null;
        String explorerUrl = null;
        for(JsonElement s : services) {
            JsonObject service = s.getAsJsonObject();
            String type = optString(service, "type");
            String url = optString(service, "url");
            if(url == null || url.isEmpty() || "N/A".equalsIgnoreCase(url)) {
                continue;
            }

            if("indexer".equalsIgnoreCase(type) && indexerUrl == null) {
                indexerUrl = url;
            } else if("explorer".equalsIgnoreCase(type) && explorerUrl == null) {
                String kind = optString(service, "kind");
                if(kind == null || !kind.toLowerCase(Locale.ROOT).contains("null")) {
                    explorerUrl = url;
                }
            }
        }
        return new Services(indexerUrl, explorerUrl);
    }

    private static String displayName(JsonObject node) {
        String name = optString(node, "name");
        if(name == null || name.isEmpty()) {
            name = optString(node, "paynym");
        }
        if(name == null || name.isEmpty()) {
            name = optString(node, "id");
        }
        return name;
    }

    /**
     * Returns true if {@code version} (e.g. "1.28.2") is at least {@code minMajor.minMinor}.
     * Tolerates trailing patch/qualifier segments and malformed values (which return false).
     */
    static boolean versionAtLeast(String version, int minMajor, int minMinor) {
        if(version == null || version.isEmpty()) {
            return false;
        }
        try {
            String[] parts = version.trim().split("[.\\-+]");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if(major != minMajor) {
                return major > minMajor;
            }
            return minor >= minMinor;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Matches a Dojo's network field ("mainnet"/"testnet") against the wallet's current network.
     * All test networks (testnet3, testnet4, signet) are treated as non-mainnet.
     */
    static boolean matchesNetwork(String network) {
        if(network == null) {
            return false;
        }
        boolean nodeMainnet = "mainnet".equalsIgnoreCase(network.trim());
        boolean walletMainnet = Network.get() == Network.MAINNET;
        return nodeMainnet == walletMainnet;
    }

    private static String optString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }
}
