package data.scripts.hullmods;

import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.PSEModPlugin;

public class PSE_OverclockedForgeVats extends BaseHullMod {
    private static final float FORGE_SPEED_INCREASE_PERCENT = 50f;
    private static final float MISSILE_HEALTH_DECREASE_PERCENT = -50f;
    private static final float MISSILE_DAMAGE_DECREASE_PERCENT = -25f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(ForgeTracker.REGEN_DELAY_STAT_KEY).modifyPercent(id, -FORGE_SPEED_INCREASE_PERCENT);

        stats.getMissileHealthBonus().modifyPercent(id, MISSILE_HEALTH_DECREASE_PERCENT);
        stats.getMissileWeaponDamageMult().modifyPercent(id, MISSILE_DAMAGE_DECREASE_PERCENT);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && !ship.isFighter() && ship.getSystem() != null && PSEModPlugin.DRONE_SYSTEM_IDS.contains(ship.getSystem().getId());
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.isFighter()) return "Can not be installed on fighters!";
        return "Only compatible with Pearson Exotronics ships with a drone system";
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return (int) FORGE_SPEED_INCREASE_PERCENT + "%";
        if (index == 1) return (int) -MISSILE_HEALTH_DECREASE_PERCENT + "%";
        if (index == 2) return (int) -MISSILE_DAMAGE_DECREASE_PERCENT + "%";
        return null;
    }
}
