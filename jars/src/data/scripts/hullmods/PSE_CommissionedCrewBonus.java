package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.PSEDrone;

import java.util.ArrayList;

public class PSE_CommissionedCrewBonus extends BaseHullMod {
    public static final float DRONE_ARMOUR_RATING_FLAT_BONUS = 10f;
    public static final float SHIP_ARMOUR_RATING_BONUS = 5f;

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "" + (int) DRONE_ARMOUR_RATING_FLAT_BONUS;
        }
        if (index == 1) {
            return "" + (int) SHIP_ARMOUR_RATING_BONUS + "%";
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
        stats.getArmorBonus().modifyMult(id, 1f + (SHIP_ARMOUR_RATING_BONUS / 100f));
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
