package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDroneAPI;
import data.scripts.PSEModPlugin;
import data.scripts.plugins.PSE_DroneManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PSE_DroneBastion extends BaseShipSystemScript {
    static final float FLUX_PER_SECOND = 100f;

    public enum BastionDroneOrders {
        CARDINAL,
        FRONT,
        RECALL
    }

    public ArrayList<PSEDroneAPI> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private BastionDroneOrders droneOrders = BastionDroneOrders.RECALL;

    private ShipAPI ship;

    //json as loaded from shipsystem file
    private JSONObject specJson;

    //initialise values from json
    private int maxDeployedDrones;
    private float launchDelay;
    private float launchSpeed;
    private String droneVariant;

    private PSE_DroneManagerPlugin plugin;

    private boolean canSwitchDroneOrders;

    public PSE_DroneBastion() {
        this.specJson = PSEModPlugin.droneBastionSpecJson;
        try {
            this.maxDeployedDrones = specJson.getInt("maxDrones");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.launchDelay = (float) specJson.getDouble("launchDelay");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.launchSpeed = (float) specJson.getDouble("launchSpeed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.droneVariant = specJson.getString("droneVariant");
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

            String UNIQUE_SYSTEM_ID = "PSE_droneBastion_" + ship.hashCode();
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

    public JSONObject getSpecJson() {
        return specJson;
    }

    public int getIndex(PSEDroneAPI drone) {
        int index = 0;
        for (PSEDroneAPI deployedDrone : deployedDrones) {
            if (deployedDrone == drone) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    public BastionDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public ShipAPI getShip() {
        return this.ship;
    }

    public String getDroneVariant() {
        return this.droneVariant;
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
            case CARDINAL:
                engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "DEFENCE FORMATION", false);
                break;
            case FRONT:
                engine.maintainStatusForPlayerShip("BASTION_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "FRONT FORMATION", false);
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

    public ArrayList<PSEDroneAPI> getDeployedDrones() {
        return deployedDrones;
    }

    public void setDeployedDrones(ArrayList<PSEDroneAPI> list) {
        this.deployedDrones = list;
    }

    public void ensurePluginExistence() {
        if (plugin == null) {
            plugin = new PSE_DroneManagerPlugin(this, maxDeployedDrones, launchDelay, launchSpeed, ship, droneVariant);
            engine.addPlugin(plugin);
        }
    }
}
