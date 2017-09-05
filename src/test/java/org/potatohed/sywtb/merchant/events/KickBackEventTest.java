package org.potatohed.sywtb.merchant.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.OnMessageDeliveryScript;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.TransactionLineItem;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.potatohed.sectorunit.MockedGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.potatohed.sywtb.merchant.util.ReflectionUtils.reflectiveSet;

@RunWith(Theories.class)
public class KickBackEventTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(KickBackEventTest.class);
  private static final Long VALID_LAST_REFUND = 100L;
  private static final FactionAPI VALID_LAST_FACTION = Mockito.mock(FactionAPI.class);
  private static final MarketAPI VALID_LAST_MARKET = Mockito.mock(MarketAPI.class);
  private static final String VALID_NAME = "Valid_Name";

  @ClassRule
  public static MockedGlobal mockedGlobal = new MockedGlobal();

  @DataPoints
  public static List<Float> AMOUNTS = Arrays.asList(0f, 10f, 100f, 1000f, 10000f, 100000f);

  @DataPoints
  public static List<RepLevel> REPS = Arrays.asList(RepLevel.values());

  @DataPoints
  public static List<CoreUITradeMode> MODES = Arrays.asList(CoreUITradeMode.values());

  //private final CampaignEventManagerAPI EVENT_MANAGER = InstancedInterals().EVENT_MANAGER;
  private final VerificationMode ONCE = Mockito.times(1);

  private KickBackEvent systemUnderTest;

  @Before
  public void setup() {
    systemUnderTest = new KickBackEvent();
    Mockito.reset(Global.getSector());
  }

  @Test
  @Theory
  public void reportPlayerMarketTransaction(Float tariff, RepLevel level, CoreUITradeMode mode) {
    PlayerMarketTransaction mockedTransaction = transactionFrom(tariff, level, mode);

    systemUnderTest.reportPlayerMarketTransaction(mockedTransaction);
    if (shouldRefund(tariff, level, mode)) {
      verify(Global.getSector())
          .reportEventStage(
              any(CampaignEventPlugin.class),
              any(String.class),
              any(SectorEntityToken.class),
              any(MessagePriority.class),
              any(OnMessageDeliveryScript.class));
      LOGGER.info("Event generated refund. Tariff: {}  RepLevel: {} Mode: {}", tariff.toString(),
          level.getDisplayName(), mode.toString());
    } else {
      verifyZeroInteractions(Global.getSector());
      LOGGER.debug("Event did not generate refund. Tariff: {}  RepLevel: {} Mode: {}",
          tariff.toString(), level.getDisplayName(), mode.toString());
    }
  }

  @Test
  public void verifyTokens(){
    when(VALID_LAST_FACTION.getDisplayName()).thenReturn(VALID_NAME);
    when(VALID_LAST_MARKET.getName()).thenReturn(VALID_NAME);
    reflectiveSet(systemUnderTest, "lastRefundAmount", VALID_LAST_REFUND);
    reflectiveSet(systemUnderTest, "lastFaction", VALID_LAST_FACTION);
    reflectiveSet(systemUnderTest, "lastMarket", VALID_LAST_MARKET);

    Map<String, String> tokens =systemUnderTest.getTokenReplacements();

    LOGGER.info("{}", tokens);
    assertThat(tokens, hasEntry("$credits", VALID_LAST_REFUND.toString()));
    assertThat(tokens, hasEntry("$market", VALID_NAME));
    assertThat(tokens, hasEntry("$faction", VALID_NAME));

  }


  private boolean shouldRefund(Float tariff, RepLevel level, CoreUITradeMode mode) {
    if (mode == CoreUITradeMode.SNEAK) { return false; }
    switch (level) {
    case COOPERATIVE:
      return tariff > 533;
    case FRIENDLY:
      return tariff > 800;
    case WELCOMING:
      return tariff > 1600;
    case FAVORABLE:
      return tariff > 4000;
    default:
      return false;
    }
  }

  private PlayerMarketTransaction transactionFrom(Float tariff,
                                                  final RepLevel level,
                                                  CoreUITradeMode mode) {
    PlayerMarketTransaction transaction = Mockito
        .mock(PlayerMarketTransaction.class, Answers.RETURNS_DEEP_STUBS);
    when(transaction.getTradeMode()).thenReturn(mode);
    when(transaction.getMarket().getFaction().getRelToPlayer().getLevel()).thenReturn(level);
    when(transaction.getMarket().getFaction().getRelToPlayer().isAtWorst(any(RepLevel.class)))
        .thenAnswer(new Answer<Boolean>() {
          @Override public Boolean answer(InvocationOnMock invocation) throws Throwable {
            return level.isAtWorst((RepLevel) invocation.getArgument(0));
          }
        });
    TransactionLineItem item = Mockito.mock(TransactionLineItem.class, Answers.RETURNS_DEEP_STUBS);
    when(item.getTariff()).thenReturn(tariff);

    when(transaction.getLineItems()).thenReturn(Collections.singletonList(item));

    return transaction;
  }

}
