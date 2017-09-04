package org.potatohed.sywtb.merchant.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;

public class Internals {

    private Internals(){
        EVENT_MANAGER = Global.getSector().getEventManager();
    }

    public static Internals InstancedInterals() {
        return new Internals();
    }

    public final CampaignEventManagerAPI EVENT_MANAGER;

}
