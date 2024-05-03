package data.scripts.hullmods;

import cmu.misc.SubsystemManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.drones.pulveriser.PulveriserSubsystem;

public class PSE_PulveriserDrone extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        SubsystemManager.queueSubsystemForShip(ship, PulveriserSubsystem.class);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !ship.getHullStyleId().equals("PEARSON_EXOTRONICS_BASE_HULL") && !ship.isFighter() && !ship.isFrigate() && !ship.isDestroyer() && ship.getHullSpec().getDefenseType() != ShieldAPI.ShieldType.PHASE;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.isFrigate() || ship.isFighter()) return "Can not be installed on frigates!";
        if (ship.isDestroyer()) return "Can not be installed on destroyers!";
        if (ship.getHullSpec().getFighterBays() > 0) return "Cannot be installed on a carrier";
        if (ship.getHullStyleId().equals("PEARSON_EXOTRONICS_BASE_HULL")) return "Can only be installed on non-PSExotech hulls";
        return "Only compatible with applicable conversion targets.";
    }
}
