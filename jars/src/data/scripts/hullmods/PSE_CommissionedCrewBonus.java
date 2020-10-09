package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.PSEDrone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PSE_CommissionedCrewBonus extends BaseHullMod {
    private static final float DRONE_ARMOUR_RATING_FLAT_BONUS = 50f;

    private static final Map<ShipAPI.HullSize, Float> ECM_REDUCTION_PERCENT = new HashMap<>();
    static {
        ECM_REDUCTION_PERCENT.put(ShipAPI.HullSize.CAPITAL_SHIP, -25f);
        ECM_REDUCTION_PERCENT.put(ShipAPI.HullSize.CRUISER, -30f);
        ECM_REDUCTION_PERCENT.put(ShipAPI.HullSize.DESTROYER, -40f);
        ECM_REDUCTION_PERCENT.put(ShipAPI.HullSize.FRIGATE, -50f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "" + (int) DRONE_ARMOUR_RATING_FLAT_BONUS;
        }
        if (index == 1) {
            return "" + -ECM_REDUCTION_PERCENT.get(hullSize);
        }
        return null;
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        if (fighter.getHullSpec().getBaseHullId().startsWith("PSE_")) {
            fighter.getMutableStats().getArmorBonus().modifyFlat(this.getClass().toString(), DRONE_ARMOUR_RATING_FLAT_BONUS);
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifyPercent(id, ECM_REDUCTION_PERCENT.get(hullSize));
    }


    @SuppressWarnings("unchecked")
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (ship == null || !ship.isAlive()) {
            return;
        }

        String key = "PSE_DroneList_" + ship.hashCode();

        ArrayList<PSEDrone> list = (ArrayList<PSEDrone>) engine.getCustomData().get(key);

        if (list != null && !list.isEmpty()) {
            for (PSEDrone drone : list) {
                drone.getMutableStats().getEffectiveArmorBonus().modifyFlat(this.getClass().toString(), DRONE_ARMOUR_RATING_FLAT_BONUS);
            }
        }
    }
}
