package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public final class PSE_DroneUtils {
    private static final Color DRONE_EXPLOSION_COLOUR = new Color(183, 255, 153, 255);

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
            if (90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > -90f
            ) { //between 90 and -90 is an acute angle therefore in front
                drone.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else if ((180f >= rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > 90f) || (-90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle >= -180f)
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

    public static void rotateToTarget(ShipAPI ship, PSEDrone drone, Vector2f targetedLocation, CombatEngineAPI engine) {
        engine = Global.getCombatEngine();
        float droneFacing = drone.getFacing();

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));


        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta;
        if (targetedLocation != null) {
            //GET ABSOLUTE ANGLE FROM DRONE TO TARGETED LOCATION
            Vector2f droneToTargetedLocDir = VectorUtils.getDirectionalVector(drone.getLocation(), targetedLocation);
            float droneAngleToTargetedLoc = VectorUtils.getFacing(droneToTargetedLocDir); //ABSOLUTE 360 ANGLE

            rotateToFacing(drone, droneAngleToTargetedLoc, engine);
        } else {
            rotateToFacing(drone, ship.getFacing(), engine);
        }
    }

    public static void rotateToFacing(PSEDrone drone, float absoluteFacingTargetAngle, CombatEngineAPI engine) {
        float droneFacing = drone.getFacing();
        float angvel = drone.getAngularVelocity();
        float rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, absoluteFacingTargetAngle);

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2
        float decelerationAngleAbs = (angvel * angvel) / (2 * drone.getTurnDeceleration());

        float accel = 0f;
        if (rotationAngleDelta < 0f) {
            if (-decelerationAngleAbs < rotationAngleDelta) {
                accel += drone.getTurnDeceleration() * engine.getElapsedInLastFrame();
            } else {
                accel -= drone.getTurnAcceleration() * engine.getElapsedInLastFrame();
            }
        } else if (rotationAngleDelta > 0f) {
            if (decelerationAngleAbs > rotationAngleDelta) {
                accel -= drone.getTurnDeceleration() * engine.getElapsedInLastFrame();
            } else {
                accel += drone.getTurnAcceleration() * engine.getElapsedInLastFrame();
            }
        }

        angvel += accel;

        MathUtils.clamp(angvel, -drone.getMaxTurnRate(), drone.getMaxTurnRate());

        drone.setAngularVelocity(angvel);
    }

    public static void rotateToFacingJerky(PSEDrone drone, float targetAngle) {
        float delta = MathUtils.getShortestRotation(drone.getFacing(), targetAngle);
        drone.setFacing(drone.getFacing() + delta * 0.1f);
    }

    public static void attemptToLand(ShipAPI ship, PSEDrone drone, float amount, IntervalUtil delayBeforeLandingTracker, CombatEngineAPI engine) {
        delayBeforeLandingTracker.advance(amount);
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

    public static void attemptToLandAsExtra(ShipAPI ship, PSEDrone drone) {
        if (!drone.isLanding() && MathUtils.getDistance(drone, ship) < ship.getCollisionRadius()) {
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
        for (ShipAPI ally : AIUtils.getNearbyAllies(drone, focusWeaponRange)) {
            if (ally.getCollisionClass() == CollisionClass.FIGHTER || ally.getCollisionClass() == CollisionClass.NONE) {
                continue;
            }

            float distance = MathUtils.getDistance(ally, drone);
            if (MathUtils.getDistance(target, drone) < distance) {
                continue;
            }

            if (CollisionUtils.getCollisionPoint(drone.getLocation(), target.getLocation(), ally) != null) {
                return true;
            }

            /*float frac = (focusWeaponRange - distance) / focusWeaponRange;
            float arc = (frac * 20f);// + arcDeviation;
            if (arc <= 0f) arc = 1f;

            float num = distance - focusWeaponRange;
            num *= num * num * num;
            float div = focusWeaponRange * focusWeaponRange * focusWeaponRange * focusWeaponRange;
            float arc = (num / div) * arcDeviation;

            if (!PSE_MiscUtils.isEntityInArc(ally, drone.getLocation(), arcFacing, arc)) {
                continue;
            }

            areFriendliesInFiringArc = true;*/
        }
        return false;
    }

    public static ShipAPI getAlternateHost(PSEDrone drone, String prefix, CombatEngineAPI engine) {
        engine = Global.getCombatEngine();
        List<ShipAPI> allies = AIUtils.getNearbyAllies(drone, 4000f);
        if (allies.isEmpty()) {
            return null;
        }
        float dist = Float.MAX_VALUE;
        ShipAPI host = null;
        for (ShipAPI ship : allies) {
            for (String key : engine.getCustomData().keySet()) {
                if (key.contentEquals(prefix + ship.hashCode())) {
                    float temp = MathUtils.getDistance(drone, ship);
                    if (temp < dist) {
                        dist = temp;
                        host = ship;
                    }
                }
            }
        }
        return host;
    }

    public static void deleteDrone (PSEDrone drone, CombatEngineAPI engine) {
        //engine.removeEntity(drone);
        //engine.spawnExplosion(drone.getLocation(), drone.getVelocity(), DRONE_EXPLOSION_COLOUR, drone.getMass(), 1.5f);
        engine.applyDamage(
                drone,
                drone.getLocation(),
                10000f,
                DamageType.HIGH_EXPLOSIVE,
                0f,
                true,
                false,
                null,
                false
        );
    }
}