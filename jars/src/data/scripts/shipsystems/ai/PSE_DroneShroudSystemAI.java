package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneShroud;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PSE_DroneShroudSystemAI implements ShipSystemAIScript {
    private IntervalUtil tracker = new IntervalUtil(0.5f, 1.5f);

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private static final float CONCERN_WEIGHT_THRESHOLD = 30f;
    private static final Map<ShipAPI.HullSize, Float> mults = new HashMap<>();
    static {
        mults.put(ShipAPI.HullSize.FIGHTER, 0.1f);
        mults.put(ShipAPI.HullSize.CAPITAL_SHIP, 1f);
        mults.put(ShipAPI.HullSize.CRUISER, 0.8f);
        mults.put(ShipAPI.HullSize.DESTROYER, 0.4f);
        mults.put(ShipAPI.HullSize.FRIGATE, 0.2f);
    }

    private float longestWeaponRange = 0f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            //gets the longest range weapon
            if (weapon.getRange() > longestWeaponRange) {
                longestWeaponRange = weapon.getRange();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        if (!tracker.intervalElapsed()) {
            return;
        }

        //unique identifier so that individual system can be gotten from combat engine custom data
        String uniqueSystemId = PSE_DroneShroud.UNIQUE_SYSTEM_PREFIX + ship.hashCode();
        PSE_DroneShroud droneSystem = (PSE_DroneShroud) engine.getCustomData().get(uniqueSystemId);
        if (droneSystem == null || ship == null || !ship.isAlive()) {
            return;
        }

        String key = "PSE_DroneList_" + ship.hashCode();
        ArrayList<PSEDrone> drones = (ArrayList<PSEDrone>) engine.getCustomData().get(key);
        if (drones == null) return;

        int count = 0;
        for (PSEDrone drone : drones) {
            if (drone.getFluxTracker().getHardFlux() > 0.5f) {
                count += 1;
            }
        }

        float concernWeightTotal = 0f;
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, longestWeaponRange)) {
            if (enemy == null || enemy.getFleetMember() == null) {
                continue;
            }

            float weight = enemy.getFleetMember().getDeploymentCostSupplies();
            weight *= mults.get(enemy.getHullSize());
            if (enemy.getFluxTracker().isOverloadedOrVenting()) {
                weight *= 0.75f;
            }
            if (enemy.getHullLevel() < 0.4f) {
                weight *= 0.25f;
            }
            if (enemy.getFluxLevel() > 0.5f) {
                weight *= 0.4f;
            }
            if (enemy.getEngineController().isFlamedOut()) {
                weight *= 0.1f;
            }

            concernWeightTotal += (weight * (1f - (MathUtils.getDistance(ship, enemy) / longestWeaponRange)));
        }

        //useful debug display
        //engine.maintainStatusForPlayerShip("SHROUD_DEBUG", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", concernWeightTotal + ", " + count, true);

        switch (droneSystem.getDroneOrders()) {
            case CIRCLE:
                if (concernWeightTotal <= CONCERN_WEIGHT_THRESHOLD && count <= 1) {
                    droneSystem.nextDroneOrder();
                } else if (AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
            case BROADSIDE_MOVEMENT:
                if (concernWeightTotal > CONCERN_WEIGHT_THRESHOLD) {
                    droneSystem.nextDroneOrder();
                } else if (count > 1) {
                    droneSystem.nextDroneOrder();
                } else if (AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
            case RECALL:
                if (!AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
        }
    }
}