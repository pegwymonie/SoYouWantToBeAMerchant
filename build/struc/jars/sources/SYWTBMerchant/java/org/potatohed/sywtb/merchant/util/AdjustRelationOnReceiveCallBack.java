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

package org.potatohed.sywtb.merchant.util;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.OnMessageDeliveryScript;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;

public class AdjustRelationOnReceiveCallBack implements OnMessageDeliveryScript {

    private FactionAPI faction;
    private Long changeAmount;

    public AdjustRelationOnReceiveCallBack(FactionAPI faction, Long changeAmount) {
        this.faction = faction;
        this.changeAmount = changeAmount;
    }

    @Override
    public void beforeDelivery(CommMessageAPI message) {
        faction.getRelToPlayer()
                .adjustRelationship(toPrecent(changeAmount), RepLevel.COOPERATIVE);
    }

    @Override
    public boolean shouldDeliver() {
        return true;
    }

    private float toPrecent(Long rewardAmount) {
        return rewardAmount.floatValue() / 100;
    }
}
