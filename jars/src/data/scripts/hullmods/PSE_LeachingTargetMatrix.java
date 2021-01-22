package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;

import java.util.ArrayList;
import java.util.List;

public class PSE_LeachingTargetMatrix extends BaseHullMod {
    private IntervalUtil tracker = new IntervalUtil(1f, 1f);
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || !ship.isAlive() || engine == null || engine.isPaused()) return;

        if (!tracker.intervalElapsed()) return;

        List<PSEDrone> drones = new ArrayList<>();
        for (ShipAPI s : engine.getShips()) {
            if (s instanceof PSEDrone) {
                PSEDrone drone = (PSEDrone) s;

                if (drone.getLaunchingShip().equals(ship)) drones.add(drone);
            }
        }

        for (PSEDrone drone : drones) {
            drone.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f));
            drone.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f));
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        float rangeMult = ship.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f) * 100f;

        String desc;
        String amount;
        if (rangeMult >= 100f) {
            amount = (int) (rangeMult - 100f) + "%";
            desc = "increased";
        } else {
            amount = (int) (100f - rangeMult) + "";
            desc = "decreased";
        }

        if (index == 0) return desc;
        if (index == 1) return amount;
        return null;
    }
}