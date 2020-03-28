package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.SPEDroneAPI;
import data.scripts.SPEModPlugin;
import data.scripts.plugins.SPE_droneManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;

public class SPE_droneCorona extends BaseShipSystemScript {
    public enum CoronaDroneOrders {
        DEPLOY,
        ATTACK,
        RECALL
    }

    public ArrayList<SPEDroneAPI> deployedDrones = new ArrayList<>();

    private CombatEngineAPI engine;

    private CoronaDroneOrders droneOrders = CoronaDroneOrders.DEPLOY;

    private ShipAPI ship;

    //json as loaded from shipsystem file
    private JSONObject specJson;

    //initialise values from json
    private int maxDeployedDrones;
    private float launchDelay;
    private float launchSpeed;
    private String droneVariant;
    private Vector2f launchVelocity;

    private SPE_droneManagerPlugin plugin;

    public SPE_droneCorona() {
        this.specJson = SPEModPlugin.droneCoronaSpecJson;
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

    }

    boolean canSwitchDroneOrders;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        //initialisation and engine data stuff
        this.engine = Global.getCombatEngine();
        this.ship = (ShipAPI) stats.getEntity();

        if (plugin == null) {
            plugin = new SPE_droneManagerPlugin(this, maxDeployedDrones, launchDelay, launchSpeed, ship, droneVariant);
            engine.addPlugin(plugin);
        }

        String UNIQUE_SYSTEM_ID = "SPE_droneCorona_" + ship.hashCode();
        engine.getCustomData().put(UNIQUE_SYSTEM_ID, this);

        if (ship.getSystem().isOn()) {
            //can only be called once on activation
            if (canSwitchDroneOrders) {
                droneOrders = getNextOrder(droneOrders);
                canSwitchDroneOrders = false;
            }
        } else {
            canSwitchDroneOrders = true;
        }

        switch (droneOrders) {
            case DEPLOY:
                break;
            case ATTACK:
                break;
            case RECALL:
                break;
        }
    }

    public JSONObject getSpecJson() {
        return specJson;
    }

    public int getIndex(SPEDroneAPI drone) {
        int index = 0;
        for (SPEDroneAPI deployedDrone : deployedDrones) {
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

    public ShipAPI getShip() {
        return this.ship;
    }

    public CoronaDroneOrders getNextOrder(CoronaDroneOrders droneOrders) {
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
                engine.maintainStatusForPlayerShip("CORONA_STAT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", "RECALLING DRONES", true);
                break;
        }
    }

    public ArrayList<SPEDroneAPI> getDeployedDrones() {
        return deployedDrones;
    }
}
