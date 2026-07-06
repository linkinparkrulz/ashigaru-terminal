package com.sparrowwallet.sparrow.net.dojo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sparrowwallet.drongo.Network;
import com.sparrowwallet.sparrow.io.Server;
import com.sparrowwallet.sparrow.net.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers Electrum (Fulcrum) servers from the public Ashigaru/Dojo node directory.
 *
 * <p>The directory lists Dojo nodes, each carrying a {@code dojo.api} pairing payload
 * ({@code url}, {@code apikey}). For nodes running version 1.28+ we authenticate against the
 * node's backend and read its {@code /support/services} endpoint, which exposes the node's
 * indexer (a Fulcrum instance speaking the Electrum protocol). That indexer URL is what a
 * wallet can connect to.
 *
 * <p>Flow per node (matches the manual curl flow):
 * <ol>
 *     <li>{@code POST <base>/auth/login} form-encoded {@code apikey=<key>} &rarr; {@code access_token}</li>
 *     <li>{@code GET  <base>/support/services?at=<token>} &rarr; the {@code type == "indexer"} url</li>
 * </ol>
 *
 * All requests go through {@link HttpClientService}, which routes over the configured Tor proxy
 * (the directory and every node are onion services).
 */
public class DojoNodeDiscovery {
    private static final Logger log = LoggerFactory.getLogger(DojoNodeDiscovery.class);

    public static final String DIRECTORY_URL = "http://dojobayeryasshgghz537de5ckgd5hhi4z5sdeil3roeh65fwhdnu2yd.onion/data/dojos.json";

    private static final int MIN_MAJOR = 1;
    private static final int MIN_MINOR = 28;

    public static class DiscoveredNode {
        private final Server server;
        private final String name;
        private final String version;
        private final boolean verified;

        public DiscoveredNode(Server server, String name, String version, boolean verified) {
            this.server = server;
            this.name = name;
            this.version = version;
            this.verified = verified;
        }

        public Server getServer() {
            return server;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        /**
         * True if the node's {@code signed} block cryptographically verifies against its embedded
         * BIP47 payment code and references this node's pairing url.
         */
        public boolean isVerified() {
            return verified;
        }
    }

    /**
     * Fetches the node directory and derives an Electrum server for each eligible node
     * (version &ge; 1.28, active, matching the current network). Nodes that are offline or
     * refuse authentication are logged and skipped rather than aborting the whole run.
     *
     * <p>This method performs blocking network I/O over Tor and must be called off the UI thread.
     */
    public static List<DiscoveredNode> discover(HttpClientService http) throws Exception {
        String directoryJson = http.requestJson(DIRECTORY_URL, String.class, null);
        JsonObject directory = JsonParser.parseString(directoryJson).getAsJsonObject();
        JsonArray nodes = directory.getAsJsonArray("nodes");

        Map<String, DiscoveredNode> byHost = new LinkedHashMap<>();
        if(nodes == null) {
            return new ArrayList<>();
        }

        for(JsonElement element : nodes) {
            try {
                JsonObject node = element.getAsJsonObject();
                String status = optString(node, "status");
                String network = optString(node, "network");
                String version = optString(node, "version");

                if(!"active".equalsIgnoreCase(status)) {
                    continue;
                }
                if(!versionAtLeast(version, MIN_MAJOR, MIN_MINOR)) {
                    continue;
                }
                if(!matchesNetwork(network)) {
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
                String base = rawUrl;
                while(base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }

                String indexerUrl = fetchIndexerUrl(http, base, apikey);
                if(indexerUrl == null) {
                    continue;
                }

                String name = displayName(node);
                Server server = new Server(indexerUrl, name);
                boolean verified = SignedMessageVerifier.verify(optString(node, "signed"), rawUrl);
                byHost.putIfAbsent(server.getHost(), new DiscoveredNode(server, name, version, verified));
            } catch(Exception e) {
                log.warn("Skipping dojo node during discovery: " + e.getMessage());
            }
        }

        return new ArrayList<>(byHost.values());
    }

    private static String fetchIndexerUrl(HttpClientService http, String base, String apikey) throws Exception {
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

        for(JsonElement s : services) {
            JsonObject service = s.getAsJsonObject();
            if("indexer".equalsIgnoreCase(optString(service, "type"))) {
                String url = optString(service, "url");
                if(url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        return null;
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
     * Matches a node's network field ("mainnet"/"testnet") against the wallet's current network.
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
