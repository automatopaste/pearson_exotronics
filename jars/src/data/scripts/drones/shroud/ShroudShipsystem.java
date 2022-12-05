package data.scripts.drones.shroud;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.HashMap;
import java.util.Map;

public class ShroudShipsystem extends DroneShipsystem implements ForgeSpec {

    public static final float MAX_TURN_BONUS = 25f;
    public static final float ACC_TURN_BONUS = 50f;
    public static final float TOP_SPEED_BONUS = 15;
    public static final float ACC_BONUS = 25f;

    public enum ShroudOrders {
        CIRCLE,
        BROADSIDE,
        RECALL
    }
    public ShroudOrders droneOrders = ShroudOrders.RECALL;

    private final Map<ShroudOrders, SpriteAPI> icons = new HashMap<>();

    private final float[] angles = new float[getMaxDeployedDrones()];

    public ShroudShipsystem() {
        icons.put(ShroudOrders.CIRCLE, Global.getSettings().getSprite("graphics/icons/hullsys/high_energy_focus.png"));
        icons.put(ShroudOrders.BROADSIDE, Global.getSettings().getSprite("graphics/icons/hullsys/temporal_shell.png"));
        icons.put(ShroudOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == ShroudOrders.values().length - 1) droneOrders = ShroudOrders.values()[0];
        else droneOrders = ShroudOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return ShroudOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case CIRCLE:
                return "DEFENCE FORMATION";
            case BROADSIDE:
                return "BROADSIDE FORMATION";
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
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);

        if (droneOrders == ShroudOrders.BROADSIDE) {
            stats.getMaxTurnRate().modifyPercent(id, MAX_TURN_BONUS);
            stats.getTurnAcceleration().modifyPercent(id, ACC_TURN_BONUS);
            stats.getMaxSpeed().modifyFlat(id, TOP_SPEED_BONUS);
            stats.getAcceleration().modifyPercent(id, ACC_BONUS);
        } else {
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
        }
    }

    @Override
    public int getMaxDeployedDrones() {
        return 5;
    }

    @Override
    public float getForgeCooldown() {
        return 40f;
    }

    @Override
    public float getLaunchDelay() {
        return 0.2f;
    }

    @Override
    public float getLaunchSpeed() {
        return 10f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_kingston_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 5;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != ShroudOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new ShroudDroneAI(drone, mothership, this);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        switch (droneOrders) {
            case CIRCLE:
                if (index == 0) return new StatusData("DEFENCE FORMATION", false);

                break;
            case BROADSIDE:
                if (index == 0) return new StatusData("BROADSIDE FORMATION", false);
                if (index == 1) return new StatusData("DRIVE FIELD BOOSTED", false);

                break;
            case RECALL:
                if (index == 0) return new StatusData("RECALL", false);

                break;
        }
        return null;
    }

    public ShroudOrders getDroneOrders() {
        return droneOrders;
    }
}
