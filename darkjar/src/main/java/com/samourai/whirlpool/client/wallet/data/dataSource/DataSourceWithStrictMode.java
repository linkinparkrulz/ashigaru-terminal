package com.samourai.whirlpool.client.wallet.data.dataSource;

import java.util.List;

public interface DataSourceWithStrictMode {

  void pushTx(String txHex, List<Integer> strictModeVouts) throws Exception;
}
