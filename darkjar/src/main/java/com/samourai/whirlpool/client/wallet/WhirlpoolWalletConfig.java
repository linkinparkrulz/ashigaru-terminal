package com.samourai.whirlpool.client.wallet;

import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.bip47.rpc.java.SecretPointFactoryJava;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.ITx0ParamServiceConfig;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.IndexRange;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class WhirlpoolWalletConfig extends WhirlpoolClientConfig implements ITx0ParamServiceConfig {
  private final Logger log = LoggerFactory.getLogger(WhirlpoolWalletConfig.class);

  private DataSourceFactory dataSourceFactory;
  private DataPersisterFactory dataPersisterFactory;
  private boolean mobile;

  private int maxClients;
  private int maxClientsPerPool;
  private boolean liquidityClient;
  private int clientDelay;
  private int autoTx0Delay;
  private String autoTx0PoolId;
  private boolean autoTx0Aggregate; // only on testnet
  private Tx0FeeTarget autoTx0FeeTarget;
  private boolean autoMix;
  private String scode;
  private int tx0MaxOutputs;
  private int tx0MaxRetry;
  private boolean tx0StrictMode;
  private Map<String, Long> overspend;

  private int tx0MinConfirmations;
  private int refreshUtxoDelay;
  private int refreshPoolsDelay;

  private int feeMin;
  private int feeMax;
  private int feeFallback;

  private boolean resyncOnFirstRun;
  private boolean postmixIndexAutoFix;
  private int persistDelaySeconds;
  private String partner;

  private ISecretPointFactory secretPointFactory;

  public WhirlpoolWalletConfig(
      DataSourceFactory dataSourceFactory,
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      TorClientService torClientService,
      ServerApi serverApi,
      NetworkParameters params,
      boolean mobile) {
    // Android => odd indexs, CLI => even indexs
    super(
        httpClientService,
        stompClientService,
        torClientService,
        serverApi,
        null,
        params,
        mobile ? IndexRange.ODD : IndexRange.EVEN);

    this.dataSourceFactory = dataSourceFactory;
    this.dataPersisterFactory = new FileDataPersisterFactory();
    this.mobile = mobile;

    // default settings
    this.maxClients = mobile ? 1 : 5;
    this.maxClientsPerPool = 1;
    this.liquidityClient = mobile ? false : true;
    this.clientDelay = 30;
    this.autoTx0Delay = 60;
    this.autoTx0PoolId = null;
    this.autoTx0Aggregate = false;
    this.autoTx0FeeTarget = Tx0FeeTarget.BLOCKS_4;
    this.autoMix = true;
    this.scode = null;
    this.tx0MaxOutputs = 0;
    this.tx0MaxRetry = 5;
    this.tx0StrictMode = true;
    this.overspend = new LinkedHashMap<String, Long>();

    // technical settings
    this.tx0MinConfirmations = 0;
    this.refreshUtxoDelay = 60; // 1min
    this.refreshPoolsDelay = 600; // 10min

    this.feeMin = 1;
    this.feeMax = 510;
    this.feeFallback = 75;

    this.resyncOnFirstRun = false;
    this.postmixIndexAutoFix = true;
    this.persistDelaySeconds = 10;
    this.partner = WhirlpoolProtocol.PARTNER_ID_ASHIGARU;

    this.secretPointFactory = SecretPointFactoryJava.getInstance();
  }

  public void verify() throws Exception {
    boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(getNetworkParameters());

    // require testnet for autoTx0Aggregate
    if (autoTx0Aggregate && !isTestnet) {
      throw new RuntimeException("--auto-tx0 is required for --auto-tx0-aggregate");
    }

    // require autoTx0PoolId for autoTx0Aggregate
    if (autoTx0Aggregate && StringUtils.isEmpty(autoTx0PoolId)) {
      throw new RuntimeException("--auto-tx0 is required for --auto-tx0-aggregate");
    }

    // verify JCE provider doesn't throw any exception
    try {
      ECKey ecKey = new ECKey();
      secretPointFactory
          .newSecretPoint(ecKey.getPrivKeyBytes(), ecKey.getPubKey())
          .ECDHSecretAsBytes();
    } catch (Exception e) {
      log.error("secretPointFactory not supported", e);
      String javaVersion = System.getProperty("java.version");
      throw new NotifiableException(
          "Java version not supported, please use a another Java runtime (current: "
              + javaVersion
              + ", recommended: OpenJDK 8-11).");
    }
  }

  public DataSourceFactory getDataSourceFactory() {
    return dataSourceFactory;
  }

  public DataPersisterFactory getDataPersisterFactory() {
    return dataPersisterFactory;
  }

  public void setDataPersisterFactory(DataPersisterFactory dataPersisterFactory) {
    this.dataPersisterFactory = dataPersisterFactory;
  }

  public boolean isMobile() {
    return mobile;
  }

  public void setMobile(boolean mobile) {
    this.mobile = mobile;
  }

  public int getMaxClients() {
    return maxClients;
  }

  public void setMaxClients(int maxClients) {
    this.maxClients = maxClients;
  }

  public int getMaxClientsPerPool() {
    return maxClientsPerPool;
  }

  public void setMaxClientsPerPool(int maxClientsPerPool) {
    this.maxClientsPerPool = maxClientsPerPool;
  }

  public boolean isLiquidityClient() {
    return liquidityClient;
  }

  public void setLiquidityClient(boolean liquidityClient) {
    this.liquidityClient = liquidityClient;
  }

  public int getClientDelay() {
    return clientDelay;
  }

  public void setClientDelay(int clientDelay) {
    this.clientDelay = clientDelay;
  }

  public int getAutoTx0Delay() {
    return autoTx0Delay;
  }

  public void setAutoTx0Delay(int autoTx0Delay) {
    this.autoTx0Delay = autoTx0Delay;
  }

  public boolean isAutoTx0() {
    return !StringUtils.isEmpty(autoTx0PoolId);
  }

  public String getAutoTx0PoolId() {
    return autoTx0PoolId;
  }

  public boolean isAutoTx0Aggregate() {
    return autoTx0Aggregate;
  }

  public void setAutoTx0Aggregate(boolean autoTx0Aggregate) {
    this.autoTx0Aggregate = autoTx0Aggregate;
  }

  public void setAutoTx0PoolId(String autoTx0PoolId) {
    this.autoTx0PoolId = autoTx0PoolId;
  }

  public Tx0FeeTarget getAutoTx0FeeTarget() {
    return autoTx0FeeTarget;
  }

  public void setAutoTx0FeeTarget(Tx0FeeTarget autoTx0FeeTarget) {
    this.autoTx0FeeTarget = autoTx0FeeTarget;
  }

  public boolean isAutoMix() {
    return autoMix;
  }

  public void setAutoMix(boolean autoMix) {
    this.autoMix = autoMix;
  }

  public String getScode() {
    return scode;
  }

  public void setScode(String scode) {
    this.scode = scode;
  }

  public int getTx0MaxOutputs() {
    return tx0MaxOutputs;
  }

  public void setTx0MaxOutputs(int tx0MaxOutputs) {
    this.tx0MaxOutputs = tx0MaxOutputs;
  }

  public int getTx0MaxRetry() {
    return tx0MaxRetry;
  }

  public void setTx0MaxRetry(int tx0MaxRetry) {
    this.tx0MaxRetry = tx0MaxRetry;
  }

  public boolean isTx0StrictMode() {
    return tx0StrictMode;
  }

  public void setTx0StrictMode(boolean tx0StrictMode) {
    this.tx0StrictMode = tx0StrictMode;
  }

  public Long getOverspend(String poolId) {
    return overspend != null ? overspend.get(poolId) : null;
  }

  public void setOverspend(Map<String, Long> overspend) {
    this.overspend = overspend;
  }

  public int getTx0MinConfirmations() {
    return tx0MinConfirmations;
  }

  public void setTx0MinConfirmations(int tx0MinConfirmations) {
    this.tx0MinConfirmations = tx0MinConfirmations;
  }

  public int getRefreshUtxoDelay() {
    return refreshUtxoDelay;
  }

  public void setRefreshUtxoDelay(int refreshUtxoDelay) {
    this.refreshUtxoDelay = refreshUtxoDelay;
  }

  public int getRefreshPoolsDelay() {
    return refreshPoolsDelay;
  }

  public void setRefreshPoolsDelay(int refreshPoolsDelay) {
    this.refreshPoolsDelay = refreshPoolsDelay;
  }

  public int getFeeMin() {
    return feeMin;
  }

  public void setFeeMin(int feeMin) {
    this.feeMin = feeMin;
  }

  public int getFeeMax() {
    return feeMax;
  }

  public void setFeeMax(int feeMax) {
    this.feeMax = feeMax;
  }

  public int getFeeFallback() {
    return feeFallback;
  }

  public void setFeeFallback(int feeFallback) {
    this.feeFallback = feeFallback;
  }

  public boolean isResyncOnFirstRun() {
    return resyncOnFirstRun;
  }

  public void setResyncOnFirstRun(boolean resyncOnFirstRun) {
    this.resyncOnFirstRun = resyncOnFirstRun;
  }

  public boolean isPostmixIndexAutoFix() {
    return postmixIndexAutoFix;
  }

  public void setPostmixIndexAutoFix(boolean postmixIndexAutoFix) {
    this.postmixIndexAutoFix = postmixIndexAutoFix;
  }

  public int getPersistDelaySeconds() {
    return persistDelaySeconds;
  }

  public void setPersistDelaySeconds(int persistDelaySeconds) {
    this.persistDelaySeconds = persistDelaySeconds;
  }

  public String getPartner() {
    return partner;
  }

  public void setPartner(String partner) {
    this.partner = partner;
  }

  public ISecretPointFactory getSecretPointFactory() {
    return secretPointFactory;
  }

  public void setSecretPointFactory(ISecretPointFactory secretPointFactory) {
    this.secretPointFactory = secretPointFactory;
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new LinkedHashMap<String, String>();
    configInfo.put("dataSourceFactory", dataSourceFactory.getClass().getSimpleName());
    configInfo.put("dataPersisterFactory", dataPersisterFactory.getClass().getSimpleName());
    configInfo.put("protocolVersion", WhirlpoolProtocol.PROTOCOL_VERSION);
    configInfo.put(
        "server", getServerApi() + ", network=" + getNetworkParameters().getPaymentProtocolId());
    configInfo.put(
        "externalDestination",
        (getExternalDestination() != null ? getExternalDestination().toString() : "null"));
    configInfo.put("indexRangePostmix", getIndexRangePostmix().name());
    configInfo.put(
        "refreshDelay",
        "refreshUtxoDelay=" + refreshUtxoDelay + ", refreshPoolsDelay=" + refreshPoolsDelay);
    configInfo.put(
        "mix",
        "mobile="
            + isMobile()
            + ", maxClients="
            + getMaxClients()
            + ", maxClientsPerPool="
            + getMaxClientsPerPool()
            + ", liquidityClient="
            + isLiquidityClient()
            + ", clientDelay="
            + getClientDelay()
            + ", autoTx0Delay="
            + getAutoTx0Delay()
            + ", autoTx0="
            + (isAutoTx0() ? getAutoTx0PoolId() : "false")
            + ", autoTx0Aggregate="
            + isAutoTx0Aggregate()
            + ", autoTx0FeeTarget="
            + getAutoTx0FeeTarget().name()
            + ", autoMix="
            + isAutoMix()
            + ", scode="
            + (scode != null ? ClientUtils.maskString(scode) : "null")
            + ", overspend="
            + (overspend != null ? overspend.toString() : "null"));
    configInfo.put(
        "tx0",
        "tx0MaxOutputs="
            + tx0MaxOutputs
            + ", tx0MaxRetry="
            + tx0MaxRetry
            + ", tx0StrictMode="
            + tx0StrictMode);
    configInfo.put(
        "fee", "fallback=" + getFeeFallback() + ", min=" + getFeeMin() + ", max=" + getFeeMax());
    configInfo.put("resyncOnFirstRun", Boolean.toString(resyncOnFirstRun));
    configInfo.put("autoFixPostmixIndex", Boolean.toString(postmixIndexAutoFix));
    configInfo.put("persistDelaySeconds", Integer.toString(persistDelaySeconds));
    return configInfo;
  }
}
