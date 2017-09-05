package org.potatohed.sywtb.merchant.abilities;

import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.potatohed.sectorunit.MockedGlobal;

import static org.mockito.Mockito.verify;
import static org.potatohed.sywtb.merchant.util.Internals.InstancedInterals;
import static org.potatohed.sywtb.merchant.util.ReflectionUtils.reflectiveSet;
import static org.potatohed.sywtb.merchant.util.Targets.ValidTargets;

public class RemoteMarketScanTest {

    @ClassRule
    public static MockedGlobal mockedGlobal = new MockedGlobal();

    private final VerificationMode ONCE = Mockito.times(1);
    private final CampaignEventTarget EXPECTED_TARGET = ValidTargets().PLAYER;
    private final CampaignEventManagerAPI EVENT_MANAGER = InstancedInterals().EVENT_MANAGER;
    private final String EVENT_TARGET = "merchant_market_scan";
    RemoteMarketScan systemUnderTest;

    @Before
    public void setup() {
        systemUnderTest = new RemoteMarketScan();
    }


    @Test
    public void activateImpl() throws Exception {
        systemUnderTest.activateImpl();
        verify(EVENT_MANAGER, ONCE).startEvent(EXPECTED_TARGET, EVENT_TARGET, null);

    }

    @Test
    public void cleanupImpl() throws Exception {
        CampaignEventPlugin EXPECTED_EVENT = Mockito.mock(CampaignEventPlugin.class);
        reflectiveSet(systemUnderTest, "event", EXPECTED_EVENT);

        systemUnderTest.cleanupImpl();
        verify(EVENT_MANAGER, ONCE).endEvent(EXPECTED_EVENT);
    }

    @Test
    public void applyEffect() {
        systemUnderTest.applyEffect(0,0);
    }

    @Test
    public void deactivateImpl() {
        systemUnderTest.deactivateImpl();
    }

}
