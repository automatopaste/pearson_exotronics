package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDrone;
import data.scripts.plugins.PSE_DroneManagerPlugin;

import java.util.ArrayList;

public class PSE_DroneShroud extends BaseShipSystemScript {
    public enum ShroudDroneOrders {
        CIRCLE,
        BROADSIDE_MOVEMENT,
        RECALL
    }

    private final static float ORBIT_BASE_ROTATION_SPEED = 15f;

    public ArrayList<PSEDrone> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private ShroudDroneOrders droneOrders = ShroudDroneOrders.RECALL;

    private ShipAPI ship;

    private int maxDeployedDrones;
    private float launchDelay;
    private float launchSpeed;
    private String droneVariant;

    private float orbitAngleMovementBase = 0f;

    private PSE_DroneManagerPlugin plugin;

    private boolean canSwitchDroneOrders = true;

    public PSE_DroneShroud() {
        maxDeployedDrones = 5;
        launchDelay = 0.1f;
        launchSpeed = 10f;
        droneVariant = "PSE_kingston_drone_Standard";

        plugin = null;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, java.lang.String id) {
        //initialisation and engine data stuff
        this.ship = (ShipAPI) stats.getEntity();
        this.engine = Global.getCombatEngine();

        if (engine != null) {
            ensurePluginExistence();

            String UNIQUE_SYSTEM_ID = "PSE_DroneShroud_" + ship.hashCode();
            engine.getCustomData().put(UNIQUE_SYSTEM_ID, this);
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        if (ship.getSystem().isOn()) {
            //can only be called once on activation
            if (canSwitchDroneOrders) {
                nextDroneOrder();
                canSwitchDroneOrders = false;
            }
        } else {
            canSwitchDroneOrders = true;
        }
    }

    public int getIndex(PSEDrone drone) {
        int index = 0;
        for (PSEDrone deployedDrone : deployedDrones) {
            if (deployedDrone == drone) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    public ShroudDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public void setDroneOrders(ShroudDroneOrders droneOrders) {
        this.droneOrders = droneOrders;
    }

    public ShipAPI getShip() {
        return this.ship;
    }

    public PSE_DroneManagerPlugin getPlugin() {
        return plugin;
    }

    public ShroudDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == ShroudDroneOrders.values().length - 1) {
            return ShroudDroneOrders.values()[0];
        }
        return ShroudDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    public void maintainStatusMessage() {
        switch (droneOrders) {
            case CIRCLE:
                engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DEFENCE FORMATION", false);
                break;
            case BROADSIDE_MOVEMENT:
                engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "BROADSIDE MOVEMENT FORMATION", false);
                engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY_2", "graphics/icons/hullsys/drone_pd_high.png", "DRONE-ASSISTED MANEUVERS", "DRIVE FIELD BOOSTED", false);
                break;
            case RECALL:
                if (deployedDrones.isEmpty()) {
                    engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DRONES RECALLED", true);
                } else {
                    engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "RECALLING DRONES", true);
                }
                break;
        }
    }

    public ArrayList<PSEDrone> getDeployedDrones() {
        return deployedDrones;
    }

    public void setDeployedDrones(ArrayList<PSEDrone> list) {
        this.deployedDrones = list;
    }

    public void ensurePluginExistence() {
        if (plugin == null) {
            plugin = new PSE_DroneManagerPlugin(this, maxDeployedDrones, launchDelay, launchSpeed, ship, droneVariant);
            engine.addPlugin(plugin);
        }
    }

    public void advanceOrbitAngleBase(float amount) {
        orbitAngleMovementBase += (ORBIT_BASE_ROTATION_SPEED * amount);
        if (orbitAngleMovementBase >= 360f) {
            orbitAngleMovementBase -= 360f;
        }
    }

    public void resetOrbitAngleBase() {
        orbitAngleMovementBase = 0f;
    }

    public float getOrbitAngleBase() {
        return orbitAngleMovementBase;
    }
}
