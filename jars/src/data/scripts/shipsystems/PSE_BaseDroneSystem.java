package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.plugins.PSE_DroneManagerPlugin;
import data.scripts.util.PSE_SpecLoadingUtils;

import java.util.ArrayList;
import java.util.Map;

public abstract class PSE_BaseDroneSystem extends BaseShipSystemScript {
    protected static final String STATUS_DISPLAY_KEY = "PSE_DroneStatKey";
    protected static final String STATUS_DISPLAY_SPRITE = "graphics/icons/hullsys/drone_pd_high.png";
    protected static final String STATUS_DISPLAY_TITLE = "SYSTEM STATE";
    public String systemID;

    public ArrayList<PSEDrone> deployedDrones = new ArrayList<>();

    public ShipAPI ship;

    public int maxDeployedDrones;
    public float forgeCooldown;
    public float launchDelay;
    public float launchSpeed;
    public String droneVariant;

    protected PSE_DroneManagerPlugin plugin = null;

    protected boolean canSwitchDroneOrders = true;

    protected void loadSpecData() {
        Map<String, PSE_SpecLoadingUtils.PSE_DroneSystemSpec> specMap = PSE_SpecLoadingUtils.droneSystemSpecHashMap;
        PSE_SpecLoadingUtils.PSE_DroneSystemSpec spec = specMap.get(systemID);

        maxDeployedDrones = spec.maxDeployedDrones;
        forgeCooldown = (float) spec.forgeCooldown;
        launchDelay = (float) spec.launchDelay;
        launchSpeed = (float) spec.launchSpeed;
        droneVariant = spec.droneVariant;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, java.lang.String id) {
        //initialisation and engine data stuff
        this.ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine != null) {
            ensurePluginExistence();

            String UNIQUE_SYSTEM_ID = systemID + ship.hashCode();
            engine.getCustomData().put(UNIQUE_SYSTEM_ID, this);
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        /*if (ship.getSystem().isOn()) {
            //can only be called once on activation
            if (canSwitchDroneOrders) {
                nextDroneOrder();
                canSwitchDroneOrders = false;
            }
        } else {
            canSwitchDroneOrders = true;
        }*/
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

    public abstract void nextDroneOrder();

    public abstract void maintainStatusMessage();

    public abstract boolean isRecallMode();

    public abstract void setDefaultDeployMode();

    public abstract void executePerOrders(float amount);

    public abstract PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem);

    public void applyActiveStatBehaviour() {
        ship.getMutableStats().getShieldUnfoldRateMult().modifyPercent(this.toString(),-25f);
        ship.getMutableStats().getShieldTurnRateMult().modifyPercent(this.toString(), -25f);
    }

    public void unapplyActiveStatBehaviour() {
        ship.getMutableStats().getShieldUnfoldRateMult().unmodify(this.toString());
        ship.getMutableStats().getShieldTurnRateMult().unmodify(this.toString());
    }

    public PSE_DroneManagerPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (plugin == null) return "NULL";

        int reserve = plugin.getReserveDroneCount();
        //String volume = reserve + " / " + (maxDeployedDrones - 1);

        if (reserve < maxDeployedDrones) {
            return "DRONE FORGING";
        } else if (reserve > maxDeployedDrones) {
            return "OVER DRONE FORGE CAPACITY";
        } else {
            return "AT DRONE FORGE CAPACITY";
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
            plugin = new PSE_DroneManagerPlugin(this);
            Global.getCombatEngine().addPlugin(plugin);
        }
    }

    protected void maintainSystemStateStatus(String state) {
        Global.getCombatEngine().maintainStatusForPlayerShip(STATUS_DISPLAY_KEY, STATUS_DISPLAY_SPRITE, STATUS_DISPLAY_TITLE, state, false);
    }
}