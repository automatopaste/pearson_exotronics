package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.apache.log4j.Logger;

import java.util.*;

public class PSE_SODCampEventListener extends BaseCampaignEventListener {
    public static Logger log = Global.getLogger(PSE_SODCampEventListener.class);

    public static final String SOD_CAMPS_DATA_KEY = "PSE_division_camps";
    private static final List<String> suitableSODFactions = new ArrayList<>();
    static {
        suitableSODFactions.add(Factions.INDEPENDENT);
        suitableSODFactions.add(Factions.PERSEAN);
    }

    private static final int MAX_NUM_SOD_MARKETS = Global.getSettings().getInt("PSE_maxNumSODMarkets");
    private static final float NEW_CAMP_PER_TICK_CHANCE = 0.4f;
    private static final String SOD_SUBMARKET_ID = "PSE_sod_submarket";

    public PSE_SODCampEventListener(boolean newGame) {
        super(false);

        if (newGame) {
            List<PSE_SODCamp> camps = new ArrayList<>();
            PSE_SODCamp camp = createSODCamp();
            if (camp != null) {
                camps.add(camp);
            }

            Map<String, Object> data = Global.getSector().getPersistentData();
            data.put(SOD_CAMPS_DATA_KEY, camps);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reportEconomyMonthEnd() {
        log.info("Starting SOD Economy Listener...");

        Map<String, Object> data = Global.getSector().getPersistentData();
        if (!data.containsKey(SOD_CAMPS_DATA_KEY)) {
            data.put(SOD_CAMPS_DATA_KEY, new ArrayList<PSE_SODCamp>());
        }

        List<PSE_SODCamp> camps = (List<PSE_SODCamp>) Global.getSector().getPersistentData().get(SOD_CAMPS_DATA_KEY);
        List<PSE_SODCamp> temp = new ArrayList<>();
        for (PSE_SODCamp camp : camps) {
            camp.setCurrentLifetime(camp.getCurrentLifetime() - 1);
            if (camp.getCurrentLifetime() <= 0) {
                removeSODCampEffects(camp);
                temp.add(camp);
            }
        }
        for (PSE_SODCamp camp : temp) {
            if (camp.getAssociatedMarket() == null) {
                log.info("Removed SOD camp with null market");
            } else {
                log.info("Removed SOD camp at market " + camp.getAssociatedMarket().getName());
            }
            camps.remove(camp);
        }

        boolean createNewCampThisTick = camps.size() < MAX_NUM_SOD_MARKETS && new Random(this.hashCode()).nextFloat() < NEW_CAMP_PER_TICK_CHANCE;
        if (createNewCampThisTick) {
            log.info("Attempting to create SOD camp...");

            PSE_SODCamp camp = createSODCamp();
            if (camp != null) {
                camps.add(camp);
            }
        }

        log.info("Economy ticked with " + camps.size() + " SOD camps in the sector");
        for (PSE_SODCamp camp : camps) {
            log.info("SOD camp at " + camp.getAssociatedMarket().getName() + ", is discovered: " + camp.isDiscovered());
        }
        Global.getSector().getPersistentData().put(SOD_CAMPS_DATA_KEY, camps);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        List<PSE_SODCamp> camps = (List<PSE_SODCamp>) Global.getSector().getPersistentData().get(SOD_CAMPS_DATA_KEY);
        if (camps == null) {
            Global.getSector().getPersistentData().put(SOD_CAMPS_DATA_KEY, new ArrayList<PSE_SODCamp>());
            return;
        }

        for (PSE_SODCamp camp : camps) {
            if (camp.getAssociatedMarket().equals(market)) {
                camp.setDiscovered(true);

                log.info("Discovered SOD camp at market " + market.getName());
            }
        }
    }

    private PSE_SODCamp createSODCamp() {
        MarketAPI market = pickSuitableMarketForSOD();
        if (market == null) {
            log.info("Failed to create SOD camp because no suitable market was found");

            return null;
        }
        int lifetime = 16;

        log.info("Created SOD camp at market " + market.getName() + ", with lifetime " + lifetime);

        market.addSubmarket(SOD_SUBMARKET_ID);

        return new PSE_SODCamp(market, lifetime);
    }

    private void removeSODCampEffects(PSE_SODCamp camp) {
        MarketAPI market = camp.getAssociatedMarket();

        if (market.hasSubmarket(SOD_SUBMARKET_ID)) {
            market.removeSubmarket(SOD_SUBMARKET_ID);
        }
    }

    private MarketAPI pickSuitableMarketForSOD() {
        FactionAPI pearson = Global.getSector().getFaction("pearson_exotronics");
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!suitableSODFactions.contains(faction.getId()) && pearson.getRelationship(faction.getId()) >= 0.1f) {
                suitableSODFactions.add(faction.getId());
            }
        }

        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        List<MarketAPI> candidates = new ArrayList<>();
        for (MarketAPI market : markets) {
            if (
                    market.getPlanetEntity() != null &&
                    suitableSODFactions.contains(market.getFactionId()) &&
                    !market.getFactionId().contentEquals(pearson.getId()) &&
                    market.getSubmarketsCopy().size() <= 3 &&
                    !market.isHidden()
            ) {
                candidates.add(market);
            }
        }

        if (!candidates.isEmpty()) {
            Random r = new Random(pearson.hashCode());
            return candidates.get(r.nextInt(candidates.size()));
        }
        return null;
    }
}
