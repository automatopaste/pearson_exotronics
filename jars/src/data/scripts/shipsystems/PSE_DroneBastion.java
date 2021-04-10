package data.scripts.shipsystems;

import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.util.PSE_SpecLoadingUtils;

public class PSE_DroneBastion extends PSE_BaseDroneSystem {
    //static final float FLUX_PER_SECOND = 100f;
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_droneBastion";

    public enum BastionDroneOrders {
        FRONT,
        CARDINAL,
        RECALL
    }

    private BastionDroneOrders droneOrders = BastionDroneOrders.RECALL;

    public PSE_DroneBastion() {
        systemID = UNIQUE_SYSTEM_PREFIX;

        loadSpecData();
    }

    public BastionDroneOrders getDroneOrders() {
        return droneOrders;
    }

    @Override
    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public void setDroneOrders(BastionDroneOrders droneOrders) {
        this.droneOrders = droneOrders;
    }

    public BastionDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == BastionDroneOrders.values().length - 1) {
            return BastionDroneOrders.values()[0];
        }
        return BastionDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public void maintainStatusMessage() {
        switch (droneOrders) {
            case FRONT:
                maintainSystemStateStatus("FRONT FORMATION");
                break;
            case CARDINAL:
                maintainSystemStateStatus("CARDINAL FORMATION");
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
        return droneOrders == BastionDroneOrders.RECALL;
    }

    @Override
    public void setDefaultDeployMode() {
        setDroneOrders(BastionDroneOrders.FRONT);
    }

    @Override
    public void executePerOrders(float amount) {
        //unneeded
    }

    @Override
    public PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        return new PSE_DroneBastionDroneAI(spawnedDrone, baseDroneSystem);
    }
}