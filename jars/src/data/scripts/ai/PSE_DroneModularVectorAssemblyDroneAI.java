package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import data.scripts.util.PSE_DroneUtils;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PSE_DroneModularVectorAssemblyDroneAI extends PSE_BaseDroneAI {
    //USED FOR MOVEMENT AND POSITIONING AI
    private static float[] defenceOrbitAngleArray;
    private static float[] clampedOrbitAngleArray;
    private static float[] defenceOrbitRadiusArray;
    private static float[] clampedOrbitRadiusArray;
    private static float[] clampedFacingOffsetArray;
    private float defenceOrbitAngle;
    private float clampedOrbitAngle;
    private float defenceOrbitRadius;
    private float clampedOrbitRadius;
    private float clampedFacingOffset;
    private float droneFacing;
    private PSE_DroneModularVectorAssembly.ModularVectorAssemblyDroneOrders droneOrders;

    public PSE_DroneModularVectorAssemblyDroneAI(PSEDrone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        super(passedDrone, baseDroneSystem);

        defenceOrbitAngleArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitAngleArray();
        clampedOrbitAngleArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitAngleArray();
        defenceOrbitRadiusArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitRadiusArray();
        clampedOrbitRadiusArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitRadiusArray();
        clampedFacingOffsetArray = PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.getClampedFacingOffsetArray();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        droneFacing = drone.getFacing();

        //get ship system object
        PSE_DroneModularVectorAssembly shipDroneMVASystem = (PSE_DroneModularVectorAssembly) engine.getCustomData().get(getUniqueSystemID());
        if (shipDroneMVASystem == null) {
            return;
        }
        baseDroneSystem = shipDroneMVASystem;

        //assign specific values
        int droneIndex = shipDroneMVASystem.getIndex(drone);

        defenceOrbitAngle = defenceOrbitAngleArray[droneIndex];
        clampedOrbitAngle = clampedOrbitAngleArray[droneIndex];
        defenceOrbitRadius = defenceOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
        clampedOrbitRadius = clampedOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
        clampedFacingOffset = clampedFacingOffsetArray[droneIndex];

        //get orders
        droneOrders = shipDroneMVASystem.getDroneOrders();

        switch (droneOrders) {
            case TARGETING:
            case CLAMPED:
                delayBeforeLandingTracker.setElapsed(0f);
                landingSlot = null;

                break;
            case RECALL:
                PSE_DroneUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                break;
        }

        Vector2f movementTargetLocation = getMovementTargetLocation(amount);

        if (MathUtils.getDistance(drone, movementTargetLocation) == 0f) {
            PSE_DroneUtils.snapToLocation(drone, movementTargetLocation);

            ship.getMutableStats().getAcceleration().modifyFlat(this.toString(), 20f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat(this.toString(), 25f);
            ship.getMutableStats().getDeceleration().modifyFlat(this.toString(), 20f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat(this.toString(), 10f);
            ship.getMutableStats().getMaxSpeed().modifyFlat(this.toString(), 10f);

            drone.getEngineController().extendFlame(this, 8f, 1.5f, 15f);
        } else {
            PSE_DroneUtils.move(drone, droneFacing, movementTargetLocation, velocityRotationIntervalTracker);
            ship.getMutableStats().getAcceleration().unmodify(this.toString());
            ship.getMutableStats().getTurnAcceleration().unmodify(this.toString());
            ship.getMutableStats().getDeceleration().unmodify(this.toString());
            ship.getMutableStats().getMaxSpeed().unmodify(this.toString());
        }

        doRotationTargeting();

        PSE_DroneUtils.move(drone, drone.getFacing(), movementTargetLocation, velocityRotationIntervalTracker);
    }

    @Override
    protected Vector2f getMovementTargetLocation(float amount) {
        float angle;
        float shipFacing = ship.getFacing();
        Vector2f movementTargetLocation;

        switch (droneOrders) {
            case TARGETING:
                angle = defenceOrbitAngle + shipFacing;
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), defenceOrbitRadius, angle);

                break;
            case RECALL:
                movementTargetLocation = landingSlot.computePosition(ship);

                break;
            case CLAMPED:
                angle = clampedOrbitAngle + shipFacing;
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), clampedOrbitRadius, angle);

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        return movementTargetLocation;
    }

    @Override
    protected void doRotationTargeting() {
        float shipFacing = ship.getFacing();

        switch (droneOrders) {
            case TARGETING:
            case RECALL:
                PSE_DroneUtils.rotateToFacing(drone, shipFacing, engine);
                break;
            case CLAMPED:
                float targetRotationAngle = shipFacing + clampedFacingOffset;

                if (ship.getEngineController().isTurningLeft()) {
                    targetRotationAngle -= 35f;
                } else if (ship.getEngineController().isTurningRight()) {
                    targetRotationAngle += 35f;
                }

                PSE_DroneUtils.rotateToFacing(drone, targetRotationAngle, engine);
                break;
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

    @Override
    public void cancelCurrentManeuver() {
    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public void forceCircumstanceEvaluation() {
    }
}