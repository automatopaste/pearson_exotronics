package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import data.scripts.util.PSE_DroneAIUtils;
import data.scripts.util.PSE_SpecLoadingUtils;
import org.lazywizard.lazylib.MathUtils;
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

        defenceOrbitAngleArray = PSE_SpecLoadingUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitAngleArray();
        clampedOrbitAngleArray = PSE_SpecLoadingUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitAngleArray();
        defenceOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_ModularVectorAssemblySpecLoading.getDefenceOrbitRadiusArray();
        clampedOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_ModularVectorAssemblySpecLoading.getClampedOrbitRadiusArray();
        clampedFacingOffsetArray = PSE_SpecLoadingUtils.PSE_ModularVectorAssemblySpecLoading.getClampedFacingOffsetArray();
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

        Vector2f movementTargetLocation = getMovementTargetLocation(amount);

        switch (droneOrders) {
            case TARGETING:
                break;
            case CLAMPED:
                delayBeforeLandingTracker.setElapsed(0f);
                landingSlot = null;

                if (MathUtils.getDistance(drone, movementTargetLocation) == 0f) {
                    PSE_DroneAIUtils.snapToLocation(drone, movementTargetLocation);

                    ship.getMutableStats().getAcceleration().modifyFlat(this.toString(), 20f);
                    ship.getMutableStats().getTurnAcceleration().modifyFlat(this.toString(), 25f);
                    ship.getMutableStats().getDeceleration().modifyFlat(this.toString(), 20f);
                    ship.getMutableStats().getMaxTurnRate().modifyFlat(this.toString(), 10f);
                    ship.getMutableStats().getMaxSpeed().modifyFlat(this.toString(), 10f);

                    drone.getEngineController().extendFlame(this, 5f, 1f, 8f);
                } else {
                    PSE_DroneAIUtils.move(drone, droneFacing, movementTargetLocation);
                    ship.getMutableStats().getAcceleration().unmodify(this.toString());
                    ship.getMutableStats().getTurnAcceleration().unmodify(this.toString());
                    ship.getMutableStats().getDeceleration().unmodify(this.toString());
                    ship.getMutableStats().getMaxSpeed().unmodify(this.toString());
                }

                break;
            case RECALL:
                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                break;
        }

        doRotationTargeting();

        PSE_DroneAIUtils.move(drone, drone.getFacing(), movementTargetLocation);
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
                if (landingSlot == null) {
                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

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
                PSE_DroneAIUtils.rotateToFacing(drone, shipFacing, engine);
                break;
            case CLAMPED:
                float targetRotationAngle = shipFacing + clampedFacingOffset;

                if (ship.getEngineController().isTurningLeft()) {
                    targetRotationAngle -= 35f;
                } else if (ship.getEngineController().isTurningRight()) {
                    targetRotationAngle += 35f;
                }

                PSE_DroneAIUtils.rotateToFacing(drone, targetRotationAngle, engine);
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