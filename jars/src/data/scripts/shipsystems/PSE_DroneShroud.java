package data.scripts.shipsystems;

import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.ai.PSE_DroneShroudDroneAI;
import data.scripts.util.PSE_SpecLoadingUtils;

import java.util.Arrays;

public class PSE_DroneShroud extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_droneShroud";

    public enum ShroudDroneOrders {
        CIRCLE,
        BROADSIDE_MOVEMENT,
        RECALL
    }

    //private final static float ORBIT_BASE_ROTATION_SPEED = 25f;
    //private float orbitAngleMovementBase = 0f;
    private final float[] orbitBaseRotationSpeed;
    public float[] orbitAngles;

    private ShroudDroneOrders droneOrders = ShroudDroneOrders.RECALL;

    public PSE_DroneShroud() {
        systemID = UNIQUE_SYSTEM_PREFIX;

        orbitBaseRotationSpeed = PSE_SpecLoadingUtils.PSE_ShroudSpecLoading.getOrbitBaseRotationSpeed();

        loadSpecData();

        orbitAngles = new float[maxDeployedDrones];
        for (int i = 0; i < maxDeployedDrones; i++) {
            orbitAngles[i] = 0f;
        }
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

    public void advanceOrbitAngles(float amount) {
        /*orbitAngleMovementBase += (ORBIT_BASE_ROTATION_SPEED * amount);
        if (orbitAngleMovementBase >= 360f) {
            orbitAngleMovementBase -= 360f;
        }*/

        for (int i = 0; i < maxDeployedDrones; i++) {
            float angle = orbitAngles[i];
            angle += orbitBaseRotationSpeed[i] * amount;
            if (angle >= 360f) angle -= 360f;
            orbitAngles[i] = angle;
        }
    }

    public void resetOrbitAngleBase() {
        //orbitAngleMovementBase = 0f;
        for (int i = 0; i < maxDeployedDrones; i++) {
            orbitAngles[i] = 0f;
        }
    }

    public float getOrbitAngleBase(int index) {
        //return orbitAngleMovementBase;
        return orbitAngles[index];
    }

    @Override
    public void executePerOrders(float amount) {
        switch (droneOrders) {
            case CIRCLE:
                advanceOrbitAngles(amount);
                break;
            case BROADSIDE_MOVEMENT:
                resetOrbitAngleBase();
                break;
            case RECALL:
                break;
        }
    }

    @Override
    public PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        return new PSE_DroneShroudDroneAI(spawnedDrone, baseDroneSystem);
    }
}