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

public class PSE_SODCampSubmarketPlugin extends BaseSubmarketPlugin {
    private static List<String> SHIP_VARIANT_ID_LIST = new ArrayList<>();
    static {
        SHIP_VARIANT_ID_LIST.add("PSE_leyland_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_kingston_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_serrano_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_denmark_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_richmond_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_kiruna_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_eyre_sod_Hull");
        SHIP_VARIANT_ID_LIST.add("PSE_cassius_sod_Hull");
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

            //clears commodities if they got there somehow
            getCargo().clear();
            //clears ships
            getCargo().getMothballedShips().clear();

            for (String variant : SHIP_VARIANT_ID_LIST) {
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
        return SHIP_VARIANT_ID_LIST.size();
    }

    public static int getApproximateStockpileLimit(CommodityOnMarketAPI com) {
        return SHIP_VARIANT_ID_LIST.size();
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
