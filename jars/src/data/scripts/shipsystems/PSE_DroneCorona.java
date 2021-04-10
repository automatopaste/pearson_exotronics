package data.scripts.shipsystems;

import data.scripts.PSEDrone;
import data.scripts.ai.PSE_BaseDroneAI;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.ai.PSE_DroneCoronaDroneAI;
import data.scripts.util.PSE_SpecLoadingUtils;

import java.awt.*;

public class PSE_DroneCorona extends PSE_BaseDroneSystem {
    public static final String UNIQUE_SYSTEM_PREFIX = "PSE_droneCorona";

    private Color defaultShieldColour;

    public enum CoronaDroneOrders {
        DEPLOY,
        ATTACK,
        RECALL
    }

    private CoronaDroneOrders droneOrders = CoronaDroneOrders.RECALL;

    public PSE_DroneCorona() {
        systemID = UNIQUE_SYSTEM_PREFIX;

        if (ship != null && ship.getShield() != null) {
            defaultShieldColour = ship.getShield().getInnerColor();
        } else {
            defaultShieldColour = new Color(255, 255, 255, 255);
        }

        loadSpecData();
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

    @Override
    public void executePerOrders(float amount) {
        if (ship.getShield() != null) {
            defaultShieldColour = ship.getShield().getInnerColor();
        } else {
            defaultShieldColour = new Color(255, 255, 255, 255);
        }

        if (droneOrders.equals(PSE_DroneCorona.CoronaDroneOrders.ATTACK)) {
            ship.setJitterShields(false);
            ship.setJitterUnder(ship, new Color(0x00D99D), 1f, 8, 1f, 2f);
            if (ship.getShield() != null) ship.getShield().setInnerColor(new Color(74, 236, 213, 193));

            ship.getMutableStats().getZeroFluxSpeedBoost().modifyMult(this.toString(), 0f);
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(this.toString(), 1.35f);

            if (ship.equals(engine.getPlayerShip())) {
                if (ship.getShield() != null) engine.maintainStatusForPlayerShip("PSE_coronaBoost2", "graphics/icons/hullsys/infernium_injector.png", "SHIELD POWER DIVERTED", "+35% DAMAGE TO SHIELDS", true);
                engine.maintainStatusForPlayerShip("PSE_coronaBoost1", "graphics/icons/hullsys/infernium_injector.png", "ENGINE POWER DIVERTED", "ZERO FLUX BOOST DISABLED", true);
            }
        } else {
            ship.getMutableStats().getZeroFluxSpeedBoost().unmodify(this.toString());
            ship.getMutableStats().getShieldDamageTakenMult().unmodify(this.toString());

            if (ship.getShield() != null) ship.getShield().setInnerColor(defaultShieldColour);
        }
    }

    @Override
    public PSE_BaseDroneAI getNewAIInstance(PSEDrone spawnedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        return new PSE_DroneCoronaDroneAI(spawnedDrone, baseDroneSystem);
    }
}