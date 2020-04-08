package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import data.scripts.PSEModPlugin;
import data.scripts.shipsystems.PSE_droneCorona;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static org.lazywizard.lazylib.combat.AIUtils.getEnemiesOnMap;

public class PSE_droneCoronaDroneAI implements ShipAIPlugin {

    private final PSEDroneAPI drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;

    static JSONObject droneSystemSpecJson;
    static JSONArray droneBehaviorSpecJson;

    //USED FOR MOVEMENT AND POSITIONING AI
    private float initialOrbitAngle;
    private float focusModeOrbitAngle;
    private float orbitRadius;
    float rotationFromFacingToLocationAngle = 0f;
    float rotationFromVelocityToLocationAngle = 0f;
    float droneAngleToTargetedLoc;
    WeaponAPI landingLocation;
    IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);

    //USED FOR SYSTEM ACTIVATION AI
    private final String PD_WEAPON_ID = "pdlaser";
    private final String FOCUS_WEAPON_ID = "hil";
    private float PDWeaponRange;
    private float focusWeaponRange;
    private boolean isInFocusMode;
    private boolean isAvoidingFriendlyFire = false;

    private String DRONE_UNIQUE_ID;
    private String UNIQUE_SYSTEM_ID;

    public PSE_droneCoronaDroneAI (FleetMemberAPI member, PSEDroneAPI passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        droneSystemSpecJson = PSEModPlugin.droneCoronaSpecJson;

        try {
            droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("droneBehavior");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();

        for (WeaponAPI weapon : droneWeapons) {
            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                PDWeaponRange = weapon.getRange();
            } else if (weapon.getId().contentEquals(FOCUS_WEAPON_ID)) {
                focusWeaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = "PSE_droneCorona_" + ship.hashCode();
    }

    @Override
    public void advance(float amount) {
        ////////////////////
        ///INITIALISATION///
        ///////////////////




        float droneFacing = drone.getFacing();

        //get ship system object
        PSE_droneCorona shipDroneCoronaSystem = (PSE_droneCorona) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneCoronaSystem == null) {
            return;
        }

        int droneIndex = shipDroneCoronaSystem.getIndex(drone);

        PSE_droneCorona.CoronaDroneOrders coronaDroneOrders = shipDroneCoronaSystem.getDroneOrders();

        JSONObject droneConfigPerIndexJsonObject = null;
        try {
            droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(droneIndex);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            initialOrbitAngle = droneConfigPerIndexJsonObject.getInt("initialOrbitAngle");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            focusModeOrbitAngle = droneConfigPerIndexJsonObject.getInt("focusModeOrbitAngle");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            orbitRadius = droneConfigPerIndexJsonObject.getInt("orbitRadius") + ship.getShieldRadiusEvenIfNoShield();
        } catch (JSONException e) {
            e.printStackTrace();
        }




        /////////////////////////
        ///TARGETING BEHAVIOUR///
        ////////////////////////




        //GET NEARBY OBJECTS TO SHOOT AT

        //get missile close to mothership
        MissileAPI nearestEnemyMissile = AIUtils.getNearestEnemyMissile(ship);

        //get non-fighter ship close to mothership
        ShipAPI nearestEnemyShip = getNearestEnemyNonFighterShip(ship);

        //get fighter close to drone
        ShipAPI nearestEnemyFighter = getNearestEnemyFighter(drone);


        //CHOOSE TARGET TO SHOOT AT, PRIORITY MISSILES > SHIPS

        //select missile target, or null if none in range
        MissileAPI droneTargetMissile;
        if (nearestEnemyMissile != null && MathUtils.getDistance(drone, nearestEnemyMissile) < PDWeaponRange) {
            droneTargetMissile = nearestEnemyMissile;
        } else {
            droneTargetMissile = null;
        }

        //select non-fighter ship target, or null if none in range
        ShipAPI droneTargetShip;
        if (nearestEnemyShip != null && MathUtils.getDistance(drone, nearestEnemyShip) < focusWeaponRange) {
            droneTargetShip = nearestEnemyShip;
        } else {
            droneTargetShip = null;
        }

        //select fighter target, or null if none in range
        ShipAPI droneTargetFighter;
        if (nearestEnemyFighter != null && MathUtils.getDistance(drone, nearestEnemyFighter) < PDWeaponRange) {
            droneTargetFighter = nearestEnemyFighter;
        } else {
            droneTargetFighter = null;
        }


        //PRIORITISE TARGET, SET LOCATION
        Vector2f targetedLocation;
        if (droneTargetMissile != null && !coronaDroneOrders.equals(PSE_droneCorona.CoronaDroneOrders.ATTACK)) {
            targetedLocation = droneTargetMissile.getLocation();
        } else if (droneTargetShip != null) {
            targetedLocation = droneTargetShip.getLocation();
        } else if (droneTargetFighter != null && !coronaDroneOrders.equals(PSE_droneCorona.CoronaDroneOrders.ATTACK)) {
            targetedLocation = droneTargetFighter.getLocation();
        } else {
            targetedLocation = null;
        }


        //ROTATION

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        //float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));

        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta;
        if (targetedLocation != null) {
            //GET ABSOLUTE ANGLE FROM DRONE TO TARGETED LOCATION
            Vector2f droneToTargetedLocDir = VectorUtils.getDirectionalVector(drone.getLocation(), targetedLocation);
            droneAngleToTargetedLoc = VectorUtils.getFacing(droneToTargetedLocDir); //ABSOLUTE 360 ANGLE

            rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, droneAngleToTargetedLoc);
        } else {
            rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, ship.getFacing());
        }

        //ROTATE TOWARDS TARGET would prefer to use shipcommands but this is more reliable
        drone.setFacing(drone.getFacing() + rotationAngleDelta * 0.1f);




        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////




        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle;
        float radius;
        Vector2f movementTargetLocation;
        switch (coronaDroneOrders) {
            case DEPLOY:
                angle = initialOrbitAngle;
                angle += ship.getFacing();

                radius = orbitRadius;

                landingLocation = null;

                 movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                isInFocusMode = false;

                break;
            case RECALL:
                attemptToLand(amount);

                if (landingLocation == null) {
                    landingLocation = shipDroneCoronaSystem.getPlugin().getLandingBayWeaponAPI();
                }

                movementTargetLocation = landingLocation.getLocation();

                isInFocusMode = false;

                break;
            case ATTACK:
                angle = focusModeOrbitAngle;
                angle += ship.getFacing();

                radius = orbitRadius;

                landingLocation = null;

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                isInFocusMode = true;

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }
        //The bones of the movement AI are below, all it needs is a target vector location to move to


        //GET USEFUL VALUES
        float angleFromDroneToTargetLocation = VectorUtils.getAngle(drone.getLocation(), movementTargetLocation); //ABSOLUTE 360 ANGLE

        float droneVelocityAngle = VectorUtils.getFacing(drone.getVelocity()); //ABSOLUTE 360 ANGLE

        rotationFromFacingToLocationAngle = MathUtils.getShortestRotation(droneFacing, angleFromDroneToTargetLocation); //ROTATION ANGLE
        rotationFromVelocityToLocationAngle = MathUtils.getShortestRotation(droneVelocityAngle, angleFromDroneToTargetLocation); //ROTATION ANGLE

        float distanceToTargetLocation = MathUtils.getDistance(drone.getLocation(), movementTargetLocation); //DISTANCE


        //CHECK IF VELOCITY INTERSECTS WITH TARGET LOCATION
        boolean isDronePathIntersectingTarget;
        isDronePathIntersectingTarget = Math.round(droneVelocityAngle) == Math.round(angleFromDroneToTargetLocation);


        //FIND DISTANCE THAT CAN BE DECELERATED FROM CURRENT SPEED TO ZERO s = v^2 / 2a
        float decelerationDistance = (float) (Math.pow(drone.getVelocity().length(), 2)) / (2 * drone.getDeceleration());


        //DECELERATE IF IN THRESHOLD DISTANCE OF TARGET
        if (distanceToTargetLocation <= decelerationDistance) {
            drone.giveCommand(ShipCommand.DECELERATE, null, 0);
        }

        //DO LARGE MOVEMENT IF OVER DISTANCE THRESHOLD
        if (distanceToTargetLocation >= decelerationDistance) {
            rotationFromFacingToLocationAngle = Math.round(rotationFromFacingToLocationAngle);

            //COURSE CORRECTION
            drone.getVelocity().set(VectorUtils.rotate(drone.getVelocity(), rotationFromVelocityToLocationAngle * 0.05f));

            if (!isDronePathIntersectingTarget) {
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
            }
        } else {
            //COURSE CORRECTION
            if (velocityRotationIntervalTracker.intervalElapsed()) {
                drone.getVelocity().set(VectorUtils.rotate(drone.getVelocity(), rotationFromVelocityToLocationAngle));
            }
        }

        //useful debugging things
        //engine.maintainStatusForPlayerShip("PSE_STATUS_KEY" + drone.getId(), "graphics/icons/hullsys/drone_pd_high.png", "DRONE TARGET ANGLE " + droneIndex, rotationFromFacingToLocationAngle + "", true);
        //engine.maintainStatusForPlayerShip("PSE_STATUS_KEY1", "graphics/icons/hullsys/drone_pd_high.png", "DRONE DECEL DISTANCE" + droneIndex, "" + decelerationDistance, true);




        ///////////////////////////////////
        ///DRONE WEAPON ACTIVATION LOGIC///
        ///////////////////////////////////




        if (droneIndex == 0) {
            if (isInFocusMode) {
                if (!drone.getSystem().isStateActive()) {
                    drone.useSystem();
                }
            } else {
                if (drone.getSystem().isStateActive()) {
                    drone.useSystem();
                }
            }
        } else {
            if (isInFocusMode) {
                for (WeaponAPI weapon : drone.getAllWeapons()) {
                    if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                        weapon.disable(true);
                    }
                }
            } else {
                for (WeaponAPI weapon : drone.getAllWeapons()) {
                    if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                        weapon.repair();
                    }
                }
            }
        }
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        ShipwideAIFlags flags = new ShipwideAIFlags();
        flags.setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
        return flags;
    }

    //not relevant
    @Override
    public void cancelCurrentManeuver() {
    }

    //not relevant
    @Override
    public ShipAIConfig getConfig() { return null; }

    //not relevant
    @Override
    public void setDoNotFireDelay(float amount) {
    }

    //called when AI activated on player ship
    @Override
    public void forceCircumstanceEvaluation() {
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

    public void attemptToLand(float amount) {
        delayBeforeLandingTracker.advance(amount);

        if (drone.isLanding()) {
            delayBeforeLandingTracker.setElapsed(0);
            engine.maintainStatusForPlayerShip("PSE_STATUS_KEY_DRONE_LANDING_STATE", "graphics/icons/hullsys/drone_pd_high.png", "LANDING STATUS", "LANDING... ", false);
        } else {
            float round = Math.round((delayBeforeLandingTracker.getIntervalDuration() - delayBeforeLandingTracker.getElapsed()) * 100) / 100f;
            engine.maintainStatusForPlayerShip("PSE_STATUS_KEY_DRONE_LANDING_STATE", "graphics/icons/hullsys/drone_pd_high.png", "LANDING STATUS", "LANDING IN " + round, false);
        }

        if (delayBeforeLandingTracker.intervalElapsed()) {
            drone.beginLandingAnimation(ship);
        }
    }
}

