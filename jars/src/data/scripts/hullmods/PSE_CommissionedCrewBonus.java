package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

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
            return "" + (int) -ECM_REDUCTION_PERCENT.get(ShipAPI.HullSize.FRIGATE) + "%";
        }
        if (index == 2) {
            return "" + (int) -ECM_REDUCTION_PERCENT.get(ShipAPI.HullSize.DESTROYER) + "%";
        }
        if (index == 3) {
            return "" + (int) -ECM_REDUCTION_PERCENT.get(ShipAPI.HullSize.CRUISER) + "%";
        }
        if (index == 4) {
            return "" + (int) -ECM_REDUCTION_PERCENT.get(ShipAPI.HullSize.CAPITAL_SHIP) + "%";
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

        float targetHP = 7500f;

        float hitpoints = 0f;
        if (stats.getEntity() instanceof  ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            hitpoints = ship.getHullSpec().getHitpoints();
        }

        float diff = targetHP - hitpoints;
        stats.getHullBonus().modifyPercent(id, 40f);

        stats.getHullBonus().computeEffective(1f);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
//        CombatEngineAPI engine = Global.getCombatEngine();
//
//        if (ship == null || !ship.isAlive()) {
//            return;
//        }
//
//        String key = "PSE_DroneList_" + ship.hashCode();
//
//        ArrayList<PSE_Drone> list = (ArrayList<PSE_Drone>) engine.getCustomData().get(key);
//
//        if (list != null && !list.isEmpty()) {
//            for (PSE_Drone drone : list) {
//                drone.getMutableStats().getEffectiveArmorBonus().modifyFlat(this.getClass().toString(), DRONE_ARMOUR_RATING_FLAT_BONUS);
//            }
//        }
    }
}
