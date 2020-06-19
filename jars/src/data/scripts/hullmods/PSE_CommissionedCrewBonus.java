package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.PSEDroneAPI;

import java.awt.*;
import java.util.ArrayList;

public class PSE_CommissionedCrewBonus extends BaseHullMod {
    public static final float DRONE_ARMOUR_RATING_FLAT_BONUS = 10f;

    @Override //All you need is this to be honest. The framework will do everything on its own.
    public void applyEffectsAfterShipCreation (ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("CHM_commission")) {
            ship.getVariant().removeMod("CHM_commission");
        }
        // This is to remove the unnecessary dummy hull mod. Unless the player want it... but nah!
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "" + (int) DRONE_ARMOUR_RATING_FLAT_BONUS;
        }
        return null;
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        if (fighter.getHullSpec().getBaseHullId().startsWith("PSE_") || fighter.getFleetMember().getCrewComposition().getCrewInt() <= 0) {
            fighter.getMutableStats().getEffectiveArmorBonus().modifyFlat(this.getClass().toString(), DRONE_ARMOUR_RATING_FLAT_BONUS);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (ship == null || !ship.isAlive()) {
            return;
        }

        String key = "PSE_DroneList_" + ship.hashCode();

        ArrayList<PSEDroneAPI> list = (ArrayList<PSEDroneAPI>) engine.getCustomData().get(key);

        if (list != null && !list.isEmpty()) {
            for (PSEDroneAPI drone : list) {
                drone.getMutableStats().getEffectiveArmorBonus().modifyFlat(this.getClass().toString(), DRONE_ARMOUR_RATING_FLAT_BONUS);
            }
        }
    }
}
