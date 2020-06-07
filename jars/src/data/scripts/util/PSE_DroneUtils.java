package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

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

    public static void move(PSEDroneAPI drone, float droneFacing, Vector2f movementTargetLocation, float sanity, IntervalUtil velocityRotationIntervalTracker, float damping) {
        //The bones of the movement AI are below, all it needs is a target vector location to move to

        //GET USEFUL VALUES
        float angleFromDroneToTargetLocation = VectorUtils.getAngle(drone.getLocation(), movementTargetLocation); //ABSOLUTE 360 ANGLE

        float droneVelocityAngle = VectorUtils.getFacing(drone.getVelocity()); //ABSOLUTE 360 ANGLE

        float rotationFromFacingToLocationAngle = MathUtils.getShortestRotation(droneFacing, angleFromDroneToTargetLocation); //ROTATION ANGLE
        float rotationFromVelocityToLocationAngle = MathUtils.getShortestRotation(droneVelocityAngle, angleFromDroneToTargetLocation); //ROTATION ANGLE

        float distanceToTargetLocation = MathUtils.getDistance(drone.getLocation(), movementTargetLocation); //DISTANCE

        //FIND DISTANCE THAT CAN BE DECELERATED FROM CURRENT SPEED TO ZERO s = v^2 / 2a
        float decelerationDistance = (float) (Math.pow(drone.getVelocity().length(), 2)) / (2 * drone.getDeceleration());
        decelerationDistance *= damping;


        //DECELERATE IF IN THRESHOLD DISTANCE OF TARGET
        if (distanceToTargetLocation <= decelerationDistance) {
            drone.giveCommand(ShipCommand.DECELERATE, null, 0);
        }

        //DO LARGE MOVEMENT IF OVER DISTANCE THRESHOLD
        if (distanceToTargetLocation >= decelerationDistance) {
            rotationFromFacingToLocationAngle = Math.round(rotationFromFacingToLocationAngle);

            //COURSE CORRECTION
            drone.getVelocity().set(VectorUtils.rotate(drone.getVelocity(), rotationFromVelocityToLocationAngle * 0.05f));

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
    }

    //FUNCTIONS
    public static void rotateToTarget(ShipAPI ship, PSEDroneAPI drone, Vector2f targetedLocation, float droneFacing, float rotationMagnitude) {
        //ROTATION

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        //float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));


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
        drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);
    }

    public static void rotateToTarget(PSEDroneAPI drone, float absoluteFacingTargetAngle, float droneFacing, float rotationMagnitude) {
        //ROTATION: THE SEQUEL

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        //float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));

        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, absoluteFacingTargetAngle);

        //ROTATE TOWARDS TARGET would prefer to use shipcommands but this is more reliable
        drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);
    }

    public static void attemptToLand(ShipAPI ship, PSEDroneAPI drone, float amount, IntervalUtil delayBeforeLandingTracker) {
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

    public static Vector2f getEnemyTargetLocation(ShipAPI ship, PSEDroneAPI drone, float weaponRange, boolean ignoreMissiles, boolean ignoreFighters, boolean ignoreShips) {
        //GET NEARBY OBJECTS TO SHOOT AT priority missiles > fighters > ships

        MissileAPI droneTargetMissile = null;
        if (!ignoreMissiles) {
            //get missile close to mothership
            MissileAPI nearestEnemyMissile = AIUtils.getNearestEnemyMissile(ship);

            //select missile target, or null if none in range
            if (nearestEnemyMissile != null && MathUtils.getDistance(drone, nearestEnemyMissile) < weaponRange) {
                droneTargetMissile = nearestEnemyMissile;
            } else {
                droneTargetMissile = null;
            }
        }

        ShipAPI droneTargetShip = null;
        if (!ignoreShips) {
            //get non-fighter ship close to mothership
            ShipAPI nearestEnemyShip = getNearestEnemyNonFighterShip(ship);

            //select non-fighter ship target, or null if none in range
            if (nearestEnemyShip != null && MathUtils.getDistance(drone, nearestEnemyShip) < weaponRange) {
                droneTargetShip = nearestEnemyShip;
            } else {
                droneTargetShip = null;
            }
        }

        ShipAPI droneTargetFighter = null;
        if (!ignoreFighters) {
            //get fighter close to drone
            ShipAPI nearestEnemyFighter = getNearestEnemyFighter(drone);

            //select fighter target, or null if none in range
            if (nearestEnemyFighter != null && MathUtils.getDistance(drone, nearestEnemyFighter) < weaponRange) {
                droneTargetFighter = nearestEnemyFighter;
            } else {
                droneTargetFighter = null;
            }
        }


        //PRIORITISE TARGET, SET LOCATION
        Vector2f targetedLocation;
        if (droneTargetMissile != null) {
            targetedLocation = droneTargetMissile.getLocation();
        } else if (droneTargetFighter != null) {
            targetedLocation = droneTargetFighter.getLocation();
        } else if (droneTargetShip != null) {
            targetedLocation = droneTargetShip.getLocation();
        } else {
            targetedLocation = null;
        }
        return targetedLocation;
    }
}
