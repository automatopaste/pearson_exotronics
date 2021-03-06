package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.PSE_DroneBastion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PSE_DroneBastionSystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private final IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    private float longestWeaponRange = 0f;

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

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        //unique identifier so that individual system can be gotten from combat engine custom data
        String uniqueSystemId = PSE_DroneBastion.UNIQUE_SYSTEM_PREFIX + ship.hashCode();

        PSE_DroneBastion droneSystem = (PSE_DroneBastion) engine.getCustomData().get(uniqueSystemId);
        if (droneSystem == null || ship == null || !ship.isAlive()) {
            return;
        }

        if (tracker.intervalElapsed()  && ship != null) {
            List<MissileAPI> missilesInRange = AIUtils.getNearbyEnemyMissiles(ship, longestWeaponRange);
            boolean isMissileThreatPresent = !missilesInRange.isEmpty();
            float missileThreatAngle = 0;
            if (isMissileThreatPresent) {
                for (MissileAPI missile : missilesInRange) {
                    float a = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), missile.getLocation())));
                    a = Math.abs(a);
                    if (a > missileThreatAngle) {
                        missileThreatAngle = a;
                    }
                }
            }

            List<ShipAPI> shipsInRange = AIUtils.getNearbyEnemies(ship, longestWeaponRange);
            boolean isShipThreatPresent = !shipsInRange.isEmpty();
            float shipThreatAngle = 0;
            if (isShipThreatPresent) {
                for (ShipAPI eship : shipsInRange) {
                    float a = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), eship.getLocation())));
                    a = Math.abs(a);
                    if (a > shipThreatAngle) {
                        shipThreatAngle = a;
                    }
                }
            }

            List<ShipAPI> enemiesInRange = AIUtils.getNearbyEnemies(ship, longestWeaponRange * 2f);
            switch (droneSystem.getDroneOrders()) {
                case FRONT:
                    if ((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle >= 60 || missileThreatAngle >= 90)) {
                        droneSystem.nextDroneOrder();
                    } else if (enemiesInRange.isEmpty() && !isMissileThreatPresent) {
                        droneSystem.nextDroneOrder();
                    }
                    break;
                case CARDINAL:
                    if ((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle < 60 || missileThreatAngle < 90)) {
                        droneSystem.nextDroneOrder();
                    } else if (enemiesInRange.isEmpty() && !isMissileThreatPresent) {
                        droneSystem.nextDroneOrder();
                    }
                    break;
                case RECALL:
                    if (!enemiesInRange.isEmpty() || isMissileThreatPresent) {
                        droneSystem.nextDroneOrder();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}