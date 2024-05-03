package data.scripts.drones.sepherum;

import cmu.drones.systems.DroneSubsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import cmu.subsystems.BaseSubsystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.drones.loitering.LoiteringDroneAI;

public class SepherumSubsystem extends DroneSubsystem implements ForgeSpec {

    public static final String SUBSYSTEM_ID = "PSE_SepherumDrone"; //this should match the id in the csv
    private static final String DRONE_VARIANT = "PSE_sepherum_wing";

    private ShipAPI mothership;

    private enum SepherumOrders {
        ACTIVE,
        RECALL
    }

    private SepherumOrders droneOrders = SepherumOrders.RECALL;

    @Override
    public void onActivation() {
        super.onActivation();

        CombatEngineAPI engine = Global.getCombatEngine();

        CombatFleetManagerAPI manager = engine.getFleetManager(mothership.getOwner());
        boolean suppress = manager.isSuppressDeploymentMessages();
        manager.setSuppressDeploymentMessages(true);

        float angle = mothership.getFacing();

        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, DRONE_VARIANT);
        ShipAPI drone = manager.spawnFleetMember(member, mothership.getLocation(), angle, 0f);

        drone.setAnimatedLaunch();

        drone.getVelocity().set(mothership.getVelocity());

        drone.setShipAI(new LoiteringDroneAI(drone, mothership));
        drone.setOwner(mothership.getOwner());

        manager.setSuppressDeploymentMessages(suppress);
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        this.mothership = mothership;

        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {

    }

    @Override
    public int getNumDroneOrders() {
        return 2;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case ACTIVE:
                return "ACTIVE";
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
        return null;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 0;
    }

    @Override
    public float getForgeCooldown() {
        return 0;
    }

    @Override
    public float getLaunchDelay() {
        return 0;
    }

    @Override
    public float getLaunchSpeed() {
        return 0;
    }

    @Override
    public String getDroneVariant() {
        return null;
    }

    @Override
    public int getMaxReserveCount() {
        return 0;
    }

    @Override
    public boolean canDeploy() {
        return false;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI shipAPI, ShipAPI shipAPI1) {
        return null;
    }
}
