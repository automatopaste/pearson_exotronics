package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

public class PSE_DroneCitadelDroneAI extends PSE_BaseDroneAI{
    @Override
    public void setDoNotFireDelay(float amount) {

    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return null;
    }

    @Override
    public void cancelCurrentManeuver() {

    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }
//    private final float[] antiFighterOrbitAngleArray;
//    private final float[] antiFighterFacingOffsetArray;
//    private final float[] antiFighterOrbitRadiusArray;
//    private final float[] shieldOrbitRadiusArray;
//    private float antiFighterOrbitAngle;
//    private float shieldOrbitAngle;
//    private float antiFighterOrbitRadius;
//    private float shieldOrbitRadius;
//    private float antiFighterFacingOffset;
//    private PSE_DroneCitadel.CitadelDroneOrders orders;
//
//    public PSE_DroneCitadelDroneAI(PSE_Drone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
//        super(passedDrone, baseDroneSystem);
//
//        antiFighterOrbitAngleArray = PSE_SpecLoadingUtils.PSE_CitadelSpecLoading.getAntiFighterOrbitAngleArray();
//        antiFighterFacingOffsetArray = PSE_SpecLoadingUtils.PSE_CitadelSpecLoading.getAntiFighterFacingOffsetArray();
//        antiFighterOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_CitadelSpecLoading.getAntiFighterOrbitRadiusArray();
//        shieldOrbitRadiusArray = PSE_SpecLoadingUtils.PSE_CitadelSpecLoading.getShieldOrbitRadiusArray();
//    }
//
//    @Override
//    public void advance(float amount) {
//        super.advance(amount);
//
//        PSE_DroneCitadel droneCitadelSystem = (PSE_DroneCitadel) engine.getCustomData().get(getUniqueSystemID());
//        if (droneCitadelSystem == null) {
//            return;
//        }
//        baseDroneSystem = droneCitadelSystem;
//
//        //assign specific values
//        droneIndex = baseDroneSystem.getIndex(drone);
//
//        antiFighterOrbitAngle = antiFighterOrbitAngleArray[droneIndex];
//        antiFighterFacingOffset = antiFighterFacingOffsetArray[droneIndex];
//        antiFighterOrbitRadius = antiFighterOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
//        shieldOrbitRadius = shieldOrbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
//
//        List<PSE_Drone> deployedDrones = baseDroneSystem.deployedDrones;
//        float[] orbitAngleArray = new float[deployedDrones.size()];
//        float angleDivisor = 360f / deployedDrones.size();
//        for (int i = 0; i < deployedDrones.size(); i++) {
//            orbitAngleArray[i] = angleDivisor * i;
//        }
//        shieldOrbitAngle = orbitAngleArray[droneIndex] + droneCitadelSystem.getOrbitAngleBase();
//
//        //get orders
//        orders = droneCitadelSystem.getDroneOrders();
//
//        switch (orders) {
//            case ANTI_FIGHTER:
//            case RECALL:
//                if (drone.getShield().isOn()) {
//                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
//                }
//
//                break;
//            case SHIELD:
//                delayBeforeLandingTracker.setElapsed(0f);
//                landingSlot = null;
//
//                if (!drone.getShield().isOn()) {
//                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
//                }
//
//                break;
//        }
//
//        doRotationTargeting();
//
//        Vector2f movementTargetLocation = getMovementTargetLocation(amount);
//        if (movementTargetLocation != null) {
//            PSE_DroneAIUtils.move(drone, drone.getFacing(), movementTargetLocation);
//        }
//    }
//
//    @Override
//    protected Vector2f getMovementTargetLocation(float amount) {
//        float angle;
//        float shipFacing = ship.getFacing();
//        Vector2f movementTargetLocation;
//        switch (orders) {
//            case ANTI_FIGHTER:
//                angle = antiFighterOrbitAngle + shipFacing;
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), antiFighterOrbitRadius, angle);
//
//                break;
//            case SHIELD:
//                angle = shieldOrbitAngle + shipFacing;
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), shieldOrbitRadius, angle);
//
//                break;
//            case RECALL:
//                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);
//
//                if (landingSlot == null) {
//                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
//                }
//                movementTargetLocation = landingSlot.computePosition(ship);
//
//                break;
//            default:
//                movementTargetLocation = ship.getLocation();
//        }
//
//        return movementTargetLocation;
//    }
//
//    @Override
//    protected void doRotationTargeting() {
//        float targetFacing;
//
//        switch (orders) {
//            case ANTI_FIGHTER:
//                targetFacing = antiFighterOrbitAngle + ship.getFacing() + antiFighterFacingOffset;
//                break;
//            case RECALL:
//                targetFacing = antiFighterOrbitAngle + ship.getFacing();
//                break;
//            case SHIELD:
//                targetFacing = shieldOrbitAngle + ship.getFacing();
//                break;
//            default:
//                targetFacing = 0f;
//                break;
//        }
//
//        PSE_DroneAIUtils.rotateToFacing(drone, targetFacing, engine);
//    }
}