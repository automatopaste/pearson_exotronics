package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public class PSE_CustomSubmarketPlugin extends BaseSubmarketPlugin {
    private static List<String> SHIP_HULL_ID_LIST = new ArrayList<>();
    static {
        SHIP_HULL_ID_LIST.add("paragon");
    }

    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        //checks for an interval so that it doesn't constantly replenish stock
        if (okToUpdateShipsAndWeapons()) {
            //used for the interval checker
            sinceSWUpdate = 0f;

            getCargo().clear();

            for (String id : SHIP_HULL_ID_LIST) {
                //creates empty hull variant
                String variant = id + "_Hull";
                addShip(variant, false, 1f);
            }
        }

        getCargo().sort();
    }

    public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
        return false;
    }

    @Override
    public PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
        return PlayerEconomyImpactMode.NONE;
    }

    @Override
    public int getStockpileLimit(CommodityOnMarketAPI com) {
        return SHIP_HULL_ID_LIST.size();
    }

    public static int getApproximateStockpileLimit(CommodityOnMarketAPI com) {
        return SHIP_HULL_ID_LIST.size();
    }

    @Override
    public boolean isOpenMarket() {
        return false;
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    //prevent commodity selling
    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return true;
    }
    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "Cannot sell to this submarket";
    }

    //prevent ship selling
    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI fleetMember, TransferAction action) {
        return !action.equals(TransferAction.PLAYER_BUY);
    }
    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action)
    {
        return "Cannot sell to this submarket";
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        if (ui.getTradeMode() == CampaignUIAPI.CoreUITradeMode.SNEAK) {
            return "Requires: proper docking authorization (transponder on)";
        }
        return super.getTooltipAppendix(ui);
    }

    @Override
    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        if (ui.getTradeMode() == CampaignUIAPI.CoreUITradeMode.SNEAK) {
            String appendix = getTooltipAppendix(ui);
            if (appendix == null) return null;

            Highlights h = new Highlights();
            h.setText(appendix);
            h.setColors(Misc.getNegativeHighlightColor());
            return h;
        }
        return super.getTooltipAppendixHighlights(ui);
    }
}
