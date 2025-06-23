package com.samourai.whirlpool.client.wallet.data.dataPersister;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public interface DataPersisterFactory {

  DataPersister createDataPersister(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w)
      throws Exception;
}
