package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashMap;
import java.util.Map;

public class PSE_SpecialOperationsDivision extends BaseHullMod {
    private static final float GUIDANCE_IMPROVEMENT = 0.5f;
    private static final float CR_DECAY_MULT = 0.9f;
    private static final float ECCM_CHANCE = 0.25f;
    private static final float SENSOR_BONUS_PERCENT = 15f;

    private static Map<ShipAPI.HullSize, Float> mag = new HashMap<>();
    static {
        mag.put(ShipAPI.HullSize.FRIGATE, 1f);
        mag.put(ShipAPI.HullSize.DESTROYER, 1f);
        mag.put(ShipAPI.HullSize.CRUISER, 2f);
        mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 2f);
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getCRLossPerSecondPercent().modifyMult(id, CR_DECAY_MULT);
        stats.getMissileGuidance().modifyFlat(id, GUIDANCE_IMPROVEMENT);
        stats.getMissileGuidance().modifyFlat(id, ECCM_CHANCE);

        stats.getSensorProfile().modifyPercent(id, -SENSOR_BONUS_PERCENT);
        stats.getSensorStrength().modifyPercent(id, SENSOR_BONUS_PERCENT);

        stats.getDynamic().getStat(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, mag.get(hullSize));
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) (ECCM_CHANCE * 100f) + "%";
        if (index == 1) return "" + (int) ((1f - CR_DECAY_MULT) * 100f) + "%";
        if (index == 2) return "" + (int) (SENSOR_BONUS_PERCENT) + "%";
        return null;
    }
}