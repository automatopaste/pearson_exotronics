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
import java.util.List;

public class PSE_DroneCorona extends BaseShipSystemScript {
    static final float FLUX_PER_SECOND = 100f;

    public enum CoronaDroneOrders {
        DEPLOY,
        ATTACK,
        RECALL
    }

    public ArrayList<PSEDroneAPI> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private CoronaDroneOrders droneOrders = CoronaDroneOrders.RECALL;

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

    public PSE_DroneCorona() {
        this.specJson = PSEModPlugin.droneCoronaSpecJson;
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

            String UNIQUE_SYSTEM_ID = "PSE_droneCorona_" + ship.hashCode();
            engine.getCustomData().put(UNIQUE_SYSTEM_ID, this);
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        if (ship.getSystem().isOn()) {
            //can only be called once on activation
            if (canSwitchDroneOrders) {
                /*
                if (getNextOrder() == CoronaDroneOrders.ATTACK) {
                    ship.getSystem().setFluxPerSecond(FLUX_PER_SECOND);
                } else {
                    ship.getSystem().setFluxPerSecond(0f);
                }

                 */
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

    public CoronaDroneOrders getDroneOrders() {
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
