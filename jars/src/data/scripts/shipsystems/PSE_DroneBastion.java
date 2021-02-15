package data.scripts.shipsystems;

import data.scripts.util.PSE_MiscUtils;

public class PSE_DroneBastion extends PSE_BaseDroneSystem {
    //static final float FLUX_PER_SECOND = 100f;
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_DroneBastion_";

    public enum BastionDroneOrders {
        FRONT,
        CARDINAL,
        RECALL
    }

    private BastionDroneOrders droneOrders = BastionDroneOrders.RECALL;

    public PSE_DroneBastion() {
        uniqueSystemPrefix = UNIQUE_SYSTEM_PREFIX;

        maxDeployedDrones = PSE_MiscUtils.PSE_BastionSpecLoading.getMaxDeployedDrones();
        launchDelay = (float) PSE_MiscUtils.PSE_BastionSpecLoading.getLaunchDelay();
        launchSpeed = (float) PSE_MiscUtils.PSE_BastionSpecLoading.getLaunchSpeed();
        droneVariant = PSE_MiscUtils.PSE_BastionSpecLoading.getDroneVariant();
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
}