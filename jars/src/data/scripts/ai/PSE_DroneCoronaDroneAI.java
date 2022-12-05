package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

public class PSE_DroneCoronaDroneAI extends PSE_BaseDroneAI {
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
//    private final float[] initialOrbitAngleArray;
//    private final float[] focusModeOrbitAngleArray;
//    private final float[] orbitRadiusArray;
//    private float initialOrbitAngle;
//    private float focusModeOrbitAngle;
//    private float orbitRadius;
//    private PSE_DroneCorona.CoronaDroneOrders droneOrders;
//
//    //USED FOR SYSTEM ACTIVATION AI
//    private static final String PD_WEAPON_ID = "pdlaser";
//    private float PDWeaponRange;
//    private float focusWeaponRange;
//    private boolean isInFocusMode;
//
//    public PSE_DroneCoronaDroneAI(PSE_Drone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
//        super(passedDrone, baseDroneSystem);
//
//        List<WeaponAPI> droneWeapons = drone.getAllWeapons();
//
//        for (WeaponAPI weapon : droneWeapons) {
//            String FOCUS_WEAPON_ID = "hil";
//            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
//                PDWeaponRange = weapon.getRange();
//            } else if (weapon.getId().contentEquals(FOCUS_WEAPON_ID)) {
//                focusWeaponRange = weapon.getRange();
//            }
//        }
//
//        initialOrbitAngleArray = PSE_SpecLoadingUtils.PSE_CoronaSpecLoading.getInitialOrbitAngleArray();
//        focusModeOrbitAngleArray = PSE_SpecLoadingUtils.PSE_CoronaSpecLoading.getFocusOrbitAngleArray();
//        orbitRadiusArray = PSE_SpecLoadingUtils.PSE_CoronaSpecLoading.getOrbitRadiusArray();
//    }
//
//    @Override
//    public void advance(float amount) {
//        super.advance(amount);
//
//        PSE_DroneCorona coronaDroneSystem = (PSE_DroneCorona) engine.getCustomData().get(getUniqueSystemID());
//        if (coronaDroneSystem == null) {
//            return;
//        }
//        baseDroneSystem = coronaDroneSystem;
//
//        //assign specific values
//        int droneIndex = coronaDroneSystem.getIndex(drone);
//
//        initialOrbitAngle = initialOrbitAngleArray[droneIndex];
//        focusModeOrbitAngle = focusModeOrbitAngleArray[droneIndex];
//        orbitRadius = orbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();
//
//        //GET DRONE ORDERS STATE
//        droneOrders = coronaDroneSystem.getDroneOrders();
//
//        ///////////////////////////////////
//        ///DRONE WEAPON ACTIVATION LOGIC///
//        ///////////////////////////////////
//
//
//        if (isInFocusMode) {
//            for (WeaponAPI weapon : drone.getAllWeapons()) {
//                if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
//                    weapon.disable(true);
//                }
//            }
//
//            if (droneIndex == 0 && !drone.getSystem().isStateActive()) {
//                drone.useSystem();
//            }
//        } else {
//            for (WeaponAPI weapon : drone.getAllWeapons()) {
//                if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
//                    weapon.repair();
//                }
//            }
//
//            if (droneIndex == 0 && drone.getSystem().isStateActive()) {
//                drone.useSystem();
//            }
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
//        float radius;
//        Vector2f movementTargetLocation;
//        switch (droneOrders) {
//            case DEPLOY:
//                angle = initialOrbitAngle + ship.getFacing();
//
//                radius = orbitRadius;
//
//                delayBeforeLandingTracker.setElapsed(0f);
//
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
//                landingSlot = null;
//
//                isInFocusMode = false;
//
//                break;
//            case RECALL:
//                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);
//
//                if (landingSlot == null) {
//                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
//                }
//
//                movementTargetLocation = landingSlot.computePosition(ship);
//
//                isInFocusMode = false;
//
//                break;
//            case ATTACK:
//                angle = focusModeOrbitAngle + ship.getFacing();
//
//                radius = orbitRadius;
//
//                delayBeforeLandingTracker.setElapsed(0f);
//
//                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
//                landingSlot = null;
//
//                isInFocusMode = true;
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
//        //PRIORITISE TARGET, SET LOCATION
//        CombatEntityAPI target;
//        float targetingArcDeviation = 120f;
//        if (isInFocusMode) {
//            if (engine.getPlayerShip().equals(ship) && ship.getShipTarget() != null) {
//                target = ship.getShipTarget();
//            } else {
//                target = PSE_DroneAIUtils.getEnemyTarget(ship, drone, focusWeaponRange, true, true, false, targetingArcDeviation);
//            }
//        } else {
//            target = PSE_DroneAIUtils.getEnemyTarget(ship, drone, PDWeaponRange,false, false, false, targetingArcDeviation);
//        }
//
//        //ROTATION
//        float relativeAngleFromShip = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(ship, drone.getShipAPI()));
//        //arc logic
//        if (target != null) {
//            if (PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), relativeAngleFromShip, 60f)) {
//                if (isInFocusMode) {
//                    Vector2f to = Vector2f.sub(target.getLocation(), drone.getLocation(),new Vector2f());
//                    PSE_DroneAIUtils.rotateToFacingJerky(drone, VectorUtils.getFacing(to));
//                } else {
//                    PSE_DroneAIUtils.rotateToTarget(ship, drone, target.getLocation());
//                }
//            } else {
//                PSE_DroneAIUtils.rotateToFacing(drone, ship.getFacing(), engine);
//            }
//
//            //check for friendlies
//            boolean areFriendliesInFiringArc = PSE_DroneAIUtils.areFriendliesBlockingArc(drone, target, focusWeaponRange);
//            drone.setHoldFireOneFrame(areFriendliesInFiringArc);
//
//            /* debug stuff
//            if (ship.getShipTarget() != null) {
//                engine.maintainStatusForPlayerShip("thingiepse", "graphics/icons/hullsys/drone_pd_high.png", "TARGET", ship.getShipTarget().toString(), false);
//                engine.maintainStatusForPlayerShip("thingiepse2", "graphics/icons/hullsys/drone_pd_high.png", "FRIENDLIES", areFriendliesInFiringArc + "", false);
//            }//*/
//        } else {
//            PSE_DroneAIUtils.rotateToFacing(drone, ship.getFacing(), engine);
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