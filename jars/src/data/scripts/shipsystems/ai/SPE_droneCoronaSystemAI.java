package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.SPE_droneCorona;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class SPE_droneCoronaSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipSystemAPI system;
    private SPE_droneCorona droneSystem;

    private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    private boolean isShipInFocusModeEngagementRange = false;
    private boolean isTargetVulnerable = false;

    private float longestWeaponRange = 0f;
    private float droneLongestWeaponRange = 0;

    String UNIQUE_SYSTEM_ID;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            //gets the range of the longest range ballistic or energy weapon on init
            if (weapon.getRange() > longestWeaponRange) {
                longestWeaponRange = weapon.getRange();
            }
        }

        UNIQUE_SYSTEM_ID = "SPE_droneCorona_" + ship.hashCode();

        ship.useSystem();
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        this.droneSystem = (SPE_droneCorona) engine.getCustomData().get(UNIQUE_SYSTEM_ID);

        if (target != null) {
            float TARGET_VENT_TIME_REMAINING_THRESHOLD = 4f;
            isTargetVulnerable = target.getShield() == null || (target.getFluxTracker().isOverloadedOrVenting() && (target.getFluxTracker().getTimeToVent() > TARGET_VENT_TIME_REMAINING_THRESHOLD || target.getFluxTracker().getOverloadTimeRemaining() > TARGET_VENT_TIME_REMAINING_THRESHOLD));

            float distanceToTarget = MathUtils.getDistance(ship.getLocation(), target.getLocation());

            isShipInFocusModeEngagementRange = droneLongestWeaponRange > distanceToTarget;
        }

        if (tracker.intervalElapsed()  && ship != null) {
            List<MissileAPI> missilesInRange = AIUtils.getNearbyEnemyMissiles(ship, longestWeaponRange);
            boolean isMissileThreatPresent;
            isMissileThreatPresent = !missilesInRange.isEmpty();

            List<ShipAPI> shipsInRange = AIUtils.getNearbyEnemies(ship, longestWeaponRange);
            boolean isBomberThreatPresent;
            if (!shipsInRange.isEmpty()) {
                int num = 0;

                for (ShipAPI ship : shipsInRange) {
                    if (ship.getHullSize() == ShipAPI.HullSize.FIGHTER) {
                        for (WeaponAPI weapon : ship.getAllWeapons()) {
                            if (weapon.getType().equals(WeaponAPI.WeaponType.MISSILE)) {
                                num++;
                            }
                        }
                    }
                }

                isBomberThreatPresent = (num > 0);
            } else {
                isBomberThreatPresent = false;
            }

            boolean PANICAAAAA = isMissileThreatPresent || isBomberThreatPresent;

            switch (droneSystem.getDroneOrders()) {
                case DEPLOY:

                    for (ShipAPI drone : droneSystem.getDeployedDrones()) {
                        List<WeaponAPI> weapons = drone.getAllWeapons();
                        for (WeaponAPI weapon : weapons) {
                            if (weapon.getRange() > droneLongestWeaponRange) {
                                droneLongestWeaponRange = weapon.getRange();
                            }
                        }
                    }


                    if (isTargetVulnerable) {
                        ship.useSystem();
                    } else if (!PANICAAAAA && isShipInFocusModeEngagementRange) {
                        ship.useSystem();
                    }

                    break;
                case ATTACK:
                    if (AIUtils.getNearbyEnemies(ship, 2000f).isEmpty()) {
                        ship.useSystem();
                    }
                    break;
                case RECALL:
                    if (PANICAAAAA) {
                        ship.useSystem();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}