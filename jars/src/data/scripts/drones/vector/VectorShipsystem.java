package data.scripts.drones.vector;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.combat.ai.P;
import data.scripts.drones.citadel.CitadelShipsystem;
import data.scripts.drones.rift.RiftShipsystem;

import java.util.HashMap;
import java.util.Map;

public class VectorShipsystem extends DroneShipsystem implements ForgeSpec {

    public enum VectorOrders {
        RESONATOR,
        VECTOR_THRUST,
        RECALL
    }

    private VectorOrders droneOrders = VectorOrders.RECALL;

    private final Map<VectorOrders, SpriteAPI> icons = new HashMap<>();

    public VectorShipsystem() {
        icons.put(VectorOrders.RESONATOR, Global.getSettings().getSprite("graphics/icons/hullsys/emp_emitter.png"));
        icons.put(VectorOrders.VECTOR_THRUST, Global.getSettings().getSprite("graphics/icons/hullsys/maneuvering_jets.png"));
        icons.put(VectorOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == VectorOrders.values().length - 1) droneOrders = VectorOrders.values()[0];
        else droneOrders = VectorOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return VectorOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case RESONATOR:
                return "RESONATOR ARRAY";
            case VECTOR_THRUST:
                return "VECTOR THRUST BOOSTER";
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
        return 0.5f;
    }

    @Override
    public float getLaunchSpeed() {
        return 150f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_drone3_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 1;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != VectorOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new VectorDroneAI(drone, mothership, this);
    }

    public VectorOrders getDroneOrders() {
        return droneOrders;
    }
}
