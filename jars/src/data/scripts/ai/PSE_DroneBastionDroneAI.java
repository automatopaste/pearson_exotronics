package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneBastion;
import data.scripts.util.PSE_DroneUtils;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PSE_DroneBastionDroneAI implements ShipAIPlugin {



    private final PSEDrone drone;
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private boolean loaded = false;

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

    public PSE_DroneBastionDroneAI(PSEDrone passedDrone) {
        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        for (WeaponAPI weapon : drone.getAllWeapons()) {
            if (weapon.getId().contentEquals(WEAPON_ID)) {
                weaponRange = weapon.getRange();
            }
        }

        this.UNIQUE_SYSTEM_ID = PSE_DroneBastion.UNIQUE_SYSTEM_PREFIX + ship.hashCode();

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
    }

    @Override
    public void advance(float amount) {
        this.engine = Global.getCombatEngine();

        if (engine.isPaused()) {
            return;
        }
        if (drone == null || ship == null) {
            return;
        }

        ////////////////////
        ///INITIALISATION///
        ///////////////////

        float sanity = 1f;

        if (ship == null) {
            return;
        }
        if (!ship.isAlive()) {
            landingSlot = null;

            ship = PSE_DroneUtils.getAlternateHost(drone, PSE_DroneBastion.UNIQUE_SYSTEM_PREFIX, engine);

            if (ship == null) {
                PSE_DroneUtils.deleteDrone(drone, engine);
                return;
            }
        }

        float droneFacing = drone.getFacing();
        float shipFacing = ship.getFacing();

        //get ship system object
        PSE_DroneBastion shipDroneBastionSystem = (PSE_DroneBastion) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneBastionSystem == null) {
            return;
        }
        if (!shipDroneBastionSystem.getDeployedDrones().contains(drone)) {
            shipDroneBastionSystem.getDeployedDrones().add(drone);
        }

        //config checking
        if (!loaded) {
            cardinalOrbitAngleArray = PSE_MiscUtils.PSE_BastionSpecLoading.getCardinalOrbitAngleArray();
            frontOrbitAngleArray = PSE_MiscUtils.PSE_BastionSpecLoading.getFrontOrbitAngleArray();
            orbitRadiusArray = PSE_MiscUtils.PSE_BastionSpecLoading.getOrbitRadiusArray();

            loaded = true;
        }

        //assign specific values
        int droneIndex = shipDroneBastionSystem.getIndex(drone);
        if (droneIndex == -1) {
            if (landingSlot == null) {
                landingSlot = shipDroneBastionSystem.getPlugin().getLandingBayWeaponSlotAPI();
            }

            Vector2f movementTargetLocation = landingSlot.computePosition(ship);

            PSE_DroneUtils.move(drone, drone.getFacing(), movementTargetLocation, 1f, velocityRotationIntervalTracker);

            Vector2f to = Vector2f.sub(movementTargetLocation, drone.getLocation(), new Vector2f());
            float angle = VectorUtils.getFacing(to);
            PSE_DroneUtils.rotateToFacing(drone, angle, engine);

            PSE_DroneUtils.attemptToLandAsExtra(ship, drone);
            return;
        }

        float cardinalOrbitAngle = cardinalOrbitAngleArray[droneIndex];
        float frontOrbitAngle = frontOrbitAngleArray[droneIndex];
        float orbitRadius = orbitRadiusArray[droneIndex] + ship.getShieldRadiusEvenIfNoShield();

        //get orders
        PSE_DroneBastion.BastionDroneOrders bastionDroneOrders = shipDroneBastionSystem.getDroneOrders();


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////


        //needs no special targeting behaviour
        CombatEntityAPI target;
        target = PSE_DroneUtils.getEnemyTarget(ship, drone, weaponRange, false, false, false, 360f);

        //ROTATION
        float facing = frontOrbitAngle + shipFacing;
        float droneAngleRelativeToShip = VectorUtils.getFacing(PSE_MiscUtils.getVectorFromAToB(ship, drone.getShipAPI()));
        switch (bastionDroneOrders) {
            case FRONT:
                if (target != null && PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), droneAngleRelativeToShip, 120f)) {
                    PSE_DroneUtils.rotateToTarget(ship, drone, target.getLocation(), engine);
                } else {
                    PSE_DroneUtils.rotateToFacing(drone, shipFacing, engine);
                }
                break;
            case CARDINAL:
            case RECALL:
                if (target != null && PSE_MiscUtils.isEntityInArc(target, drone.getLocation(), droneAngleRelativeToShip, 120f)) {
                    PSE_DroneUtils.rotateToTarget(ship, drone, target.getLocation(), engine);
                } else {
                    PSE_DroneUtils.rotateToFacing(drone, facing, engine);
                }
                break;
        }



        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////


        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle;
        Vector2f movementTargetLocation;
        switch (bastionDroneOrders) {
            case FRONT:
                angle = cardinalOrbitAngle + shipFacing;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);
                landingSlot = null;

                break;
            case RECALL:
                PSE_DroneUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = shipDroneBastionSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);

                break;
            case CARDINAL:
                angle = frontOrbitAngle + shipFacing;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), orbitRadius, angle);

                landingSlot = null;

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }
        //engine.maintainStatusForPlayerShip("thing", "graphics/icons/hullsys/drone_pd_high.png", "ANGLE", angle + ", " + Arrays.toString(frontOrbitAngleArray), false);

        PSE_DroneUtils.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker);
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
