package data.scripts.shipsystems;

import data.scripts.util.PSE_MiscUtils;

public class PSE_DroneCitadel extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_DroneCitadel_";

    public enum CitadelDroneOrders {
        ANTI_FIGHTER,
        SHIELD,
        RECALL
    }

    private final static float ORBIT_BASE_ROTATION_SPEED = 30f;
    private float orbitAngleMovementBase = 0f;

    private CitadelDroneOrders droneOrders = CitadelDroneOrders.RECALL;

    public PSE_DroneCitadel() {
        uniqueSystemPrefix = UNIQUE_SYSTEM_PREFIX;

        maxDeployedDrones = PSE_MiscUtils.PSE_CitadelSpecLoading.getMaxDeployedDrones();
        launchDelay = (float) PSE_MiscUtils.PSE_CitadelSpecLoading.getLaunchDelay();
        launchSpeed = (float) PSE_MiscUtils.PSE_CitadelSpecLoading.getLaunchSpeed();
        droneVariant = PSE_MiscUtils.PSE_CitadelSpecLoading.getDroneVariant();
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
}