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
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import org.potatohed.sywtb.merchant.util.TariffDetails;

import java.util.HashMap;
import java.util.Map;

public class KickBackEvent extends BaseEventPlugin {

    private Long lastRefundAmount;
    private FactionAPI lastFaction;
    private MarketAPI lastMarket;

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        MarketAPI market = transaction.getMarket();
        RelationshipAPI factionStanding = market.getFaction().getRelToPlayer();

        if (isFavorable(factionStanding) && notSneaking(transaction)) {
            final Long refund = refundFrom(tariffDetailsOf(transaction), factionStanding);
            if (refund > 400) {
                issueRefund(market, refund);
            }
        }
    }

    private TariffDetails tariffDetailsOf(PlayerMarketTransaction transaction) {
        long tariffPaid = 0L;
        long demandTariff = 0L;
        for (PlayerMarketTransaction.TransactionLineItem transactionLineItem : transaction.getLineItems()) {
            tariffPaid += transactionLineItem.getTariff();
            demandTariff += transactionLineItem.getDemandTariff();
        }
        return new TariffDetails(tariffPaid, demandTariff);
    }

    private Long refundFrom(TariffDetails details, RelationshipAPI factionStanding) {
        Long regularTariff = details.getRegularTariff();
        Long demandTariff = details.getDemandTariff();
        Double refundAmount = 0D;
        switch (factionStanding.getLevel()) {
            // 75% of demand tariff + 75% of demand tariff
            case COOPERATIVE:
                refundAmount += (regularTariff * .25) + (demandTariff * .25);
                // 50% of demand tariff + 50% of normal tariff
            case FRIENDLY:
                refundAmount += (regularTariff * .25) + (demandTariff * .25);
                // 25% of demand tariff + 25% of normal tariff
            case WELCOMING:
                refundAmount += (regularTariff * .25) + (demandTariff * .15);
                // 10 % of demand tariff
            case FAVORABLE:
                refundAmount += (demandTariff * .10);
        }
        return refundAmount.longValue();
    }

    private void issueRefund(MarketAPI marketAPI, final Long refund) {
        lastRefundAmount = refund;
        lastFaction = marketAPI.getFaction();
        lastMarket = marketAPI;
        Global.getSector()
                .reportEventStage(
                        this,
                        "merchant_tariff_refund",
                        marketAPI.getPrimaryEntity(),
                        MessagePriority.DELIVER_IMMEDIATELY,
                        new BaseOnMessageDeliveryScript() {
                            @Override
                            public void beforeDelivery(CommMessageAPI message) {
                                Global.getSector().getPlayerFleet().getCargo().getCredits().add(refund);
                            }
                        });
    }

    private boolean notSneaking(PlayerMarketTransaction transaction) {
        return transaction.getTradeMode() == CampaignUIAPI.CoreUITradeMode.OPEN;
    }

    private boolean isFavorable(RelationshipAPI factionStanding) {
        return factionStanding.isAtWorst(RepLevel.FAVORABLE);
    }

    @Override
    public Map<String, String> getTokenReplacements() {
        return new HashMap<String, String>() {{
            put("$credits", lastRefundAmount.toString());
            put("$faction", lastFaction.getDisplayName());
            put("$market", lastMarket.getName());
        }};
    }
}
