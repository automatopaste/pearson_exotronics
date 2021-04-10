package data.scripts.shipsystems;

import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.ai.PSE_DroneModularVectorAssemblyDroneAI;

import java.util.ArrayList;

public class PSE_DroneModularVectorAssembly extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_droneMVA";

    public enum ModularVectorAssemblyDroneOrders {
        TARGETING,
        CLAMPED,
        RECALL
    }

    public ArrayList<PSEDrone> deployedDrones = new ArrayList<>();

    private ModularVectorAssemblyDroneOrders droneOrders = ModularVectorAssemblyDroneOrders.RECALL;

    public PSE_DroneModularVectorAssembly() {
        systemID = UNIQUE_SYSTEM_PREFIX;

        loadSpecData();
    }

    public ModularVectorAssemblyDroneOrders getDroneOrders() {
        return droneOrders;
    }

    public void nextDroneOrder() {
        droneOrders = getNextOrder();
    }

    public ModularVectorAssemblyDroneOrders getNextOrder() {
        if (droneOrders.ordinal() == ModularVectorAssemblyDroneOrders.values().length - 1) {
            return ModularVectorAssemblyDroneOrders.values()[0];
        }
        return ModularVectorAssemblyDroneOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public void maintainStatusMessage() {
        switch (droneOrders) {
            case TARGETING:
                maintainSystemStateStatus("TARGETING FORMATION");
                break;
            case CLAMPED:
                maintainSystemStateStatus("THRUSTER ASSEMBLY");
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
        return droneOrders == ModularVectorAssemblyDroneOrders.RECALL;
    }

    @Override
    public void setDefaultDeployMode() {
        droneOrders = ModularVectorAssemblyDroneOrders.TARGETING;
    }

    @Override
    public void executePerOrders(float amount) {
        //unneeded
    }

    @Override
    public PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        return new PSE_DroneModularVectorAssemblyDroneAI(spawnedDrone, baseDroneSystem);
    }
}