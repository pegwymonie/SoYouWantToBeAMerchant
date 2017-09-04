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
import com.fs.starfarer.api.campaign.BaseOnMessageDeliveryScript;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.impl.campaign.events.PriceUpdate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RemoteMarketScanEvent extends BaseEventPlugin {

    private List<PriceUpdatePlugin> possibleUpdates = new LinkedList<>();

    @Override
    public void startEvent() {
        for (final MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
            possibleUpdates = extractUpdates(marketAPI);
            Global.getSector().reportEventStage(this, "merchant_prices_remote", Global.getSector().getPlayerFleet(), MessagePriority.DELIVER_IMMEDIATELY, new BaseOnMessageDeliveryScript() {
                public void beforeDelivery(CommMessageAPI message) {
                    message.setAddToIntelTab(true);
                    message.setShowInCampaignList(false);
                    if (marketAPI.getStarSystem() != null) message.setStarSystemId(marketAPI.getStarSystem().getId());
                    message.setLocInHyper(marketAPI.getLocationInHyperspace());
                    message.setPriceUpdates(possibleUpdates);
                }
            });
        }
    }

    @Override
    public void advance(float amount) {

    }

    private List<PriceUpdatePlugin> extractUpdates(MarketAPI marketAPI) {
        List<PriceUpdatePlugin> updates = new ArrayList<>();
        for (CommodityOnMarketAPI commodityOnMarketAPI : marketAPI.getAllCommodities()) {
            PriceUpdate priceUpdate = new PriceUpdate(commodityOnMarketAPI);
            if (shouldShow(priceUpdate)) updates.add(priceUpdate);
        }
        return updates;
    }

    private boolean shouldShow(PriceUpdate priceUpdate) {
        float ava = priceUpdate.getAvailable();
        float dem = priceUpdate.getDemand();
        switch (priceUpdate.getType()) {
            case CHEAP:
                return hasEnough(ava);
            case NORMAL:
                return hasAlot(ava);
            case EXPENSIVE:
                return hasEnough(dem);

            default:
                return false;
        }
    }

    /**
     * This was keying off the players cargo capacity, but that was really making it useless late game.
     */
    private boolean hasEnough(float qty) {
        return qty >= 100;
    }

    private boolean hasAlot(float qty) {
        return qty >= 1000;
    }

    @Override
    public List<PriceUpdatePlugin> getPriceUpdates() {
        return possibleUpdates;
    }

    @Override
    public CampaignEventCategory getEventCategory() {
        return CampaignEventCategory.DO_NOT_SHOW_IN_MESSAGE_FILTER;
    }

    public Map<String, String> getTokenReplacements() {
        Map<String, String> map = super.getTokenReplacements();
        map.put("$fromSystem", "hyperspace");

        List<PriceUpdatePlugin> updates = getPriceUpdates();
        if (updates != null && !updates.isEmpty()) {
            String priceList = "Price information updated for: ";
            for (PriceUpdatePlugin update : updates) {
                CommodityOnMarketAPI com = update.getCommodity();
                priceList += com.getCommodity().getName() + " (" + com.getMarket().getName() + "), ";
            }
            priceList = priceList.substring(0, priceList.length() - 2);
            priceList += ".";
            map.put("$priceList", priceList);
        }
        return map;
    }
}
