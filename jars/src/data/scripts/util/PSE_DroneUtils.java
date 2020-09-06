package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.lazywizard.lazylib.combat.AIUtils.getEnemiesOnMap;

public final class PSE_DroneUtils {
    private static CombatEngineAPI engine;

    private PSE_DroneUtils() {
        engine = Global.getCombatEngine();
    }

    //directly modified from lazywizard's lazylib getNearestEnemy
    public static ShipAPI getNearestEnemyNonFighterShip(final CombatEntityAPI entity) {
        ShipAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI tmp : getEnemiesOnMap(entity)) {
            if (tmp.isFighter()) {
                continue;
            }
            float distance = MathUtils.getDistance(tmp, entity.getLocation());
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    //directly modified from lazywizard's lazylib getNearestEnemy
    public static ShipAPI getNearestEnemyFighter(final CombatEntityAPI entity) {
        ShipAPI closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI tmp : getEnemiesOnMap(entity)) {
            if (!tmp.isFighter()) {
                continue;
            }
            float distance = MathUtils.getDistance(tmp, entity.getLocation());
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public static void move(PSEDrone drone, float droneFacing, Vector2f movementTargetLocation, float sanity, IntervalUtil velocityRotationIntervalTracker) {
        //The bones of the movement AI are below, all it needs is a target vector location to move to

        //GET USEFUL VALUES
        float angleFromDroneToTargetLocation = VectorUtils.getAngle(drone.getLocation(), movementTargetLocation); //ABSOLUTE 360 ANGLE

        float droneVelocityAngle = VectorUtils.getFacing(drone.getVelocity()); //ABSOLUTE 360 ANGLE

        float rotationFromFacingToLocationAngle = MathUtils.getShortestRotation(droneFacing, angleFromDroneToTargetLocation); //ROTATION ANGLE
        float rotationFromVelocityToLocationAngle = MathUtils.getShortestRotation(droneVelocityAngle, angleFromDroneToTargetLocation); //ROTATION ANGLE

        float distanceToTargetLocation = MathUtils.getDistance(drone.getLocation(), movementTargetLocation); //DISTANCE

        //damping scaling based on ship speed (function y = -x + 2 where x is 0->1)
        float damping = (-drone.getLaunchingShip().getVelocity().length() / drone.getLaunchingShip().getMaxSpeedWithoutBoost()) + 2f;

        //FIND DISTANCE THAT CAN BE DECELERATED FROM CURRENT SPEED TO ZERO s = v^2 / 2a
        float decelerationDistance = (float) (Math.pow(drone.getVelocity().length(), 2)) / (2 * drone.getDeceleration());
        //decelerationDistance *= 0.5f;

        //DO LARGE MOVEMENT IF OVER DISTANCE THRESHOLD
        if (distanceToTargetLocation >= decelerationDistance) {
            rotationFromFacingToLocationAngle = Math.round(rotationFromFacingToLocationAngle);

            //COURSE CORRECTION
            drone.getVelocity().set(VectorUtils.rotate(drone.getVelocity(), rotationFromVelocityToLocationAngle * 0.5f));

            //accelerate forwards or backwards
            if (
                    90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > -90f
            ) { //between 90 and -90 is an acute angle therefore in front
                drone.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else if (
                    (180f >= rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > 90f) || (-90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle >= -180f)
            ) { //falls between 90 to 180 or -90 to -180, which should be obtuse and thus relatively behind
                drone.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            }

            //strafe left or right
            if (
                    180f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > 0f
            ) { //between 0 and 180 (i.e. left)
                drone.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            } else if (
                    0f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > -180f
            ) { //between 0 and -180 (i.e. right)
                drone.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
            }
        } else {
            //COURSE CORRECTION
            if (velocityRotationIntervalTracker.intervalElapsed()) {
                drone.getVelocity().set(VectorUtils.rotate(drone.getVelocity(), rotationFromVelocityToLocationAngle * sanity));
            }
        }

        //DECELERATE IF IN THRESHOLD DISTANCE OF TARGET
        if (distanceToTargetLocation <= decelerationDistance) {
            drone.giveCommand(ShipCommand.DECELERATE, null, 0);

            float frac = distanceToTargetLocation / decelerationDistance;
            frac = (float) Math.sqrt(frac);
            //drone.getVelocity().set((Vector2f) drone.getVelocity().scale(frac));

            if (frac <= 0.25f) {
                drone.getVelocity().set(drone.getLaunchingShip().getVelocity());
            } else {
                drone.getVelocity().set((Vector2f) drone.getVelocity().scale(frac));
            }
        }
    }

    public static void snapToLocation(PSEDrone drone, Vector2f target) {
        drone.getLocation().set(target);
    }

    public static void rotateToTarget(ShipAPI ship, PSEDrone drone, Vector2f targetedLocation, float droneFacing, float rotationMagnitude) {
        engine = Global.getCombatEngine();

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));


        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta;
        if (targetedLocation != null) {
            //GET ABSOLUTE ANGLE FROM DRONE TO TARGETED LOCATION
            Vector2f droneToTargetedLocDir = VectorUtils.getDirectionalVector(drone.getLocation(), targetedLocation);
            float droneAngleToTargetedLoc = VectorUtils.getFacing(droneToTargetedLocDir); //ABSOLUTE 360 ANGLE

            rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, droneAngleToTargetedLoc);
        } else {
            rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, ship.getFacing());
        }

