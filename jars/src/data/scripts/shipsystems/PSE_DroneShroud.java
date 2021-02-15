package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.PSEDrone;
import data.scripts.PSEModPlugin;
import data.scripts.plugins.PSE_DroneManagerPlugin;

import java.util.ArrayList;

public class PSE_DroneShroud extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_DroneShroud_";

    public enum ShroudDroneOrders {
        CIRCLE,
        BROADSIDE_MOVEMENT,
        RECALL
    }

    private final static float ORBIT_BASE_ROTATION_SPEED = 25f;
    private float orbitAngleMovementBase = 0f;

    private ShroudDroneOrders droneOrders = ShroudDroneOrders.RECALL;

    public PSE_DroneShroud() {
        maxDeployedDrones = 5;
        launchDelay = 0.1f;
        launchSpeed = 10f;
        droneVariant = "PSE_kingston_wing";
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

    public ShroudDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == ShroudDroneOrders.values().length - 1) {
            return ShroudDroneOrders.values()[0];
        }
        return ShroudDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public void maintainStatusMessage() {
        switch (droneOrders) {
            case CIRCLE:
                maintainSystemStateStatus("CIRCLE FORMATION");
                break;
            case BROADSIDE_MOVEMENT:
                maintainSystemStateStatus("BROADSIDE FORMATION");
                engine.maintainStatusForPlayerShip("SHROUD_STAT_KEY_2", STATUS_DISPLAY_SPRITE, "DRONE-ASSISTED MANEUVERS", "DRIVE FIELD BOOSTED", false);
                break;
            case RECALL:
                if (deployedDrones.isEmpty()) {
                    maintainSystemStateStatus("DRONES RECALLED");
                } else {
                    maintainSystemStateStatus("RECALLING DRONES");
                }
                break;
        }
    }

    @Override
    public boolean isRecallMode() {
        return droneOrders == ShroudDroneOrders.RECALL;
    }

    @Override
    public void setDefaultDeployMode() {
        setDroneOrders(ShroudDroneOrders.CIRCLE);
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