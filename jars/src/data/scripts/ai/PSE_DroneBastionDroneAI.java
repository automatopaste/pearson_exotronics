package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

public class PSE_DroneBastionDroneAI extends PSE_BaseDroneAI {
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
//    //USED FOR MOVEMENT AND POSITIONING AI
//    private final float[] cardinalOrbitAngleArray;
//    private final float[] frontOrbitAngleArray;
//    private final float[] orbitRadiusArray;
//    private float cardinalOrbitAngle;
//    private float frontOrbitAngle;
//    private float orbitRadius;
//    private WeaponSlotAPI landingSlot;
//    private PSE_DroneBastion.BastionDroneOrders orders;
//    private PSE_DroneBastion droneBastionSystem;
//
//    //USED FOR SYSTEM ACTIVATION AI
//    private static final String WEAPON_ID = "pdlaser";
//    private float weaponRange;
//
//    public PSE_DroneBastionDroneAI(PSE_Drone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
//        super(passedDrone, baseDroneSystem);
//
//        for (WeaponAPI weapon : drone.getAllWeapons()) {
//            if (weapon.getId().contentEquals(WEAPON_ID)) {
//                weaponRange = weapon.getRange();
//            }
//        }
//
//        cardinalOrbitAngleArray = PSE_SpecLoadingUtils.PSE_BastionSpecLoading.getCardinalOrbitAngleArray();
//        frontOrbitAngleArray = PSE_SpecLoadingUtils.PSE_BastionSpecLoading.getFrontOrbitAngleArray();
//        orbitRadiusArray = PSE_SpecLoadingUtils.PSE_BastionSpecLoading.getOrbitRadiusArray();
//    }
//
//    @Override
//    public void advance(float amount) {
//        super.advance(amount);
//
//        droneBastionSystem = (PSE_DroneBastion) engine.getCustomData().get(getUniqueSystemID());
//        if (droneBastionSystem == null) {
//            return;
//        }
//        baseDroneSystem = droneBastionSystem;
//
//        //assign specific values
//        int droneIndex = baseDroneSystem.getIndex(drone);
//
//        cardinalOrbitAngle = cardinalOrbitAngleArray[droneIndex];
//        frontOrbitAngle = frontOrbitAngleArray[droneIndex];
//        orbitRadius = orbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
//
//        //get orders
//        orders = droneBastionSystem.getDroneOrders();
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
//            case FRONT:
//                angle = cardinalOrbitAngle + shipFacing;
//
//                delayBeforeLandingTracker.setElapsed(0f);
//
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);
//                landingSlot = null;
//                break;
//            case RECALL:
//                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);
//
//                if (landingSlot == null) {
//                    landingSlot = droneBastionSystem.getPlugin().getLandingBayWeaponSlotAPI();
//                }
//
//                movementTargetLocation = landingSlot.computePosition(ship);
//                break;
//            case CARDINAL:
//                angle = frontOrbitAngle + shipFacing;
//
//                delayBeforeLandingTracker.setElapsed(0f);
//
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);
//
//                landingSlot = null;
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
//        CombatEntityAPI target;
//        target = PSE_DroneAIUtils.getEnemyTarget(ship, drone, weaponRange, false, false, false, 360f);
//
//        //ROTATION
//        float shipFacing = ship.getFacing();
//        float facing = frontOrbitAngle + shipFacing;
//        float droneAngleRelativeToShip = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(ship, drone.getShipAPI()));
//        switch (orders) {
//            case FRONT:
//                if (target != null && PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), droneAngleRelativeToShip, 120f)) {
//                    PSE_DroneAIUtils.rotateToTarget(ship, drone, target.getLocation());
//                } else {
//                    PSE_DroneAIUtils.rotateToFacing(drone, shipFacing, engine);
//                }
//                break;
//            case CARDINAL:
//            case RECALL:
//                if (target != null && PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), droneAngleRelativeToShip, 120f)) {
//                    PSE_DroneAIUtils.rotateToTarget(ship, drone, target.getLocation());
//                } else {
//                    PSE_DroneAIUtils.rotateToFacing(drone, facing, engine);
//                }
//                break;
//        }
//    }
//
//    @Override
//    public boolean needsRefit() {
//        return false;
//    }
//
//    @Override
//    public ShipwideAIFlags getAIFlags() {
//        ShipwideAIFlags flags = new ShipwideAIFlags();
//        flags.setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
//        return flags;
//    }
//
//    @Override
//    public void cancelCurrentManeuver() {
//    }
//
//    @Override
//    public ShipAIConfig getConfig() {
//        return null;
//    }
//
//    @Override
//    public void setDoNotFireDelay(float amount) {
//    }
//
//    @Override
//    public void forceCircumstanceEvaluation() {
//    }
}