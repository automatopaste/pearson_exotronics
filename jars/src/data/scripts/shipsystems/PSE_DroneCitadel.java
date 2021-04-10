package data.scripts.shipsystems;

import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.ai.PSE_DroneCitadelDroneAI;
import data.scripts.util.PSE_SpecLoadingUtils;

public class PSE_DroneCitadel extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_droneCitadel";

    public enum CitadelDroneOrders {
        ANTI_FIGHTER,
        SHIELD,
        RECALL
    }

    private final static float ORBIT_BASE_ROTATION_SPEED = 30f;
    private float orbitAngleMovementBase = 0f;

    private CitadelDroneOrders droneOrders = CitadelDroneOrders.RECALL;

    public PSE_DroneCitadel() {
        systemID = UNIQUE_SYSTEM_PREFIX;

        loadSpecData();
    }

    private CitadelDroneOrders getNextDroneOrder() {
        if (droneOrders.ordinal() == CitadelDroneOrders.values().length - 1) {
            return CitadelDroneOrders.values()[0];
        }
        return CitadelDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public void nextDroneOrder() {
        droneOrders = getNextDroneOrder();
    }

    @Override
    public void maintainStatusMessage() {
        switch (droneOrders) {
            case ANTI_FIGHTER:
                maintainSystemStateStatus("ANTI-FIGHTER FORMATION");
                break;
            case SHIELD:
                maintainSystemStateStatus("SHIELD FORMATION");
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

    public CitadelDroneOrders getDroneOrders() {
        return droneOrders;
    }

    @Override
    public boolean isRecallMode() {
        return droneOrders == CitadelDroneOrders.RECALL;
    }

    @Override
    public void setDefaultDeployMode() {
        droneOrders = CitadelDroneOrders.SHIELD;
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

    @Override
    public void executePerOrders(float amount) {
        switch (droneOrders) {
            case ANTI_FIGHTER:
            case RECALL:
                resetOrbitAngleBase();
                break;
            case SHIELD:
                advanceOrbitAngleBase(amount);
                break;
        }
    }

    @Override
    public PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        return new PSE_DroneCitadelDroneAI(spawnedDrone, baseDroneSystem);
    }
}