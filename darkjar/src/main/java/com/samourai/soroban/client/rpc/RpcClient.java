package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanServer;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

  private static final int WAIT_DELAY_MS = 500;
  private static final String ENDPOINT_RPC = "/rpc";

  private IHttpClient httpclient;
  private String url;

  public RpcClient(IHttpClient httpClient, boolean onion, NetworkParameters params) {
    this(httpClient, SorobanServer.get(params).getServerUrl(onion));
  }

  public RpcClient(IHttpClient httpClient, String serverUrl) {
    this.httpclient = httpClient;
    this.url = serverUrl + ENDPOINT_RPC;
  }

  private Observable<Map<String, Object>> call(String method, HashMap<String, Object> params)
      throws IOException {

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("content-type", "application/json");
    headers.put("User-Agent", "HotJava/1.1.2 FCS");

    HashMap<String, Object> body = new HashMap<String, Object>();
    body.put("method", method);
    body.put("jsonrpc", "2.0");
    body.put("id", 1);
    body.put("params", Arrays.asList(params));

    Observable<Map<String, Object>> result =
        httpclient
            .postJson(url, Map.class, headers, body)
            .map(
                new Function<Optional<Map>, Map<String, Object>>() {
                  @Override
                  public Map<String, Object> apply(Optional<Map> rpcOpt) throws Exception {
                    Map<String, Object> rpc = rpcOpt.get();

                    // check error
                    String error = (String) rpc.get("error");
                    if (error != null) {
                      throw new IOException("Error: " + error);
                    }

                    // check status if any
                    Map<String, String> result = (Map<String, String>) rpc.get("result");
                    if (result != null) {
                      String status = result.get("Status");
                      if (status != null && !status.equals("success")) {
                        throw new IOException("invalid status: " + status);
                      }
                    }
                    return rpc;
                  }
                });
    return result;
  }

  public Observable<String[]> directoryValues(String name) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entries", new String[0]);

    return call("directory.List", params)
        .map(
            new Function<Map<String, Object>, String[]>() {
              @Override
              public String[] apply(Map<String, Object> rpc) {
                Map<?, ?> result = (Map<?, ?>) rpc.get("result");
                ArrayList<?> src = (ArrayList<?>) result.get("Entries");
                String[] dest = new String[src.size()];
                System.arraycopy(src.toArray(), 0, dest, 0, src.size());
                return dest;
              }
            });
  }

  public Observable<String> directoryValue(String name) throws IOException {
    return directoryValues(name)
        .map(
            new Function<String[], String>() {
              @Override
              public String apply(String[] values) throws Exception {
                if (values == null || values.length == 0) {
                  throw new Exception("No value");
                }
                String value = values[values.length - 1];
                if (value.isEmpty()) {
                  throw new Exception("No value");
                }
                return value;
              }
            });
  }

  public Observable<String> directoryValueWait(final String name, final long timeoutMs) {
    return Observable.fromCallable(
        new Callable<String>() {
          @Override
          public String call() throws Exception {
            long timeStart = System.currentTimeMillis();
            long elapsedTime;
            while (true) {
              elapsedTime = System.currentTimeMillis() - timeStart;

              try {
                String value = directoryValue(name).blockingSingle();
                return value;
              } catch (Exception e) {
                // null value
              }

              // always check values at least once, or more when timeout not expired
              if (elapsedTime >= timeoutMs) {
                throw new TimeoutException(
                    String.format("Waited " + Math.round(elapsedTime / 1000) + "s, aborting"));
              }
              try {
                Thread.sleep(WAIT_DELAY_MS);
              } catch (InterruptedException e) {
              }
            }
          }
        });
  }

  public Observable directoryAdd(String name, String entry, String mode) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();

    params.put("Name", name);
    params.put("Entry", entry);
    params.put("Mode", mode);

    if (log.isDebugEnabled()) {
      log.debug("=> " + name + " (" + mode + ")");
    }
    return call("directory.Add", params);
  }

  public Observable directoryRemove(String name, String entry) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entry", entry);

    return call("directory.Remove", params);
  }

  public Observable<String> waitAndRemove(final String name, final long timeoutMs)
      throws Exception {
    return directoryValueWait(name, timeoutMs)
        .map(
            new Function<String, String>() {
              @Override
              public String apply(String value) throws Exception {
                directoryRemove(name, value).subscribe();
                return value;
              }
            });
  }
}
