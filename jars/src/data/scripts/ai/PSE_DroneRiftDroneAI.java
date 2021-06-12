package data.scripts.ai;

import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.shipsystems.PSE_DroneRift;
import data.scripts.util.PSE_DroneAIUtils;
import data.scripts.util.PSE_SpecLoadingUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PSE_DroneRiftDroneAI extends PSE_BaseDroneAI {
    private final float[] fieldOrbitRadiusArray;
    private final float[] fieldOrbitSpeedArray;
    private final float[] defenceOrbitAngleArray;
    private final float[] defenceFacingArray;
    private final float[] defenceOrbitRadiusArray;
    private float fieldOrbitAngle;
    private float fieldOrbitRadius;
    private float defenceOrbitAngle;
    private float defenceFacing;
    private float defenceOrbitRadius;
    private WeaponSlotAPI landingSlot;
    private PSE_DroneRift.RiftDroneOrders orders;

    public PSE_DroneRiftDroneAI(PSEDrone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        super(passedDrone, baseDroneSystem);

        fieldOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_RiftSpecLoading.getFieldOrbitRadiusArray();
        fieldOrbitSpeedArray = PSE_SpecLoadingUtils.PSE_RiftSpecLoading.getFieldOrbitSpeedArray();
        defenceOrbitAngleArray = PSE_SpecLoadingUtils.PSE_RiftSpecLoading.getDefenceOrbitAngleArray();
        defenceFacingArray = PSE_SpecLoadingUtils.PSE_RiftSpecLoading.getDefenceFacingArray();
        defenceOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_RiftSpecLoading.getDefenceOrbitRadiusArray();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        PSE_DroneRift droneRiftSystem = (PSE_DroneRift) engine.getCustomData().get(getUniqueSystemID());
        if (droneRiftSystem == null) {
            return;
        }
        baseDroneSystem = droneRiftSystem;

        //assign specific values
        droneIndex = baseDroneSystem.getIndex(drone);

        fieldOrbitRadius = fieldOrbitRadiusArray[droneIndex];
        float fieldOrbitSpeed = fieldOrbitSpeedArray[droneIndex];
        defenceOrbitAngle = defenceOrbitAngleArray[droneIndex];
        defenceFacing = defenceFacingArray[droneIndex];
        defenceOrbitRadius = defenceOrbitRadiusArray[droneIndex];

        orders = droneRiftSystem.getDroneOrders();

        switch (orders) {
            case DEFENCE:
                List<PSEDrone> deployedDrones = baseDroneSystem.deployedDrones;
                float angleDivisor = 360f / deployedDrones.size();
                fieldOrbitAngle = droneIndex * angleDivisor;

                delayBeforeLandingTracker.setElapsed(0f);

                landingSlot = null;
                break;
            case ECCM_ARRAY:
                delayBeforeLandingTracker.setElapsed(0f);

                landingSlot = null;

                fieldOrbitAngle += fieldOrbitSpeed * amount;
                break;
            case RECALL:
                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }
                break;
        }

        doRotationTargeting();

        Vector2f movementTargetLocation = getMovementTargetLocation(amount);
        if (movementTargetLocation != null) {
            PSE_DroneAIUtils.move(drone, drone.getFacing(), movementTargetLocation);
        }
    }

    @Override
    protected Vector2f getMovementTargetLocation(float amount) {
        float angle;
        float radius;
        Vector2f movementTargetLocation;

        switch (orders) {
            case DEFENCE:
                angle = defenceOrbitAngle + ship.getFacing();
                radius = defenceOrbitRadius + ship.getShieldRadiusEvenIfNoShield();
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                break;
            case ECCM_ARRAY:
                angle = fieldOrbitAngle + ship.getFacing();
                radius = fieldOrbitRadius + ship.getShieldRadiusEvenIfNoShield();
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                break;
            case RECALL:
                if (landingSlot == null) {
                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);
                break;
            default:
                movementTargetLocation = ship.getMouseTarget();
        }

        return movementTargetLocation;
    }

    @Override
    protected void doRotationTargeting() {
        float targetFacing;

        switch (orders) {
            case DEFENCE:
                targetFacing = ship.getFacing() + defenceFacing;
                break;
            case ECCM_ARRAY:
                targetFacing = ship.getFacing() + fieldOrbitAngle;
                break;
            case RECALL:
            default:
                targetFacing = ship.getFacing();
                break;
        }

        PSE_DroneAIUtils.rotateToFacing(drone, targetFacing, engine);
    }
}