        //ROTATE TOWARDS TARGET would prefer to use shipcommands but this is more reliable
        //drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);

        float angvel = drone.getAngularVelocity();
        if (rotationAngleDelta > 0f) {
            angvel += drone.getTurnAcceleration() * engine.getElapsedInLastFrame();
        } else if (rotationAngleDelta < 0f) {
            angvel -= drone.getTurnAcceleration() * engine.getElapsedInLastFrame();
        }
        MathUtils.clamp(angvel, -drone.getMaxTurnRate(), drone.getMaxTurnRate());

        drone.setAngularVelocity(angvel);
    }

    public static void rotateToFacing(PSEDrone drone, float absoluteFacingTargetAngle, float droneFacing, float rotationMagnitude) {
        //ROTATION: THE SEQUEL

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm cause it's kind of unnecessary)
        //float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));

        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, absoluteFacingTargetAngle);

        //ROTATE TOWARDS TARGET would prefer to use shipcommands but this is more reliable
        drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);
    }

    public static void attemptToLand(ShipAPI ship, PSEDrone drone, float amount, IntervalUtil delayBeforeLandingTracker) {
        delayBeforeLandingTracker.advance(amount);
        engine = Global.getCombatEngine();
        boolean isPlayerShip = ship.equals(engine.getPlayerShip());

        if (drone.isLanding()) {
            delayBeforeLandingTracker.setElapsed(0);
            if (isPlayerShip) {
                engine.maintainStatusForPlayerShip("PSE_STATUS_KEY_DRONE_LANDING_STATE", "graphics/icons/hullsys/drone_pd_high.png", "LANDING STATUS", "LANDING... ", false);
            }
        } else {
            float round = Math.round((delayBeforeLandingTracker.getIntervalDuration() - delayBeforeLandingTracker.getElapsed()) * 100) / 100f;
            if (isPlayerShip) {
                engine.maintainStatusForPlayerShip("PSE_STATUS_KEY_DRONE_LANDING_STATE", "graphics/icons/hullsys/drone_pd_high.png", "LANDING STATUS", "LANDING IN " + round, false);
            }
        }

        if (delayBeforeLandingTracker.intervalElapsed()) {
            drone.beginLandingAnimation(ship);
        }
    }

    public static CombatEntityAPI getEnemyTarget(ShipAPI ship, PSEDrone drone, float weaponRange, boolean ignoreMissiles, boolean ignoreFighters, boolean ignoreShips, float targetingArcDeviation) {
        //GET NEARBY OBJECTS TO SHOOT AT priority missiles > fighters > ships
        float facing = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(ship, drone.getShipAPI()));

        MissileAPI droneTargetMissile = null;
        if (!ignoreMissiles) {
            //get missile close to mothership
            List<MissileAPI> enemyMissiles = AIUtils.getNearbyEnemyMissiles(ship, weaponRange);
            float tracker = Float.MAX_VALUE;
            for (MissileAPI missile : enemyMissiles) {
                if (!PSE_MiscUtils.isEntityInArc(missile, drone.getLocation(), facing, targetingArcDeviation)) {
                    continue;
                }

                float distance = MathUtils.getDistance(missile, drone);
                if (distance < tracker) {
                    tracker = distance;
                    droneTargetMissile = missile;
                }
            }
        }

        ShipAPI droneTargetShip = null;
        if (!ignoreShips) {
            if (ship.getShipTarget() != null) {
                droneTargetShip = ship.getShipTarget();
            } else {
                //get non-fighter ship close to mothership
                List<ShipAPI> enemyShips = AIUtils.getNearbyEnemies(drone, weaponRange);
                float tracker = Float.MAX_VALUE;
                for (ShipAPI enemyShip : enemyShips) {
                    if (enemyShip.isFighter()) {
                        continue;
                    }

                    //check if there is a friendly ship in the way
                    boolean areFriendliesInFiringArc = false;
                    float relAngle = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(drone, enemyShip));
                    for (ShipAPI ally : AIUtils.getNearbyAllies(drone, weaponRange)) {
                        if (PSE_MiscUtils.isEntityInArc(ally, drone.getLocation(), relAngle, 20f)) {
                            if (MathUtils.getDistance(enemyShip, drone) > MathUtils.getDistance(ally, drone)) {
                                areFriendliesInFiringArc = true;
                                break;
                            }
                        }
                    }
                    if (areFriendliesInFiringArc) {
                        continue;
                    }

                    //can only match similar facing to host ship for balancing
                    if (!PSE_MiscUtils.isEntityInArc(enemyShip, drone.getLocation(), facing, targetingArcDeviation)) {
                        continue;
                    }

                    float distance = MathUtils.getDistance(enemyShip, drone);
                    if (distance < tracker) {
                        tracker = distance;
                        droneTargetShip = enemyShip;
                    }
                }
            }
        }

        ShipAPI droneTargetFighter = null;
        if (!ignoreFighters) {
            //get fighter close to drone
            List<ShipAPI> enemyShips = AIUtils.getNearbyEnemies(drone, weaponRange);
            float tracker = Float.MAX_VALUE;
            for (ShipAPI enemyShip : enemyShips) {
                if (!enemyShip.isFighter()) {
                    continue;
                }

                if (!PSE_MiscUtils.isEntityInArc(enemyShip, drone.getLocation(), facing, targetingArcDeviation)) {
                    continue;
                }

                float distance = MathUtils.getDistance(enemyShip, drone);
                if (distance < tracker) {
                    tracker = distance;
                    droneTargetFighter = enemyShip;
                }
            }
        }

        //PRIORITISE TARGET, SET LOCATION
        CombatEntityAPI target;
        if (droneTargetMissile != null) {
            target = droneTargetMissile;
        } else if (droneTargetFighter != null) {
            target = droneTargetFighter;
        } else target = droneTargetShip;
        return target;
    }

    public static boolean areFriendliesBlockingArc(CombatEntityAPI drone, CombatEntityAPI target, float focusWeaponRange, float arcFacing, float arcDeviation) {
        boolean areFriendliesInFiringArc = false;
        for (ShipAPI ally : AIUtils.getNearbyAllies(drone, focusWeaponRange)) {
            if (!PSE_MiscUtils.isEntityInArc(ally, drone.getLocation(), arcFacing, arcDeviation)) {
                continue;
            }
            if (MathUtils.getDistance(target, drone) < MathUtils.getDistance(ally, drone)) {
                continue;
            }
            areFriendliesInFiringArc = true;
            break;
        }
        return areFriendliesInFiringArc;
    }
}
