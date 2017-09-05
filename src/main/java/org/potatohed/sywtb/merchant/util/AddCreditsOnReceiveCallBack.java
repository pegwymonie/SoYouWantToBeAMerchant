package org.potatohed.sywtb.merchant.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OnMessageDeliveryScript;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;

public class AddCreditsOnReceiveCallBack implements OnMessageDeliveryScript {

  private Long changeAmount;

  public AddCreditsOnReceiveCallBack(Long changeAmount) {
    this.changeAmount = changeAmount;
  }

  @Override
  public void beforeDelivery(CommMessageAPI message) {
    Global.getSector().getPlayerFleet().getCargo().getCredits().add(changeAmount);
  }

  @Override public boolean shouldDeliver() {
    return true;
  }
}
