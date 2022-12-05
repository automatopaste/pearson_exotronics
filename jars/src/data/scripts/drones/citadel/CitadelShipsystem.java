package data.scripts.drones.citadel;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.HashMap;
import java.util.Map;

public class CitadelShipsystem extends DroneShipsystem implements ForgeSpec {

    public enum CitadelOrders {
        ANTI_FIGHTER,
        SHIELD,
        RECALL
    }

    private CitadelOrders droneOrders = CitadelOrders.RECALL;

    private final Map<CitadelOrders, SpriteAPI> icons = new HashMap<>();

    public CitadelShipsystem() {
        icons.put(CitadelOrders.ANTI_FIGHTER, Global.getSettings().getSprite("graphics/icons/hullsys/drone_borer.png"));
        icons.put(CitadelOrders.SHIELD, Global.getSettings().getSprite("graphics/icons/hullsys/fortress_shield.png"));
        icons.put(CitadelOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == CitadelOrders.values().length - 1) droneOrders = CitadelOrders.values()[0];
        else droneOrders = CitadelOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return CitadelOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case ANTI_FIGHTER:
                return "ANTI-FIGHTER MODE";
            case SHIELD:
                return "SHIELD MODE";
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
        return 4;
    }

    @Override
    public float getForgeCooldown() {
        return 20f;
    }

    @Override
    public float getLaunchDelay() {
        return 0.3f;
    }

    @Override
    public float getLaunchSpeed() {
        return 100f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_citadel_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 4;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != CitadelOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new CitadelDroneAI(drone, mothership, this);
    }

    public CitadelOrders getDroneOrders() {
        return droneOrders;
    }
}
