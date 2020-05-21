package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import data.scripts.PSEModPlugin;
import data.scripts.shipsystems.PSE_DroneCorona;
import data.scripts.util.PSE_DroneUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Objects;

import static data.scripts.util.PSE_DroneUtil.getNearestEnemyFighter;
import static data.scripts.util.PSE_DroneUtil.getNearestEnemyNonFighterShip;

public class PSE_DroneCoronaDroneAI implements ShipAIPlugin {

    private final PSEDroneAPI drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;

    static JSONObject droneSystemSpecJson;
    static JSONArray droneBehaviorSpecJson;
    private boolean hasLoadedJson = false;

    //USED FOR MOVEMENT AND POSITIONING AI
    private float initialOrbitAngle;
    private float focusModeOrbitAngle;
    private float orbitRadius;
    private WeaponAPI landingLocation;
    private IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    private IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);

    //USED FOR SYSTEM ACTIVATION AI
    private final String PD_WEAPON_ID = "pdlaser";
    private float PDWeaponRange;
    private float focusWeaponRange;
    private boolean isInFocusMode;

    private String UNIQUE_SYSTEM_ID;

    public PSE_DroneCoronaDroneAI(PSEDroneAPI passedDrone) {
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
            String FOCUS_WEAPON_ID = "hil";
            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                PDWeaponRange = weapon.getRange();
            } else if (weapon.getId().contentEquals(FOCUS_WEAPON_ID)) {
                focusWeaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = "PSE_droneCorona_" + ship.hashCode();

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
    }

    @Override
    public void advance(float amount) {
        ////////////////////
        ///INITIALISATION///
        ///////////////////


        float droneFacing = drone.getFacing();

        //get ship system object
        PSE_DroneCorona shipDroneCoronaSystem = (PSE_DroneCorona) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneCoronaSystem == null) {
            return;
        }

        int droneIndex = shipDroneCoronaSystem.getIndex(drone);

        PSE_DroneCorona.CoronaDroneOrders coronaDroneOrders = shipDroneCoronaSystem.getDroneOrders();

        //JSON loading
        if (!hasLoadedJson) {
            JSONObject droneConfigPerIndexJsonObject = null;
            try {
                droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(droneIndex);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                initialOrbitAngle = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("initialOrbitAngle");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                focusModeOrbitAngle = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("focusModeOrbitAngle");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                orbitRadius = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("orbitRadius") + ship.getShieldRadiusEvenIfNoShield();
            } catch (JSONException e) {
                e.printStackTrace();
            }
             hasLoadedJson = true;
        }

        float sanity = 1f;


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //PRIORITISE TARGET, SET LOCATION
        Vector2f targetedLocation;
        if (coronaDroneOrders.equals(PSE_DroneCorona.CoronaDroneOrders.ATTACK)) {
            targetedLocation = getTargetLocation(true, true, false);
        } else {
            targetedLocation = getTargetLocation(false, false, false);
        }


        //ROTATION
        PSE_DroneUtil.rotateToTarget(ship, drone, targetedLocation, droneFacing, 0.1f);

        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////


        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle;
        float radius;
        Vector2f movementTargetLocation;
        switch (coronaDroneOrders) {
            case DEPLOY:
                angle = initialOrbitAngle + ship.getFacing();

                radius = orbitRadius;

                landingLocation = null;
                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                isInFocusMode = false;

                break;
            case RECALL:
                PSE_DroneUtil.attemptToLand(ship, drone, amount, delayBeforeLandingTracker);

                if (landingLocation == null) {
                    landingLocation = shipDroneCoronaSystem.getPlugin().getLandingBayWeaponAPI();
                }

                movementTargetLocation = landingLocation.getLocation();

                isInFocusMode = false;

                break;
            case ATTACK:
                angle = focusModeOrbitAngle + ship.getFacing();

                radius = orbitRadius;

                landingLocation = null;
                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                isInFocusMode = true;

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        PSE_DroneUtil.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker, 2f);


        ///////////////////////////////////
        ///DRONE WEAPON ACTIVATION LOGIC///
        ///////////////////////////////////


        if (isInFocusMode) {
            for (WeaponAPI weapon : drone.getAllWeapons()) {
                if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                    weapon.disable(true);
                }
            }

            if (droneIndex == 0 && !drone.getSystem().isStateActive()) {
                drone.useSystem();
            }
            } else {
            for (WeaponAPI weapon : drone.getAllWeapons()) {
                if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                    weapon.repair();
                }
            }

            if (droneIndex == 0 && drone.getSystem().isStateActive()) {
                drone.useSystem();
            }
        }
    }

    public Vector2f getTargetLocation(boolean ignoreMissiles, boolean ignoreFighters, boolean ignoreShips) {
        //GET NEARBY OBJECTS TO SHOOT AT priority missiles > fighters > ships

        MissileAPI droneTargetMissile = null;
        if (!ignoreMissiles) {
            //get missile close to mothership
            MissileAPI nearestEnemyMissile = AIUtils.getNearestEnemyMissile(ship);

            //select missile target, or null if none in range
            if (nearestEnemyMissile != null && MathUtils.getDistance(drone, nearestEnemyMissile) < PDWeaponRange) {
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
            if (nearestEnemyShip != null && MathUtils.getDistance(drone, nearestEnemyShip) < focusWeaponRange) {
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
            if (nearestEnemyFighter != null && MathUtils.getDistance(drone, nearestEnemyFighter) < PDWeaponRange) {
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

    /*
    public void doIndependentBehaviour(float facing, float sanity) {
    }*/

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


