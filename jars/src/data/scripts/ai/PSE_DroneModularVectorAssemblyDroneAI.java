package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import data.scripts.util.PSE_DroneUtils;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PSE_DroneModularVectorAssemblyDroneAI implements ShipAIPlugin {

    private final PSEDrone drone;
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private boolean loaded = false;

    //USED FOR MOVEMENT AND POSITIONING AI
    private static float[] defenceOrbitAngleArray;
    private static float[] clampedOrbitAngleArray;
    private static float[] defenceOrbitRadiusArray;
    private static float[] clampedOrbitRadiusArray;
    private static float[] clampedFacingOffsetArray;
    private IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    private IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    private WeaponSlotAPI landingSlot;

    //USED FOR SYSTEM ACTIVATION AI
    private static final String WEAPON_ID = "pdlaser";
    private float weaponRange;

    private String UNIQUE_SYSTEM_ID;

    public PSE_DroneModularVectorAssemblyDroneAI(PSEDrone passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        for (WeaponAPI weapon : drone.getAllWeapons()) {
            if (weapon.getId().contentEquals(WEAPON_ID)) {
                weaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = PSE_DroneModularVectorAssembly.UNIQUE_SYSTEM_PREFIX + ship.hashCode();

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
    }

    @Override
    public void advance(float amount) {
        this.engine = Global.getCombatEngine();

        if (engine.isPaused()) {
            return;
        }
        if (drone == null) {
            return;
        }


        ////////////////////
        ///INITIALISATION///
        ///////////////////


        float sanity = 1f;

        if (ship == null || !ship.isAlive()) {
            landingSlot = null;

            ship = PSE_DroneUtils.getAlternateHost(drone, PSE_DroneModularVectorAssembly.UNIQUE_SYSTEM_PREFIX, engine);

            if (ship == null) {
                PSE_DroneUtils.deleteDrone(drone, engine);
                return;
            }
        }

        float droneFacing = drone.getFacing();
        float shipFacing = ship.getFacing();

        //get ship system object
        PSE_DroneModularVectorAssembly shipDroneMVASystem = (PSE_DroneModularVectorAssembly) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneMVASystem == null) {
            return;
        }
        if (!shipDroneMVASystem.getDeployedDrones().contains(drone)) {
            shipDroneMVASystem.getDeployedDrones().add(drone);
        }

        //config
        if (!loaded) {
            defenceOrbitAngleArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitAngleArray();
            clampedOrbitAngleArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitAngleArray();
            defenceOrbitRadiusArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitRadiusArray();
            clampedOrbitRadiusArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitRadiusArray();
            clampedFacingOffsetArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedFacingOffsetArray();

            loaded = true;
        }

        //assign specific values
        int droneIndex = shipDroneMVASystem.getIndex(drone);
        if (droneIndex == -1) {
            if (landingSlot == null) {
                landingSlot = shipDroneMVASystem.getPlugin().getLandingBayWeaponSlotAPI();
            }

            Vector2f movementTargetLocation = landingSlot.computePosition(ship);

            PSE_DroneUtils.move(drone, drone.getFacing(), movementTargetLocation, 1f, velocityRotationIntervalTracker);

            Vector2f to = Vector2f.sub(movementTargetLocation, drone.getLocation(), new Vector2f());
            float angle = VectorUtils.getFacing(to);
            PSE_DroneUtils.rotateToFacing(drone, angle, engine);

            PSE_DroneUtils.attemptToLandAsExtra(ship, drone);
            return;
        }
        float defenceOrbitAngle = defenceOrbitAngleArray[droneIndex];
        float clampedOrbitAngle = clampedOrbitAngleArray[droneIndex];
        float defenceOrbitRadius = defenceOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
        float clampedOrbitRadius = clampedOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
        float clampedFacingOffset = clampedFacingOffsetArray[droneIndex];

        //get orders
        PSE_DroneModularVectorAssembly.ModularVectorAssemblyDroneOrders droneOrders = shipDroneMVASystem.getDroneOrders();


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //needs no special targeting behaviour
        CombatEntityAPI target = PSE_DroneUtils.getEnemyTarget(ship, drone, weaponRange, false, false, false, 120f);


        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////


        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        boolean isClamped = false;
        float angle;
        Vector2f movementTargetLocation;
        switch (droneOrders) {
            case TARGETING:
                angle = defenceOrbitAngle + shipFacing;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), defenceOrbitRadius, angle);
                landingSlot = null;

                //ROTATION
                if (target != null) {
                    PSE_DroneUtils.rotateToTarget(ship, drone, target.getLocation(), engine);
                } else {
                    PSE_DroneUtils.rotateToFacing(drone, shipFacing, engine);
                }

                break;
            case RECALL:
                PSE_DroneUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = shipDroneMVASystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                //ROTATION
                PSE_DroneUtils.rotateToFacing(drone, shipFacing, engine);

                movementTargetLocation = landingSlot.computePosition(ship);

                break;
            case CLAMPED:
                angle = clampedOrbitAngle + shipFacing;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), clampedOrbitRadius, angle);

                landingSlot = null;

                isClamped = MathUtils.getDistance(drone, movementTargetLocation) == 0f;

                //ROTATION
                float targetRotationAngle = shipFacing + clampedFacingOffset;

                if (ship.getEngineController().isTurningLeft()) {
                    targetRotationAngle -= 35f;
                } else if (ship.getEngineController().isTurningRight()) {
                    targetRotationAngle += 35f;
                }

                PSE_DroneUtils.rotateToFacing(drone, targetRotationAngle, engine);

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        if (isClamped) {
            PSE_DroneUtils.snapToLocation(drone, movementTargetLocation);

            ship.getMutableStats().getAcceleration().modifyFlat(this.toString(), 20f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat(this.toString(), 25f);
            ship.getMutableStats().getDeceleration().modifyFlat(this.toString(), 20f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat(this.toString(), 10f);
            ship.getMutableStats().getMaxSpeed().modifyFlat(this.toString(), 10f);

            drone.getEngineController().extendFlame(this, 8f, 1.5f, 15f);
        } else {
            PSE_DroneUtils.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker);
            ship.getMutableStats().getAcceleration().unmodify(this.toString());
            ship.getMutableStats().getTurnAcceleration().unmodify(this.toString());
            ship.getMutableStats().getDeceleration().unmodify(this.toString());
            ship.getMutableStats().getMaxSpeed().unmodify(this.toString());
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
