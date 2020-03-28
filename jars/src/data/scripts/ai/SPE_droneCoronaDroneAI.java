package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.SPEDroneAPI;
import data.scripts.SPEModPlugin;
import data.scripts.shipsystems.SPE_droneCorona;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class SPE_droneCoronaDroneAI implements ShipAIPlugin {

    private final SPEDroneAPI drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;


    static JSONObject droneSystemSpecJson;
    static JSONArray droneBehaviorSpecJson;

    //USED FOR MOVEMENT AI
    private float initialOrbitAngle;
    private float focusModeOrbitAngle;
    private float orbitRadius;
    float rotationFromFacingToLocationAngle = 0f;
    float droneAngleToTargetedLoc;

    //USED FOR SYSTEM ACTIVATION AI
    private final String PD_WEAPON_ID = "pdburst";
    private final String FOCUS_WEAPON_ID = "hil";
    private float PDWeaponRange;
    private float focusWeaponRange;
    private boolean isInFocusMode;

    private String DRONE_UNIQUE_ID;
    private SPE_droneCorona.CoronaDroneOrders coronaDroneOrders;
    private String UNIQUE_SYSTEM_ID;

    private SPE_droneCorona shipDroneCoronaSystem;


    public SPE_droneCoronaDroneAI (FleetMemberAPI member, SPEDroneAPI passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        droneSystemSpecJson = SPEModPlugin.droneCoronaSpecJson;

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

        this.UNIQUE_SYSTEM_ID = "SPE_droneCorona_" + ship.hashCode();
    }

    @Override
    public void advance(float amount) {
        float droneFacing = drone.getFacing();

        //get ship system object
        shipDroneCoronaSystem = (SPE_droneCorona) engine.getCustomData().get(UNIQUE_SYSTEM_ID);

        int droneIndex = shipDroneCoronaSystem.getIndex(drone);

        if (droneIndex == -1) {
            //throw new ClassCastException(droneIndex + " " + drone +" "+shipDroneCoronaSystem.getDeployedDrones());
        }

        this.coronaDroneOrders = shipDroneCoronaSystem.getDroneOrders();

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
            orbitRadius = droneConfigPerIndexJsonObject.getInt("orbitRadius") + ship.getShieldRadiusEvenIfNoShield() - drone.getShieldRadiusEvenIfNoShield();
        } catch (JSONException e) {
            e.printStackTrace();
        }




        /////////////////////////
        ///TARGETING BEHAVIOUR///
        ////////////////////////




        //GET NEARBY OBJECTS TO SHOOT AT

        //get missile close to mothership
        MissileAPI nearestEnemyMissile = AIUtils.getNearestEnemyMissile(ship);

        //get ship close to mothership (most likely fighter, but does not discriminate)
        ShipAPI nearestEnemyShip = AIUtils.getNearestEnemy(ship);


        //CHOOSE TARGET TO SHOOT AT, PRIORITY MISSILES > SHIPS

        //select missile target, or null if none in range
        MissileAPI droneTargetMissile;
        if (nearestEnemyMissile != null && MathUtils.getDistance(drone, nearestEnemyMissile) < PDWeaponRange * 3) {
            droneTargetMissile = nearestEnemyMissile;
        } else {
            droneTargetMissile = null;
        }

        //select ship and fighter target, or null if none in range
        ShipAPI droneTargetShip;
        if (nearestEnemyShip != null && MathUtils.getDistance(drone, nearestEnemyShip) < PDWeaponRange * 3) {
            droneTargetShip = nearestEnemyShip;
        } else {
            droneTargetShip = null;
        }


        //PRIORITISE TARGET, SET LOCATION
        Vector2f targetedLocation;
        if (droneTargetMissile != null) {
            targetedLocation = droneTargetMissile.getLocation();
        } else if (droneTargetShip != null) {
            targetedLocation = droneTargetShip.getLocation();
        } else {
            targetedLocation = null;
        }


        //ROTATION

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2a
        float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));

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

        //ROTATE TOWARDS TARGET
        drone.setFacing(drone.getFacing() + rotationAngleDelta * 0.05f);




        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////




        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle = 180f;
        float radius = 0f;
        switch (coronaDroneOrders) {
            case DEPLOY:
                angle = initialOrbitAngle;
                radius = orbitRadius;
                isInFocusMode = false;
                break;
            case RECALL:
                if (!drone.isLanding()) {
                    drone.beginLandingAnimation(ship);
                }
                isInFocusMode = false;
                break;
            case ATTACK:
                angle = focusModeOrbitAngle;
                radius = orbitRadius;
                isInFocusMode = true;
                break;
        }

        angle += ship.getFacing();

        Vector2f movementTargetLocationOnShip = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);


        //GET USEFUL VALUES
        float angleFromDroneToTargetLocation = VectorUtils.getAngle(drone.getLocation(), movementTargetLocationOnShip); //ABSOLUTE 360 ANGLE

        float droneVelocityAngle = VectorUtils.getFacing(drone.getVelocity()); //ABSOLUTE 360 ANGLE

        rotationFromFacingToLocationAngle = MathUtils.getShortestRotation(droneFacing, angleFromDroneToTargetLocation); //ROTATION ANGLE

        float distanceToTargetLocation = MathUtils.getDistance(drone.getLocation(), movementTargetLocationOnShip); //DISTANCE


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
        if (!isDronePathIntersectingTarget && distanceToTargetLocation >= decelerationDistance) {
            //accelerate forwards or backwards
            if (
                    90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > -90f || rotationFromFacingToLocationAngle == 0f
            ) { //between 90 and -90 is an acute angle therefore in front
                drone.giveCommand(ShipCommand.ACCELERATE, null, 0);
            } else if (
                    (180f >= rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > 90f) || (-90f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle >= -180f)
            ){ //falls between 90 to 180 or -90 to -180, which should be obtuse and thus relatively behind
                drone.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            }

            //strafe left or right
            if (
                    180f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > 0f
            ) { //between 0 and 180 (i.e. left)
                drone.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            } else if (
                    0f > rotationFromFacingToLocationAngle && rotationFromFacingToLocationAngle > -180f
            ){ //between 0 and -180 (i.e. right)
                drone.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
            }
        }

        //useful debugging things
        //engine.maintainStatusForPlayerShip("SPE_STATUS_KEY0", "graphics/icons/hullsys/drone_pd_high.png", "DRONE TARGET VECTOR " + droneIndex, "", true);
        //engine.maintainStatusForPlayerShip("SPE_STATUS_KEY1", "graphics/icons/hullsys/drone_pd_high.png", "DRONE VEL ANGLE" + droneIndex, "" + decelerationDistance, true);




        /////////////////////////////
        ///DRONE SYSTEM ACTIVATION///
        /////////////////////////////
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
}
