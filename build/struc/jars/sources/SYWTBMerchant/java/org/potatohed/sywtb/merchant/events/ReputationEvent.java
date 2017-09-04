/*
 * MIT License
 *
 * Copyright (c)  2017 pegwymonie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.potatohed.sywtb.merchant.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.TransactionLineItem;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketDemandAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import org.potatohed.sywtb.merchant.util.AdjustRelationOnReceiveCallBack;
import org.potatohed.sywtb.merchant.util.DemandChangeDetails;
import org.potatohed.sywtb.merchant.util.DemandChangeDetails.DemandChange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.fs.starfarer.api.campaign.PlayerMarketTransaction.LineItemType.SOLD;
import static java.lang.Math.log;
import static java.lang.Math.round;

public class ReputationEvent extends BaseEventPlugin {

    /**
     * Used to prevent effect based gains unless the normalizedValue is greater than this number.
     * ~3125(5^5) credits worth of trade.
     */
    private static final Integer TRADE_EFFECT_OFFSET = 4;
    private static final String EVENT_NAME = "Reputation Event";

    //State Objects for Event Callbacks
    private static String lastMarket;
    private static String lastFaction;
    private static String lastChange;
    private static Set<String> lastCommodities;

    public ReputationEvent() {
    }

    private static double log5(float num) {
        return log(num) / log(5);
    }

    private static float demandImpact(float qtySold, float demandTotal) {
        return qtySold / demandTotal;
    }

    private static boolean isSatisfied(float demandValue) {
        return demandValue >= 1;
    }

    private static boolean isNotSatisfied(float demandValue) {
        return !isSatisfied(demandValue);
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        MarketAPI market = trackMarket(transaction);
        Long totalReward = 0L;
        for (TransactionLineItem lineItem : transaction.getLineItems()) {
            if (wasSold(lineItem) && isResource(lineItem) &&!recentlySold(lineItem))
                totalReward +=
                        rewardAmountFor(demandChangeDataFor(lineItem, demandFrom(market, lineItem)), lineItem);
        }
        lastChange = totalReward.toString();
        processReward(totalReward, transaction.getSubmarket());
    }

    private boolean isResource(TransactionLineItem lineItem){
        return lineItem.getCargoType() == CargoAPI.CargoItemType.RESOURCES;
    }

    private boolean wasSold(TransactionLineItem lineItem){
        return lineItem.getItemType() == SOLD;
    }

    private MarketAPI trackMarket(PlayerMarketTransaction transaction) {
        if (lastMarket == null || !lastMarket.equalsIgnoreCase(submarketName(transaction))) {
            lastMarket = submarketName(transaction);
            lastFaction = submarketFaction(transaction);
            lastCommodities = new HashSet<>();
        }
        return transaction.getMarket();
    }

    private String submarketFaction(PlayerMarketTransaction transaction) {
        return transaction.getSubmarket().getFaction().getDisplayName();
    }

    private String submarketName(PlayerMarketTransaction transaction) {
        return transaction.getSubmarket().getName();
    }

    private boolean recentlySold(TransactionLineItem lineItem) {
        if (lastCommodities.contains(lineItem.getId()))
            return true;
        lastCommodities.add(lineItem.getId());
        return false;
    }

    private Long rewardAmountFor(DemandChangeDetails demandChangeData, TransactionLineItem lineItem) {
        Long rewardAmount = 0L;
        switch (demandChangeData.getType()) {
            case SATISFIED:
                rewardAmount += getRepRewardForSatisfying(lineItem);
            case DECREASED:
                rewardAmount += getRepRewardForEffect(demandChangeData.getChangeAmount(), lineItem);
                break;
            case OVERSUPPLY:
                break;
            case NONE:
                break;
        }
        return rewardAmount;
    }

    @Override
    public Map<String, String> getTokenReplacements() {
        return new HashMap<String, String>() {{
            put("$faction", lastFaction);
            put("$market", lastMarket);
            put("$change", lastChange);
        }};
    }

    private MarketDemandAPI demandFrom(MarketAPI market, TransactionLineItem lineItem) {
        return market.getDemand(lineItem.getId());
    }

    private void processReward(Long rewardAmount, SubmarketAPI submarketAPI) {
        if (rewardAmount > 0)
            fireEvent(rewardAmount, submarketAPI.getFaction());
    }

    private void fireEvent(Long rewardAmount, FactionAPI faction) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Global.getSector()
                .reportEventStage(
                        this,
                        "merchant_rep_gain_demand",
                        playerFleet,
                        MessagePriority.DELIVER_IMMEDIATELY,
                        new AdjustRelationOnReceiveCallBack(faction, rewardAmount));
    }

    /**
     * Uses log to give a gradual increase to the reward amount. Rewards are offset to discourage munchkins
     */
    private long getRepRewardForEffect(Float changeAmount, TransactionLineItem lineItem) {
        float bonus = 1f + clampToOne(changeAmount); //Max 200% Bonus
        float tradeValue = lineItem.getPrice() * bonus;
        long normalizedValue = round(log5(tradeValue));
        if (normalizedValue <= TRADE_EFFECT_OFFSET) {
            return 0;
        } else {
            return normalizedValue - TRADE_EFFECT_OFFSET;
        }
    }

    private float clampToOne(Float num) {
        return num < 1 ? num : 1;
    }

    /**
     * Uses the natual log to give a gradual increase to the reward amount. High prices commodities give more
     * upfront reward for satisfying.
     */
    private Long getRepRewardForSatisfying(TransactionLineItem lineItem) {
        Float basePrice = Global.getSector().getEconomy().getCommoditySpec(lineItem.getId()).getBasePrice();
        return round(log(basePrice));
    }

    private DemandChangeDetails demandChangeDataFor(TransactionLineItem lineItem, MarketDemandAPI demand) {
        Float currDemand = demand.getFractionMet();
        Float demandImpact = demandImpact(lineItem.getQuantity(), demand.getDemandValue());
        Float prevDemand = currDemand - demandImpact;

        if (isNotSatisfied(currDemand))
            return new DemandChangeDetails(DemandChange.DECREASED, demandImpact);
        else if (isNotSatisfied(prevDemand) && isSatisfied(currDemand)) {
            return new DemandChangeDetails(DemandChange.SATISFIED, demandImpact);
        } else if (isInconsequential(demandImpact)) {
            return new DemandChangeDetails(DemandChange.SATISFIED, demandImpact);
        } else { // Implied -> demand.getFractionMet() > 1
            return new DemandChangeDetails(DemandChange.SATISFIED, demandImpact);
        }
    }

    private boolean isInconsequential(Float demandImpact) {
        return demandImpact < 0.01F; //Less than one percent
    }

}
