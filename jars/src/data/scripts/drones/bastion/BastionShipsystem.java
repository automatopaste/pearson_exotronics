package data.scripts.drones.bastion;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.drones.corona.CoronaShipsystem;

import java.util.HashMap;
import java.util.Map;

public class BastionShipsystem extends DroneShipsystem implements ForgeSpec {

    public enum BastionOrders {
        FRONT,
        CARDINAL,
        RECALL
    }

    private BastionOrders droneOrders = BastionOrders.RECALL;

    private final Map<BastionOrders, SpriteAPI> icons = new HashMap<>();

    public BastionShipsystem() {
        icons.put(BastionOrders.FRONT, Global.getSettings().getSprite("graphics/icons/hullsys/reserve_deployment.png"));
        icons.put(BastionOrders.CARDINAL, Global.getSettings().getSprite("graphics/icons/hullsys/damper_field.png"));
        icons.put(BastionOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public int getMaxDeployedDrones() {
        return 4;
    }

    @Override
    public float getForgeCooldown() {
        return 15f;
    }

    @Override
    public float getLaunchDelay() {
        return 0.5f;
    }

    @Override
    public float getLaunchSpeed() {
        return 100f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_deuces_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 6;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != BastionOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new BastionDroneAI(drone, mothership, this);
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == BastionOrders.values().length - 1) droneOrders = BastionOrders.values()[0];
        else droneOrders = BastionOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return BastionOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case FRONT:
                return "FRONT FORMATION";
            case CARDINAL:
                return "CARDINAL FORMATION";
            case RECALL:
            default:
                return "RECALL";
        }
    }

    @Override
    public SpriteAPI getIconForActiveState() {
        return icons.get(droneOrders);
    }

    public BastionOrders getDroneOrders() {
        return droneOrders;
    }
}
