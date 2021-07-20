package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.util.PSE_DroneAIUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public abstract class PSE_BaseDroneAI implements ShipAIPlugin {
    protected CombatEngineAPI engine;
    protected PSEDrone drone;
    protected ShipAPI ship;
    protected String uniqueSystemPrefix;
    protected PSE_BaseDroneSystem baseDroneSystem;
    protected int droneIndex;

    protected final IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    protected WeaponSlotAPI landingSlot;

    public PSE_BaseDroneAI(PSEDrone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        this.drone = passedDrone;
        this.ship = drone.getDroneSource();

        this.baseDroneSystem = baseDroneSystem;
        this.uniqueSystemPrefix = baseDroneSystem.systemID;

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

            ship = PSE_DroneAIUtils.getAlternateHost(drone, uniqueSystemPrefix, 4000f);

            if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
                PSE_DroneAIUtils.deleteDrone(drone, engine);
                return;
            }
        }

        if (!baseDroneSystem.getDeployedDrones().contains(drone)) {
            baseDroneSystem.getDeployedDrones().add(drone);
        }

        //check if currently superfluous
        droneIndex = baseDroneSystem.getIndex(drone);
        if (droneIndex == -1) {
            if (landingSlot == null) {
                landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
            }

            Vector2f movementTargetLocation = landingSlot.computePosition(ship);

            PSE_DroneAIUtils.move(drone, drone.getFacing(), movementTargetLocation);

            Vector2f to = Vector2f.sub(movementTargetLocation, drone.getLocation(), new Vector2f());
            float angle = VectorUtils.getFacing(to);
            PSE_DroneAIUtils.rotateToFacing(drone, angle, engine);

            PSE_DroneAIUtils.attemptToLandAsExtra(ship, drone);
        }
    }

    protected abstract Vector2f getMovementTargetLocation(float amount);

    protected abstract void doRotationTargeting();

    protected String getUniqueSystemID() {
        if (uniqueSystemPrefix == null || ship == null) {
            return "troled";
        }
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