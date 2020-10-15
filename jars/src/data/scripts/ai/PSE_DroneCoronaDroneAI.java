package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneCorona;
import data.scripts.util.PSE_DroneUtils;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PSE_DroneCoronaDroneAI implements ShipAIPlugin {

    private final PSEDrone drone;
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private boolean loaded = false;

    //USED FOR MOVEMENT AND POSITIONING AI
    private float[] initialOrbitAngleArray;
    private float[] focusModeOrbitAngleArray;
    private float[] orbitRadiusArray;
    private IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    private IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    private WeaponSlotAPI landingSlot;

    //USED FOR SYSTEM ACTIVATION AI
    private static final String PD_WEAPON_ID = "pdlaser";
    private float PDWeaponRange;
    private float focusWeaponRange;
    private boolean isInFocusMode;

    private String UNIQUE_SYSTEM_ID;

    public PSE_DroneCoronaDroneAI(PSEDrone passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();

        for (WeaponAPI weapon : droneWeapons) {
            String FOCUS_WEAPON_ID = "hil";
            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                PDWeaponRange = weapon.getRange();
            } else if (weapon.getId().contentEquals(FOCUS_WEAPON_ID)) {
                focusWeaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = PSE_DroneCorona.UNIQUE_SYSTEM_PREFIX + ship.hashCode();

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


        if (ship == null || !ship.isAlive()) {
            landingSlot = null;

            ship = PSE_DroneUtils.getAlternateHost(drone, PSE_DroneCorona.UNIQUE_SYSTEM_PREFIX, engine);

            if (ship == null) {
                PSE_DroneUtils.deleteDrone(drone, engine);
                return;
            }
        }

        //get ship system object
        PSE_DroneCorona shipDroneCoronaSystem = (PSE_DroneCorona) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneCoronaSystem == null) {
            return;
        }
        if (!shipDroneCoronaSystem.getDeployedDrones().contains(drone)) {
            shipDroneCoronaSystem.getDeployedDrones().add(drone);
        }

        //config checking
        if (!loaded) {
            initialOrbitAngleArray = PSE_MiscUtils.PSE_CoronaSpecLoading.getInitialOrbitAngleArray();
            focusModeOrbitAngleArray = PSE_MiscUtils.PSE_CoronaSpecLoading.getFocusOrbitAngleArray();
            orbitRadiusArray = PSE_MiscUtils.PSE_CoronaSpecLoading.getOrbitRadiusArray();

            loaded = true;
        }

        //assign specific values
        int droneIndex = shipDroneCoronaSystem.getIndex(drone);
        if (droneIndex == -1) {
            if (landingSlot == null) {
                landingSlot = shipDroneCoronaSystem.getPlugin().getLandingBayWeaponSlotAPI();
            }

            Vector2f movementTargetLocation = landingSlot.computePosition(ship);

            PSE_DroneUtils.move(drone, drone.getFacing(), movementTargetLocation, 1f, velocityRotationIntervalTracker);

            Vector2f to = Vector2f.sub(movementTargetLocation, drone.getLocation(), new Vector2f());
            float angle = VectorUtils.getFacing(to);
            PSE_DroneUtils.rotateToFacing(drone, angle, engine);

            PSE_DroneUtils.attemptToLandAsExtra(ship, drone);
            return;
        }

        float initialOrbitAngle = initialOrbitAngleArray[droneIndex];
        float focusModeOrbitAngle = focusModeOrbitAngleArray[droneIndex];
        float orbitRadius = orbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();

        //UNUSED AS YET CONSTANT FOR BEHAVIOUR WHEN HOST SIGNAL LOST
        float sanity = 1f;

        //GET DRONE ORDERS STATE
        PSE_DroneCorona.CoronaDroneOrders coronaDroneOrders = shipDroneCoronaSystem.getDroneOrders();


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //PRIORITISE TARGET, SET LOCATION
        float droneFacing = drone.getFacing();
        CombatEntityAPI target;
        float targetingArcDeviation = 120f;
        if (isInFocusMode) {
            if (engine.getPlayerShip().equals(ship) && ship.getShipTarget() != null) {
                target = ship.getShipTarget();
            } else {
                target = PSE_DroneUtils.getEnemyTarget(ship, drone, focusWeaponRange, true, true, false, targetingArcDeviation);
            }
        } else {
            target = PSE_DroneUtils.getEnemyTarget(ship, drone, PDWeaponRange,false, false, false, targetingArcDeviation);
        }

        //ROTATION
        float relativeAngleFromShip = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(ship, drone.getShipAPI()));
        //arc logic
        if (target != null) {
            if (PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), relativeAngleFromShip, 60f)) {
                if (isInFocusMode) {
                    Vector2f to = Vector2f.sub(target.getLocation(), drone.getLocation(),new Vector2f());
                    PSE_DroneUtils.rotateToFacingJerky(drone, VectorUtils.getFacing(to));
                } else {
                    PSE_DroneUtils.rotateToTarget(ship, drone, target.getLocation(), engine);
                }
            } else {
                PSE_DroneUtils.rotateToFacing(drone, ship.getFacing(), engine);
            }

            //check for friendlies
            boolean areFriendliesInFiringArc = PSE_DroneUtils.areFriendliesBlockingArc(drone, target, focusWeaponRange, droneFacing, 40f);
            drone.setHoldFireOneFrame(areFriendliesInFiringArc);

            /* debug stuff
            if (ship.getShipTarget() != null) {
                engine.maintainStatusForPlayerShip("thingiepse", "graphics/icons/hullsys/drone_pd_high.png", "TARGET", ship.getShipTarget().toString(), false);
                engine.maintainStatusForPlayerShip("thingiepse2", "graphics/icons/hullsys/drone_pd_high.png", "FRIENDLIES", areFriendliesInFiringArc + "", false);
            }//*/
        } else {
            PSE_DroneUtils.rotateToFacing(drone, ship.getFacing(), engine);
        }


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

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                landingSlot = null;

                isInFocusMode = false;

                break;
            case RECALL:
                PSE_DroneUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = shipDroneCoronaSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);

                isInFocusMode = false;

                break;
            case ATTACK:
                angle = focusModeOrbitAngle + ship.getFacing();

                radius = orbitRadius;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                landingSlot = null;

                isInFocusMode = true;

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        PSE_DroneUtils.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker);


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


