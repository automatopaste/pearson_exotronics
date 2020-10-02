package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDrone;
import data.scripts.plugins.PSE_DroneManagerPlugin;
import data.scripts.util.PSE_MiscUtils;

import java.util.ArrayList;

public class PSE_DroneCorona extends BaseShipSystemScript {
    public static final float FLUX_PER_SECOND_MULT = 0.05f;

    public enum CoronaDroneOrders {
        DEPLOY,
        ATTACK,
        RECALL
    }

    public ArrayList<PSEDrone> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private CoronaDroneOrders droneOrders = CoronaDroneOrders.RECALL;

    private ShipAPI ship;

    private int maxDeployedDrones;
    private float launchDelay;
    private float launchSpeed;
    private String droneVariant;

    private PSE_DroneManagerPlugin plugin;

    private boolean canSwitchDroneOrders;

    public PSE_DroneCorona() {
        maxDeployedDrones = PSE_MiscUtils.PSE_CoronaSpecLoading.getMaxDeployedDrones();
        launchDelay = (float) PSE_MiscUtils.PSE_CoronaSpecLoading.getLaunchDelay();
        launchSpeed = (float) PSE_MiscUtils.PSE_CoronaSpecLoading.getLaunchSpeed();
        droneVariant = PSE_MiscUtils.PSE_CoronaSpecLoading.getDroneVariant();

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

            String UNIQUE_SYSTEM_ID = "PSE_DroneCorona_" + ship.hashCode();
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

    public CoronaDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public void setDroneOrders(CoronaDroneOrders droneOrders) {
        this.droneOrders = droneOrders;
    }

    public ShipAPI getShip() {
        return this.ship;
    }

    public PSE_DroneManagerPlugin getPlugin() {
        return plugin;
    }

    public CoronaDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == CoronaDroneOrders.values().length - 1) {
            return CoronaDroneOrders.values()[0];
        }
        return CoronaDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    public void maintainStatusMessage() {
        switch (droneOrders) {
            case DEPLOY:
                engine.maintainStatusForPlayerShip("CORONA_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DEFENCE FORMATION", false);
                break;
            case ATTACK:
                engine.maintainStatusForPlayerShip("CORONA_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "FOCUS FORMATION", false);
                break;
            case RECALL:
                if (deployedDrones.isEmpty()) {
                    engine.maintainStatusForPlayerShip("CORONA_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DRONES RECALLED", true);
                } else {
                    engine.maintainStatusForPlayerShip("CORONA_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "RECALLING DRONES", true);
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

    public float getFluxPerSecond() {
        return FLUX_PER_SECOND_MULT * ship.getMaxFlux();
    }
}
