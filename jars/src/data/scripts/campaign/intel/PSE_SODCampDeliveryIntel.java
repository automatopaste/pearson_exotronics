package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.shared.PlayerTradeDataForSubmarket;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.PSE_SODCamp;
import data.scripts.campaign.PSE_SODCampEventListener;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class PSE_SODCampDeliveryIntel extends BaseMissionIntel {
    public static final String DELIVERED_CAMP_KEY = "$PSE_SODDeliveredToCamp";

    private static Logger log = Global.getLogger(PSE_SODCamp.class);

    private PSE_SODCamp camp;
    private FactionAPI faction;
    private float reward;
    private CommodityOnMarketAPI commodity;
    private int quantity;

    public PSE_SODCampDeliveryIntel(PSE_SODCamp camp, CommodityOnMarketAPI commodity, int quantity, float reward) {
        this.camp = camp;
        this.commodity = commodity;
        this.quantity = quantity;
        this.reward = reward;
        faction = Global.getSector().getFaction("pearson_exotronics");

        log.info("Creating SOD camp intel at market " + camp.getAssociatedMarket().getName());

        setDuration(camp.getCurrentLifetime() * 30f);

        setImportant(true);
        missionAccepted();
        setMissionState(MissionState.ACCEPTED);
        Global.getSector().addScript(this);
    }

    @Override
    public void endMission() {
        if (camp.getAssociatedMarket() == null) {
            endAfterDelay();
            return; // to fix crash for saves that are already in a bad state
        }

        endAfterDelay();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void advanceMission(float amount) {
        if (camp.getAssociatedMarket() == null) {
            return;
        }

        if (!camp.getAssociatedMarket().isInEconomy()) {
            setMissionResult(new MissionResult(0, null, null));
            setMissionState(MissionState.FAILED);
            endMission();
        }

        List<PSE_SODCamp> camps = (List<PSE_SODCamp>) Global.getSector().getPersistentData().get(PSE_SODCampEventListener.SOD_CAMPS_DATA_KEY);
        if (camps != null && !camps.contains(camp)) {
            setMissionResult(new MissionResult(0, null, null));
            setMissionState(MissionState.FAILED);
            endMission();
        }
    }

    public void performDelivery(InteractionDialogAPI dialog) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet.getCargo();

        cargo.removeItems(CargoAPI.CargoItemType.RESOURCES, commodity.getCommodity().getId(), quantity);
        cargo.getCredits().add(reward);
        applyTradeValueImpact(reward);
        AddRemoveCommodity.addCommodityLossText(commodity.getCommodity().getId(), quantity, dialog.getTextPanel());

        AddRemoveCommodity.addCreditsGainText((int) reward, dialog.getTextPanel());

        float repAmount = 0.01f * reward / 10000f;
        if (repAmount < 0.01f) repAmount = 0.01f;
        if (repAmount > 0.05f) repAmount = 0.05f;

        CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);

        ReputationActionResponsePlugin.ReputationAdjustmentResult rep;
        rep = Global.getSector().adjustPlayerReputation(
                new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, completionRep,
                        null, dialog.getTextPanel(), true, true),
                getFactionForUIColors().getId());

        setMissionResult(new MissionResult((int) reward, rep, null));
        setMissionState(MissionState.COMPLETED);
        endMission();

        Global.getSector().getMemoryWithoutUpdate().set(DELIVERED_CAMP_KEY, camp);
    }

    private void applyTradeValueImpact(float totalReward) {
        MarketAPI market = camp.getAssociatedMarket();

        boolean illegal = market.isIllegal(commodity.getCommodity().getId());

        SubmarketAPI submarket = null;
        for (SubmarketAPI curr : market.getSubmarketsCopy()) {
            if (!curr.getPlugin().isParticipatesInEconomy()) continue;

            if (illegal && curr.getPlugin().isBlackMarket()) {
                submarket = curr;
                break;
            }
            if (!illegal && curr.getPlugin().isOpenMarket()) {
                submarket = curr;
                break;
            }
        }

        if (submarket == null) return;

        PlayerTradeDataForSubmarket tradeData = SharedData.getData().getPlayerActivityTracker().getPlayerTradeData(submarket);
        CargoStackAPI stack = Global.getFactory().createCargoStack(CargoAPI.CargoItemType.RESOURCES, commodity.getCommodity().getId(), null);
        stack.setSize(quantity);
        tradeData.addToTrackedPlayerSold(stack, totalReward);
    }

    @Override
    protected MissionResult createTimeRanOutFailedResult() {
        return createAbandonedResult(true);
    }

    @Override
    protected MissionResult createAbandonedResult(boolean withPenalty) {
        if (withPenalty) {
            float repAmount = 0.01f * reward / 10000f;
            if (repAmount < 0.01f) repAmount = 0.01f;
            if (repAmount > 0.05f) repAmount = 0.05f;

            CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.WELCOMING, -repAmount, RepLevel.INHOSPITABLE);

            ReputationActionResponsePlugin.ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
                    new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_FAILURE, completionRep,
                            null, null, true, false),
                    getFactionForUIColors().getId());

            return new MissionResult(0, rep, null);
        }
        return new MissionResult();
    }

    private void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
        Color h = Misc.getHighlightColor();
        float pad = 3f;
        float opad = 10f;

        float initPad = pad;
        if (mode == ListInfoMode.IN_DESC) initPad = opad;

        Color tc = getBulletColorForMode(mode);

        bullet(info);
        boolean isUpdate = getListInfoParam() != null;

        FactionAPI faction = getFactionForUIColors();
        if (isUpdate) {
            // 3 possible updates: de-posted/expired, failed, completed
            if (isFailed() || isCancelled()) {
                return;
            } else if (isCompleted() && missionResult != null) {
                if (missionResult.payment > 0) {
                    info.addPara("%s received", initPad, tc, h, Misc.getDGSCredits(missionResult.payment));
                }

                if (missionResult.rep1 != null && missionResult.rep1.delta != 0) {
                    CoreReputationPlugin.addAdjustmentMessage(missionResult.rep1.delta, faction, null,
                            null, null, info, tc, true, 0f);
                }
            }
        } else {
            // either in small description, or in tooltip/intel list
            if (missionResult != null) {
                if (missionResult.payment > 0) {
                    info.addPara("%s received", initPad, tc, h, Misc.getDGSCredits(missionResult.payment));
                    initPad = 0f;
                }

                if (missionResult.rep1 != null && missionResult.rep1.delta != 0) {
                    CoreReputationPlugin.addAdjustmentMessage(missionResult.rep1.delta, faction, null,
                            null, null, info, tc, false, initPad);
                }
            } else {
                if (mode != ListInfoMode.IN_DESC) {
                    info.addPara("Faction: " + faction.getDisplayName(), initPad, tc,
                            faction.getBaseUIColor(),
                            faction.getDisplayName());
                    initPad = 0f;
                }

                LabelAPI label = info.addPara("%s units to " + camp.getAssociatedMarket().getName(),
                        initPad, tc, h, "" + quantity);
                label.setHighlight("" + quantity, camp.getAssociatedMarket().getName());
                label.setHighlightColors(h, camp.getAssociatedMarket().getFaction().getBaseUIColor());
                info.addPara("%s reward", 0f, tc, h, Misc.getDGSCredits(reward));
                addDays(info, "to complete", duration - elapsedDays, tc, 0f);
            }
        }

        unindent(info);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;

        FactionAPI faction = getFactionForUIColors();

        CommodityOnMarketAPI com = commodity;
        MarketAPI market = camp.getAssociatedMarket();

        info.addImages(width, 80, opad, opad * 2f,
                com.getCommodity().getIconName(),
                faction.getCrest(),
                market.getFaction().getCrest());


        String post = "";
        if (Factions.PIRATES.equals(faction.getId())) {
            post = "-affiliated";
        }

        String start = "You've";
        if (!isPosted() && !isAccepted()) start = "You had";

        if (camp.getAssociatedMarket() == null) {
            return;
        }
        LabelAPI label = info.addPara(start + " accepted " + faction.getPersonNamePrefixAOrAn() + " " +
                        faction.getPersonNamePrefix() + post + " contract to deliver a quantity of " +
                        com.getCommodity().getLowerCaseName() +
                        " to a hidden Special Operations Division camp at " + market.getName() + ", " +
                        "which is under " + market.getFaction().getPersonNamePrefix() + " control.", opad,
                faction.getBaseUIColor(), faction.getPersonNamePrefix() + post
        );

        label.setHighlight(
                faction.getPersonNamePrefix() + post,
                "Special Operations Division",
                market.getFaction().getPersonNamePrefix()
        );
        label.setHighlightColors(
                faction.getBaseUIColor(),
                Global.getSector().getFaction("pearson_division").getBaseUIColor(),
                market.getFaction().getBaseUIColor()
        );

        if (isPosted() || isAccepted()) {
            addBulletPoints(info, ListInfoMode.IN_DESC);

            info.addPara("To make the delivery, either dock at " + market.getName() + " openly or approach it without " +
                    "attracting the attention of nearby patrols.", opad
            );

            addAcceptOrAbandonButton(info, width);
        } else {
            if (isFailed() && !market.isInEconomy()) {
                info.addPara("You have failed this contract because " + market.getName() +
                        " no longer exists as a functional polity.", opad
                );
            } else {
                addGenericMissionState(info);
            }

            addBulletPoints(info, ListInfoMode.IN_DESC);
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return camp.getAssociatedMarket().getPrimaryEntity();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_TRADE);
        tags.add(getFactionForUIColors().getId());
        return tags;
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return faction;
    }

    @Override
    public void missionAccepted() {
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);

        info.addPara(getName(), c, 0f);

        addBulletPoints(info, mode);
    }

    public String getName() {
        if (isAccepted() || isPosted()) {
            return "Delivery - " + commodity.getCommodity().getName();
        }

        return "Delivery " + getPostfixForState();
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public String getIcon() {
        return commodity.getCommodity().getIconName();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }


    @Override
    public boolean canAbandonWithoutPenalty() {
        return false;
    }

    @Override
    public String getSortString() {
        return "Special Delivery";
    }


    public boolean hasEnough() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet.getCargo();

        return cargo.getCommodityQuantity(commodity.getId()) >= quantity;
    }

    public MarketAPI getDestination() {
        return camp.getAssociatedMarket();
    }

    public String getCommodityId() {
        return commodity.getCommodity().getId();
    }

    public int getQuantity() {
        return quantity;
    }
}
