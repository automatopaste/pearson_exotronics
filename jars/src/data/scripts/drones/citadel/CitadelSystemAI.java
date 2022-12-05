package data.scripts.drones.citadel;

import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.bastion.BastionShipsystem;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class CitadelSystemAI implements ShipSystemAIScript {

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

        CitadelShipsystem system = (CitadelShipsystem) SystemData.getDroneSystem(ship, engine);
        if (system == null) return;

        boolean isMissileThreatPresent = missileDangerDir == null;

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
            case ANTI_FIGHTER:
                if (((shipThreatAngle >= 90) && !checkAttackingFlags()) || (enemiesInRange.isEmpty() && !isMissileThreatPresent)) {
                    system.cycleDroneOrders();
                }
                break;
            case SHIELD:
                if ((checkAttackingFlags() && !(shipThreatAngle >= 90)) || (enemiesInRange.isEmpty() && !isMissileThreatPresent)) {
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

    private boolean checkAttackingFlags() {
        return ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) || ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PURSUING);
    }
}