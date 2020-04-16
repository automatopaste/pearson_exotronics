package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import data.scripts.PSEModPlugin;
import data.scripts.shipsystems.PSE_DroneBastion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static data.scripts.util.PSE_DroneUtil.getNearestEnemyFighter;
import static data.scripts.util.PSE_DroneUtil.getNearestEnemyNonFighterShip;

public class PSE_DroneBastionDroneAI implements ShipAIPlugin {

    private final PSEDroneAPI drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;

    static JSONObject droneSystemSpecJson;
    static JSONArray droneBehaviorSpecJson;
    private boolean hasLoadedJson = false;

    //USED FOR MOVEMENT AND POSITIONING AI
    private float frontOrbitAngle;
    private float cardinalOrbitAngle;
    private float orbitRadius;
    float rotationFromFacingToLocationAngle = 0f;
    float rotationFromVelocityToLocationAngle = 0f;
    float droneAngleToTargetedLoc;
    WeaponAPI landingLocation;
    IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);

    //USED FOR SYSTEM ACTIVATION AI
    private final String WEAPON_ID = "pdlaser";
    private float weaponRange;

    private String DRONE_UNIQUE_ID;
    private String UNIQUE_SYSTEM_ID;

    public PSE_DroneBastionDroneAI(PSEDroneAPI passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        droneSystemSpecJson = PSEModPlugin.droneBastionSpecJson;

        try {
            droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("droneBehavior");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (WeaponAPI weapon : drone.getAllWeapons()) {
            if (weapon.getId().contentEquals(WEAPON_ID)) {
                weaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = "PSE_droneBastion_" + ship.hashCode();
    }

    @Override
    public void advance(float amount) {
        ////////////////////
        ///INITIALISATION///
        ///////////////////


        float droneFacing = drone.getFacing();
        float shipFacing = ship.getFacing();

        //get ship system object
        PSE_DroneBastion shipDroneBastionSystem = (PSE_DroneBastion) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneBastionSystem == null) {
            return;
        }

        int droneIndex = shipDroneBastionSystem.getIndex(drone);

        PSE_DroneBastion.BastionDroneOrders bastionDroneOrders = shipDroneBastionSystem.getDroneOrders();

        //JSON loading
        if (!hasLoadedJson) {
            JSONObject droneConfigPerIndexJsonObject = null;
            try {
                droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(droneIndex);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                frontOrbitAngle = droneConfigPerIndexJsonObject.getInt("initialOrbitAngle");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                cardinalOrbitAngle = droneConfigPerIndexJsonObject.getInt("bastionModeOrbitAngle");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                orbitRadius = droneConfigPerIndexJsonObject.getInt("orbitRadius") + ship.getShieldRadiusEvenIfNoShield();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            hasLoadedJson = true;
        }


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //needs no special targeting behaviour
        Vector2f targetedLocation;
        targetedLocation = getTargetLocation(false, false, false);

        //ROTATION
        float facing = frontOrbitAngle + shipFacing;
        if (bastionDroneOrders.equals(PSE_DroneBastion.BastionDroneOrders.CARDINAL)) {
            rotateToTarget(facing, droneFacing, 0.1f);
        } else if (bastionDroneOrders.equals(PSE_DroneBastion.BastionDroneOrders.FRONT)) {
            rotateToTarget(targetedLocation, droneFacing, 0.1f);
        }


        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////


        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle;
        Vector2f movementTargetLocation;
        switch (bastionDroneOrders) {
            case CARDINAL:
                angle = cardinalOrbitAngle + shipFacing;

                landingLocation = null;
                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);

                break;
            case RECALL:
                attemptToLand(amount);

                if (landingLocation == null) {
                    landingLocation = shipDroneBastionSystem.getPlugin().getLandingBayWeaponAPI();
                }

                movementTargetLocation = landingLocation.getLocation();

                break;
            case FRONT:
                angle = frontOrbitAngle + shipFacing;

                landingLocation = null;
                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        move(droneFacing, movementTargetLocation);
    }

    //FUNCTIONS
    public void rotateToTarget(Vector2f targetedLocation, float droneFacing, float rotationMagnitude) {
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
        drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);
    }

    public void rotateToTarget(float absoluteFacingTargetAngle, float droneFacing, float rotationMagnitude) {
        //ROTATION: THE SEQUEL

        //FIND ANGLE THAT CAN BE DECELERATED FROM CURRENT ANGULAR VELOCITY TO ZERO theta = v^2 / 2 (not actually used atm)
        //float decelerationAngle = (float) (Math.pow(drone.getAngularVelocity(), 2) / (2 * drone.getTurnDeceleration()));

        //point at target, if that doesn't exist then point in direction of mothership facing
        float rotationAngleDelta = MathUtils.getShortestRotation(droneFacing, absoluteFacingTargetAngle);

        //ROTATE TOWARDS TARGET would prefer to use shipcommands but this is more reliable
        drone.setFacing(drone.getFacing() + rotationAngleDelta * rotationMagnitude);
    }


    public Vector2f getTargetLocation(boolean ignoreMissiles, boolean ignoreFighters, boolean ignoreShips) {
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

    public void move(float droneFacing, Vector2f movementTargetLocation) {
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

    //OVERRIDES

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
    public ShipAIConfig getConfig() {
        return null;
    }

    //not relevant
    @Override
    public void setDoNotFireDelay(float amount) {
    }

    //called when AI activated on player ship
    @Override
    public void forceCircumstanceEvaluation() {
    }
}
