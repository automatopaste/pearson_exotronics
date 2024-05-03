package data.scripts.hullmods;

import cmu.misc.SubsystemManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.drones.sepherum.SepherumSubsystem;

public class PSE_SepherumDrone extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        SubsystemManager.queueSubsystemForShip(ship, SepherumSubsystem.class);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }
}
