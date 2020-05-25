package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import data.scripts.PSEModPlugin;
import data.scripts.shipsystems.PSE_DroneBastion;
import data.scripts.util.PSE_DroneUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Objects;

public class PSE_DroneBastionDroneAI implements ShipAIPlugin {



    private final PSEDroneAPI drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;

    static JSONObject droneSystemSpecJson;
    static JSONArray droneBehaviorSpecJson;
    private boolean hasLoadedJson = false;

    //USED FOR MOVEMENT AND POSITIONING AI
    private float[] cardinalOrbitAngleArray;
    private float[] frontOrbitAngleArray;
    private float[] orbitRadiusArray;
    private IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    private IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    private WeaponSlotAPI landingSlot;

    //USED FOR SYSTEM ACTIVATION AI
    private static final String WEAPON_ID = "pdlaser";
    private float weaponRange;

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

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
    }

    @Override
    public void advance(float amount) {
        if (engine.isPaused()) {
            return;
        }

        ////////////////////
        ///INITIALISATION///
        ///////////////////

        float sanity = 1f;

        float droneFacing = drone.getFacing();
        float shipFacing = ship.getFacing();

        //get ship system object
        PSE_DroneBastion shipDroneBastionSystem = (PSE_DroneBastion) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneBastionSystem == null) {
            return;
        }

        int droneIndex = shipDroneBastionSystem.getIndex(drone);

        PSE_DroneBastion.BastionDroneOrders bastionDroneOrders = shipDroneBastionSystem.getDroneOrders();

        //get number of different drone spots
        int numIndexes = shipDroneBastionSystem.getNumIndexes();

        //JSON config checking
        if (!hasLoadedJson) {
            cardinalOrbitAngleArray = new float[numIndexes];
            frontOrbitAngleArray = new float[numIndexes];
            orbitRadiusArray = new float[numIndexes];
            JSONObject droneConfigPerIndexJsonObject = null;

            for (int i = 0; i < numIndexes; i++) {
                try {
                    droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    cardinalOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("initialOrbitAngle");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    frontOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("focusModeOrbitAngle");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    orbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("orbitRadius") + ship.getShieldRadiusEvenIfNoShield();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            hasLoadedJson = true;
        }

        float cardinalOrbitAngle = cardinalOrbitAngleArray[droneIndex];
        float frontOrbitAngle = frontOrbitAngleArray[droneIndex];
        float orbitRadius = orbitRadiusArray[droneIndex];


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //needs no special targeting behaviour
        Vector2f targetedLocation;
        targetedLocation = PSE_DroneUtil.getTargetLocation(ship, drone, weaponRange, false, false, false);

        //ROTATION
        float facing = frontOrbitAngle + shipFacing;

        if (bastionDroneOrders.equals(PSE_DroneBastion.BastionDroneOrders.CARDINAL)) {
            PSE_DroneUtil.rotateToTarget(drone, facing, droneFacing, 0.1f);
        } else if (bastionDroneOrders.equals(PSE_DroneBastion.BastionDroneOrders.FRONT)) {
            PSE_DroneUtil.rotateToTarget(ship, drone, targetedLocation, droneFacing, 0.1f);
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

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);

                landingSlot = null;

                break;
            case RECALL:
                PSE_DroneUtil.attemptToLand(ship, drone, amount, delayBeforeLandingTracker);

                if (landingSlot == null) {
                    landingSlot = shipDroneBastionSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);

                break;
            case FRONT:
                angle = frontOrbitAngle + shipFacing;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);

                landingSlot = null;

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        PSE_DroneUtil.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker, 2f);
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
