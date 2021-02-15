package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.util.PSE_DroneUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public abstract class PSE_BaseDroneAI implements ShipAIPlugin {
    protected CombatEngineAPI engine;
    protected PSEDrone drone;
    protected ShipAPI ship;
    protected String uniqueSystemPrefix;
    protected PSE_BaseDroneSystem baseDroneSystem;

    protected final IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    protected final IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    protected WeaponSlotAPI landingSlot;

    public PSE_BaseDroneAI(PSEDrone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        this.drone = passedDrone;
        this.ship = drone.getDroneSource();

        this.baseDroneSystem = baseDroneSystem;
        this.uniqueSystemPrefix = baseDroneSystem.uniqueSystemPrefix;

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);

        engine = Global.getCombatEngine();
    }
    @Override
    public void setDoNotFireDelay(float amount) {

    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public void advance(float amount) {
        if (engine.isPaused() || drone == null) return;

        //check for relocation
        if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
            landingSlot = null;

            ship = PSE_DroneUtils.getAlternateHost(drone, uniqueSystemPrefix);

            if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
                PSE_DroneUtils.deleteDrone(drone, engine);
                return;
            }
        }

        float droneFacing = drone.getFacing();

        if (!baseDroneSystem.getDeployedDrones().contains(drone)) {
            baseDroneSystem.getDeployedDrones().add(drone);
        }

        //check if currently superfluous
        int droneIndex = baseDroneSystem.getIndex(drone);
        if (droneIndex == -1) {
            if (landingSlot == null) {
                landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
            }

            Vector2f movementTargetLocation = landingSlot.computePosition(ship);

            PSE_DroneUtils.move(drone, drone.getFacing(), movementTargetLocation, velocityRotationIntervalTracker);

            Vector2f to = Vector2f.sub(movementTargetLocation, drone.getLocation(), new Vector2f());
            float angle = VectorUtils.getFacing(to);
            PSE_DroneUtils.rotateToFacing(drone, angle, engine);

            PSE_DroneUtils.attemptToLandAsExtra(ship, drone);
        }
    }

    protected abstract Vector2f getMovementTargetLocation(float amount);

    protected abstract void doRotationTargeting();

    protected String getUniqueSystemID() {
        return uniqueSystemPrefix + ship.hashCode();
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
}