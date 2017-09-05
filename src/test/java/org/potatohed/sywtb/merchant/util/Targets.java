package org.potatohed.sywtb.merchant.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;

public class Targets {

    private Targets(){
        PLAYER = new CampaignEventTarget(Global.getSector().getPlayerPerson());
    }

    /**
     * Returns a targets Object with valid object targets.
     */
    //Using factory pattern because we do not want our values instanced before the initial mocking has occurred.
    public static Targets ValidTargets(){
        return new Targets();
    }

    public final CampaignEventTarget PLAYER;
}
