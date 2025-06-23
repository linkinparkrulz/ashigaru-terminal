package com.samourai.whirlpool.client.wallet.data.dataSource;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;

public interface DataSourceFactory {

  DataSource createDataSource(
          WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister)
      throws Exception;
}
