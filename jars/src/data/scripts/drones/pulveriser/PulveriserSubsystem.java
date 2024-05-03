package data.scripts.drones.pulveriser;

import cmu.CMUtils;
import cmu.drones.systems.DroneSubsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import cmu.drones.systems.SystemData;
import cmu.subsystems.BaseSubsystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PulveriserSubsystem extends DroneSubsystem implements ForgeSpec {

    public static final String SUBSYSTEM_ID = "PSE_PulveriserSubsystem";

    public enum PulveriserOrders {
        DEPLOY,
        RECALL
    }

    private PulveriserOrders droneOrders = PulveriserOrders.RECALL;

    private final Map<PulveriserOrders, SpriteAPI> icons = new HashMap<>();

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.4f);

    public PulveriserSubsystem() {
        icons.put(PulveriserOrders.DEPLOY, Global.getSettings().getSprite("graphics/icons/hullsys/drone_pd_high.png"));
        icons.put(PulveriserOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, BaseSubsystem.SubsystemState state, float effectLevel) {
        Map<String, Object> data = Global.getCombatEngine().getCustomData();

        super.apply(stats, id, state, effectLevel);

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();

        switch (droneOrders) {
        }
    }

    @Override
    public void aiInit() {

    }

    @Override
    public void aiUpdate(float amount) {
        if (ship == null || !ship.isAlive() | ship.isHulk()) return;

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        PulveriserSubsystem system = (PulveriserSubsystem) SystemData.getDroneSystem(ship, Global.getCombatEngine());
        if (system == null) return;

        List<ShipAPI> ships = CMUtils.getShipsInRange(
                ship.getLocation(),
                4000f,
                ship.getOwner(),
                CMUtils.ShipSearchFighters.DONT_CARE,
                CMUtils.ShipSearchOwner.ENEMY
        );

        switch (system.getDroneOrders()) {
            case DEPLOY:
                if (ships.isEmpty()) system.cycleDroneOrders();
                break;
            case RECALL:
                if (!ships.isEmpty()) system.cycleDroneOrders();
                break;
        }
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == PulveriserOrders.values().length - 1) droneOrders = PulveriserOrders.values()[0];
        else droneOrders = PulveriserOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return PulveriserOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case DEPLOY:
                return "DEFENCE ARRAY";
            case RECALL:
            default:
                return "RECALL";
        }
    }

    @Override
    public String getSubsystemId() {
        return SUBSYSTEM_ID;
    }

    @Override
    public SpriteAPI getIconForActiveState() {
        return icons.get(droneOrders);
    }

    @Override
    public int getMaxDeployedDrones() {
        return 2;
    }

    @Override
    public float getForgeCooldown() {
        return 40f;
    }

    @Override
    public float getLaunchDelay() {
        return 1f;
    }

    @Override
    public float getLaunchSpeed() {
        return 150f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_pulveriser_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 1;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != PulveriserOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new PulveriserDroneAI(drone, mothership, this);
    }

    public PulveriserOrders getDroneOrders() {
        return droneOrders;
    }

    @Override
    public String getInfoString() {
        switch (droneOrders) {
            case DEPLOY:
                return "DEPLOY";
            case RECALL:
            default:
                return "RECALL";
        }
    }

    @Override
    public String getFlavourString() {
        return "Launches two defensive anti-shield drones";
    }

//    @Override
//    public String getStatusString() {
//        return "STATUS";
//    }
}
