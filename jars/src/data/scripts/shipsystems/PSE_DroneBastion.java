package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDrone;
import data.scripts.plugins.PSE_DroneManagerPlugin;
import data.scripts.util.PSE_MiscUtils;

import java.util.ArrayList;

public class PSE_DroneBastion extends BaseShipSystemScript {
    //static final float FLUX_PER_SECOND = 100f;
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_DroneBastion_";

    public enum BastionDroneOrders {
        FRONT,
        CARDINAL,
        RECALL
    }

    public ArrayList<PSEDrone> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private BastionDroneOrders droneOrders = BastionDroneOrders.RECALL;

    private ShipAPI ship;

    private int maxDeployedDrones;
    private float launchDelay;
    private float launchSpeed;
    private String droneVariant;

    private PSE_DroneManagerPlugin plugin;

    private boolean canSwitchDroneOrders;

    public PSE_DroneBastion() {
        maxDeployedDrones = PSE_MiscUtils.PSE_BastionSpecLoading.getMaxDeployedDrones();
        launchDelay = (float) PSE_MiscUtils.PSE_BastionSpecLoading.getLaunchDelay();
        launchSpeed = (float) PSE_MiscUtils.PSE_BastionSpecLoading.getLaunchSpeed();
        droneVariant = PSE_MiscUtils.PSE_BastionSpecLoading.getDroneVariant();

        plugin = null;
        canSwitchDroneOrders = true;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, java.lang.String id) {
        //initialisation and engine data stuff
        this.ship = (ShipAPI) stats.getEntity();
        this.engine = Global.getCombatEngine();

        if (engine != null) {
            ensurePluginExistence();

            String UNIQUE_SYSTEM_ID = UNIQUE_SYSTEM_PREFIX + ship.hashCode();
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
            if (index >= maxDeployedDrones) {
                break;
            }
            if (deployedDrone == drone) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public BastionDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public void setDroneOrders(BastionDroneOrders droneOrders) {
        this.droneOrders = droneOrders;
    }

    public ShipAPI getShip() {
        return this.ship;
    }

    public PSE_DroneManagerPlugin getPlugin() {
        return plugin;
    }

    public BastionDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == BastionDroneOrders.values().length - 1) {
            return BastionDroneOrders.values()[0];
        }
        return BastionDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    public void maintainStatusMessage() {
        switch (droneOrders) {
            case FRONT:
                engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "FRONT FORMATION", false);
                break;
            case CARDINAL:
                engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "CARDINAL FORMATION", false);
                break;
            case RECALL:
                if (deployedDrones.isEmpty()) {
                    engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DRONES RECALLED", true);
                } else {
                    engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "RECALLING DRONES", true);
                }
                break;
        }
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (plugin == null) return "NULL";

        int reserve = plugin.getReserveDroneCount();
        String volume = reserve + " / " + (maxDeployedDrones - 1);

        if (reserve < maxDeployedDrones - 1) {
            return volume + ": FORGING";
        } else if (reserve > maxDeployedDrones - 1) {
            return volume + ": OVER FORGE CAPACITY";
        } else {
            return volume + ": AT CAPACITY";
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
}
