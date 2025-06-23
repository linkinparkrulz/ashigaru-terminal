package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.samourai.wallet.api.backend.MinerFee;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.whirlpool.client.event.MinerFeeChangeEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicMinerFeeSupplier extends BasicSupplier<MinerFee> implements MinerFeeSupplier {
  private static final Logger log = LoggerFactory.getLogger(BasicMinerFeeSupplier.class);

  protected int feeMin;
  protected int feeMax;

  public BasicMinerFeeSupplier(int feeMin, int feeMax) {
    super(log);
    this.feeMin = feeMin;
    this.feeMax = feeMax;
  }

  public void setValue(int value) throws Exception {
    MinerFee minerFee = mockMinerFee(value);
    setValue(minerFee);
  }

  @Override
  public void setValue(MinerFee value) throws Exception {
    // validate
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      if (value.get(minerFeeTarget) <= 0) {
        throw new Exception("Invalid MinerFee[" + minerFeeTarget + "]");
      }
    }

    MinerFee oldValue = getValue();

    // set
    super.setValue(value);

    // notify minerFee changes
    if (oldValue == null || !oldValue.equals(value)) {
      WhirlpoolEventService.getInstance().post(new MinerFeeChangeEvent(value));
    }
  }

  protected static MinerFee mockMinerFee(int feeValue) {
    Map<String, Integer> feeResponse = new LinkedHashMap<String, Integer>();
    for (MinerFeeTarget minerFeeTarget : MinerFeeTarget.values()) {
      feeResponse.put(minerFeeTarget.getValue(), feeValue);
    }
    return new MinerFee(feeResponse);
  }

  @Override
  public int getFee(MinerFeeTarget feeTarget) {
    // get fee or fallback
    int fee = getValue().get(feeTarget);

    // check min
    if (fee < feeMin) {
      log.error("Fee/b too low (" + feeTarget + "): " + fee + " => " + feeMin);
      fee = feeMin;
    }

    // check max
    if (fee > feeMax) {
      log.error("Fee/b too high (" + feeTarget + "): " + fee + " => " + feeMax);
      fee = feeMax;
    }
    return fee;
  }
}
