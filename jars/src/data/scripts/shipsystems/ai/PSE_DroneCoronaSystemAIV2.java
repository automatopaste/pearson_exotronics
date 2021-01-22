package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneCorona;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.*;

public class PSE_DroneCoronaSystemAIV2 implements ShipSystemAIScript {
    private static final float CONCERN_THRESHOLD = 1000f;

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    private float longestWeaponRange = 0f;
    private float droneBeamRange = 0f;

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
        String uniqueSystemId = PSE_DroneCorona.UNIQUE_SYSTEM_PREFIX + ship.hashCode();

        PSE_DroneCorona droneSystem = (PSE_DroneCorona) engine.getCustomData().get(uniqueSystemId);
        if (!tracker.intervalElapsed() || droneSystem == null || ship == null || !ship.isAlive()) {
            return;
        }

        if (!droneSystem.getDeployedDrones().isEmpty()) {
            PSEDrone drone = droneSystem.getDeployedDrones().get(0);
            float range = 0;
            for (WeaponAPI weapon : drone.getUsableWeapons()) {
                if (weapon.getRange() > range) range = weapon.getRange();
            }
            droneBeamRange = range * drone.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f);
        }

        boolean isInEngagementRange = false;
        ShipAPI enemy = AIUtils.getNearestEnemy(ship);
        if (ship.getShipTarget() != null) enemy = ship.getShipTarget();
        if (enemy != null && MathUtils.getDistance(enemy, ship) <= droneBeamRange) {
            isInEngagementRange = true;
        }

        boolean zapRecommended = false;
        if (
                ship.getAIFlags().hasFlag(AUTO_BEAM_FIRING_AT_PHASE_SHIP) ||
                ship.getAIFlags().hasFlag(AUTO_FIRING_AT_PHASE_SHIP) ||
                ship.getAIFlags().hasFlag(MAINTAINING_STRIKE_RANGE) ||
                ship.getAIFlags().hasFlag(MANEUVER_TARGET)
        ) zapRecommended = true;

        boolean panicAa = false;
        if (
                ship.getAIFlags().hasFlag(TURN_QUICKLY) ||
                ship.getAIFlags().hasFlag(RUN_QUICKLY)
        ) panicAa = true;

        List<MissileAPI> missilesInRange = AIUtils.getNearbyEnemyMissiles(ship, longestWeaponRange);
        boolean isMissileThreatPresent;
        float missileConcernTracker = 0f;
        for (MissileAPI missile : missilesInRange) {
            if (missile.isGuided()) {
                missileConcernTracker += missile.getDamageAmount() * (1f - (MathUtils.getDistance(ship, missile) / longestWeaponRange));
            } else {
                //get velocity of missile relative to the ship
                Vector2f relativeVelocity = Vector2f.sub(missile.getVelocity(), ship.getVelocity(), new Vector2f());

                if (ship.getShield() == null || ship.getShield().isOff()) {
                    if (CollisionUtils.getCollisionPoint(missile.getLocation(), (Vector2f) relativeVelocity.scale(10f), ship) != null) {
                        missileConcernTracker += missile.getDamageAmount();
                    }
                } else {
                    if (CollisionUtils.getCollides(missile.getLocation(), (Vector2f) relativeVelocity.scale(10f), ship.getLocation(), ship.getShieldRadiusEvenIfNoShield())) {
                        missileConcernTracker += missile.getDamageAmount();
                    }
                }

            }
        }

        float threshold = CONCERN_THRESHOLD;
        String id = Personalities.STEADY;
        if (ship.getFleetMember() != null) id = ship.getFleetMember().getCaptain().getPersonalityAPI().getId();
        switch (id) {
            case Personalities.RECKLESS:
                threshold *= 5f;
            case Personalities.AGGRESSIVE:
                threshold *= 2f;
                break;
            case Personalities.STEADY:
                break;
            case Personalities.TIMID:
            case Personalities.CAUTIOUS:
                threshold *= 0.5f;
                break;
        }
        if (ship.getShield() != null && ship.getShield().isOn()) threshold *= 2f;
        isMissileThreatPresent = missileConcernTracker >= threshold;

        //engine.maintainStatusForPlayerShip("gaming", null, "THRESHOLD, PERSONALITY", threshold + ", " + id, true);
        Global.getLogger(PSE_DroneCoronaSystemAIV2.class).info("Threshold: " + threshold + ", Personality: " + id);

        switch (droneSystem.getDroneOrders()) {
            case DEPLOY:
                if (zapRecommended) {
                    if (id.equals(Personalities.RECKLESS)) {
                        droneSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.ATTACK);
                    } else if (!panicAa && isInEngagementRange && !isMissileThreatPresent){
                        droneSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.ATTACK);
                    }
                }
                if (AIUtils.getNearbyEnemies(ship, 2500f).isEmpty()) droneSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.RECALL);
                break;
            case ATTACK:
                if (panicAa || !isInEngagementRange || isMissileThreatPresent) droneSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.DEPLOY);
                break;
            case RECALL:
                if (!AIUtils.getNearbyEnemies(ship, 2500f).isEmpty()) droneSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.DEPLOY);
                break;
        }
    }
}