package data.scripts.shipsystems;

import data.scripts.util.PSE_MiscUtils;

public class PSE_DroneCorona extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_DroneCorona_";

    public enum CoronaDroneOrders {
        DEPLOY,
        ATTACK,
        RECALL
    }

    private CoronaDroneOrders droneOrders = CoronaDroneOrders.RECALL;

    public PSE_DroneCorona() {
        uniqueSystemPrefix = UNIQUE_SYSTEM_PREFIX;

        maxDeployedDrones = PSE_MiscUtils.PSE_CoronaSpecLoading.getMaxDeployedDrones();
        launchDelay = (float) PSE_MiscUtils.PSE_CoronaSpecLoading.getLaunchDelay();
        launchSpeed = (float) PSE_MiscUtils.PSE_CoronaSpecLoading.getLaunchSpeed();
        droneVariant = PSE_MiscUtils.PSE_CoronaSpecLoading.getDroneVariant();
    }

    public CoronaDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public void setDroneOrders(CoronaDroneOrders droneOrders) {
        this.droneOrders = droneOrders;
    }

    private CoronaDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == CoronaDroneOrders.values().length - 1) {
            return CoronaDroneOrders.values()[0];
        }
        return CoronaDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public void maintainStatusMessage() {
        switch (droneOrders) {
            case DEPLOY:
                maintainSystemStateStatus("DEFENCE FORMATION");
                break;
            case ATTACK:
                maintainSystemStateStatus("FOCUS FORMATION");
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
        return false;
    }

    @Override
    public void setDefaultDeployMode() {

    }

    @Override
    public void applyActiveStatBehaviour() {
        super.applyActiveStatBehaviour();

        if (ship.getShield() != null && ship.equals(engine.getPlayerShip())) {
            engine.maintainStatusForPlayerShip(
                    "PSE_shieldDebuffStat",
                    "graphics/icons/hullsys/fortress_shield.png",
                    "SHIELD POWER DIVERTED",
                    "-25% TURN RATE",
                    true
            );
        }
    }
}