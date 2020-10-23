package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.plugins.PSE_DroneManagerPlugin;
import data.scripts.util.MagicIncompatibleHullmods;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PSE_ConvertedDroneHangars extends BaseHullMod {
    private static final int ADDITIONAL_CREW_PER_BAY = 15;
    private static final float BOMBER_COST_INCREASE_PERCENT = 50f;
    private static final float FIGHTER_REFIT_TIME_INCREASE_PERCENT = 50f;
    private static final float DRONE_SYSTEM_LAUNCH_DELAY_MULT = 3f;
    private static final float DRONE_SYSTEM_REGEN_MULT = 5f;

    private static Map<ShipAPI.HullSize, Integer> bays = new HashMap<>();
    static {
        bays.put(ShipAPI.HullSize.FIGHTER, 0);
        bays.put(ShipAPI.HullSize.FRIGATE, 0);
        bays.put(ShipAPI.HullSize.DESTROYER, 1);
        bays.put(ShipAPI.HullSize.CRUISER, 2);
        bays.put(ShipAPI.HullSize.CAPITAL_SHIP, 3);
    }

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static {
        BLOCKED_HULLMODS.add(HullMods.CONVERTED_HANGAR);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyFlat(id, ADDITIONAL_CREW_PER_BAY * bays.get(hullSize));
        stats.getNumFighterBays().modifyFlat(id, bays.get(hullSize));

        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, BOMBER_COST_INCREASE_PERCENT);
        stats.getFighterRefitTimeMult().modifyPercent(id, FIGHTER_REFIT_TIME_INCREASE_PERCENT);
        stats.getDynamic().getMod(PSE_DroneManagerPlugin.LAUNCH_DELAY_STAT_KEY).modifyMult(id, DRONE_SYSTEM_LAUNCH_DELAY_MULT);
        stats.getDynamic().getMod(PSE_DroneManagerPlugin.REGEN_DELAY_STAT_KEY).modifyMult(id, DRONE_SYSTEM_REGEN_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String hullmod : BLOCKED_HULLMODS) {
            if(ship.getVariant().getHullMods().contains(hullmod)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(
                        ship.getVariant(),
                        hullmod,
                        "PSE_ConvertedDroneHangars"
                );
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && !ship.isFrigate() && !ship.isFighter() &&
                ship.getHullSpec().getFighterBays() <= 0 &&
                !ship.getVariant().hasHullMod(HullMods.CONVERTED_BAY) &&
                ship.getSystem().getId().startsWith("PSE_drone");
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.isFrigate() || ship.isFighter()) return "Can not be installed on frigates!";
        if (ship.getHullSpec().getFighterBays() > 0) return "Ship has standard fighter bays";
        if (ship.getVariant().hasHullMod(HullMods.CONVERTED_HANGAR)) return "Not compatible with Converted Hangar";
        return "Only compatible with Pearson Exotronics ships with a drone system";
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return bays.get(ShipAPI.HullSize.DESTROYER) + "";
        if (index == 1) return bays.get(ShipAPI.HullSize.CRUISER) + "";
        if (index == 2) return bays.get(ShipAPI.HullSize.CAPITAL_SHIP) + "";
        if (index == 3) return ADDITIONAL_CREW_PER_BAY + "";
        if (index == 4) return (int) FIGHTER_REFIT_TIME_INCREASE_PERCENT + "%";
        if (index == 5) return (int) BOMBER_COST_INCREASE_PERCENT + "%";
        if (index == 6) return (int) DRONE_SYSTEM_LAUNCH_DELAY_MULT + "";
        if (index == 7) return (int) DRONE_SYSTEM_REGEN_MULT + "";
        return null;
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }
}
