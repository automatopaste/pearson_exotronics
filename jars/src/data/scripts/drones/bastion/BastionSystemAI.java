package data.scripts.drones.bastion;

import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.shroud.ShroudShipsystem;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class BastionSystemAI implements ShipSystemAIScript {

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
        if (ship == null || !ship.isAlive() | ship.isHulk()) return;

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        BastionShipsystem system = (BastionShipsystem) SystemData.getDroneSystem(ship, engine);
        if (system == null) return;

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
        switch (system.getDroneOrders()) {
            case FRONT:
                if (((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle >= 60 || missileThreatAngle >= 90)) || (enemiesInRange.isEmpty() && !isMissileThreatPresent)) {
                    system.cycleDroneOrders();
                }
                break;
            case CARDINAL:
                if (((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle < 60 || missileThreatAngle < 90)) || (enemiesInRange.isEmpty() && !isMissileThreatPresent)) {
                    system.cycleDroneOrders();
                }
                break;
            case RECALL:
                if (!enemiesInRange.isEmpty() || isMissileThreatPresent) {
                    system.cycleDroneOrders();
                }
                break;
            default:
                break;
        }
    }
}