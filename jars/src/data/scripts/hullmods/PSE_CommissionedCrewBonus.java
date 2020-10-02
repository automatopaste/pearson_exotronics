package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.PSEDrone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PSE_CommissionedCrewBonus extends BaseHullMod {
    private static final float DRONE_ARMOUR_RATING_FLAT_BONUS = 10f;

    private static final Map<ShipAPI.HullSize, Float> ARMOUR_PER_HULLSIZE_FLAT = new HashMap<>();
    static {
        ARMOUR_PER_HULLSIZE_FLAT.put(ShipAPI.HullSize.CAPITAL_SHIP, 50f);
        ARMOUR_PER_HULLSIZE_FLAT.put(ShipAPI.HullSize.CRUISER, 25f);
        ARMOUR_PER_HULLSIZE_FLAT.put(ShipAPI.HullSize.DESTROYER, 10f);
        ARMOUR_PER_HULLSIZE_FLAT.put(ShipAPI.HullSize.FRIGATE, 5f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "" + (int) DRONE_ARMOUR_RATING_FLAT_BONUS;
        }
        if (index == 1) {
            return "" + ARMOUR_PER_HULLSIZE_FLAT.get(hullSize);
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
        stats.getArmorBonus().modifyFlat(id, ARMOUR_PER_HULLSIZE_FLAT.get(hullSize));
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
