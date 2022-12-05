package data.scripts.drones.corona;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.HashMap;
import java.util.Map;

public class CoronaShipsystem extends DroneShipsystem implements ForgeSpec {

    public enum CoronaOrders {
        DEFEND,
        ATTACK,
        RECALL
    }
    private CoronaOrders droneOrders = CoronaOrders.RECALL;

    private final Map<CoronaOrders, SpriteAPI> icons = new HashMap<>();

    public CoronaShipsystem() {
        icons.put(CoronaOrders.DEFEND, Global.getSettings().getSprite("graphics/icons/hullsys/fortress_shield.png"));
        icons.put(CoronaOrders.ATTACK, Global.getSettings().getSprite("graphics/icons/hullsys/targeting_feed.png"));
        icons.put(CoronaOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == CoronaOrders.values().length - 1) droneOrders = CoronaOrders.values()[0];
        else droneOrders = CoronaOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return CoronaOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case DEFEND:
                return "DEFENCE FORMATION";
            case ATTACK:
                return "ATTACK FORMATION";
            case RECALL:
            default:
                return "RECALL";
        }
    }

    @Override
    public SpriteAPI getIconForActiveState() {
        return icons.get(droneOrders);
    }

    @Override
    public int getMaxDeployedDrones() {
        return 3;
    }

    @Override
    public float getForgeCooldown() {
        return 20f;
    }

    @Override
    public float getLaunchDelay() {
        return 1f;
    }

    @Override
    public float getLaunchSpeed() {
        return 0.5f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_drone1_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 3;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != CoronaOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new CoronaDroneAI(drone, mothership, this);
    }

    public CoronaOrders getDroneOrders() {
        return droneOrders;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        switch (droneOrders) {
            case DEFEND:
                if (index == 0) return new StatusData("DEFENCE FORMATION", false);
                break;
            case ATTACK:
                if (index == 0) return new StatusData("ATTACK FORMATION", false);
                break;
            case RECALL:
                if (index == 0) return new StatusData("RECALL", false);
                break;
        }
        return null;
    }
}
