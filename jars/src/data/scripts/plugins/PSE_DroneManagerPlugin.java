package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.ai.PSE_DroneCoronaDroneAI;
import data.scripts.ai.PSE_DroneModularVectorAssemblyDroneAI;
import data.scripts.ai.PSE_DroneShroudDroneAI;
import data.scripts.shipsystems.PSE_DroneBastion;
import data.scripts.shipsystems.PSE_DroneCorona;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import data.scripts.shipsystems.PSE_DroneShroud;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PSE_DroneManagerPlugin extends BaseEveryFrameCombatPlugin {
    public enum DroneSystemTypes {
        CORONA,
        BASTION,
        MVA,
        SHROUD
    }

    private PSE_DroneCorona coronaSystem;
    private PSE_DroneBastion bastionSystem;
    private PSE_DroneModularVectorAssembly MVASystem;
    private PSE_DroneShroud shroudSystem;

    private CombatEngineAPI engine;
    private DroneSystemTypes droneSystemType;
    private IntervalUtil tracker;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private String droneVariant;
    private float launchSpeed;
    private float maxDeployedDrones;
    private int reserveDroneCount;

    private ArrayList<PSEDrone> toRemove = new ArrayList<>();

    public PSE_DroneManagerPlugin(Object object, float maxDeployedDrones, float launchDelay, float launchSpeed, ShipAPI ship, String droneVariant) {
        if (object instanceof PSE_DroneCorona) {
            this.coronaSystem = (PSE_DroneCorona) object;
            this.droneSystemType = DroneSystemTypes.CORONA;
        } else if (object instanceof PSE_DroneBastion) {
            this.bastionSystem = (PSE_DroneBastion) object;
            this.droneSystemType = DroneSystemTypes.BASTION;
        } else if (object instanceof PSE_DroneModularVectorAssembly) {
            this.MVASystem = (PSE_DroneModularVectorAssembly) object;
            this.droneSystemType = DroneSystemTypes.MVA;
        } else  if (object instanceof PSE_DroneShroud) {
            this.shroudSystem = (PSE_DroneShroud) object;
            this.droneSystemType = DroneSystemTypes.SHROUD;
        } else {
            throw new NullPointerException("Unlucky: PSE undefined system launcher");
        }

        this.tracker = new IntervalUtil(launchDelay, launchDelay);

        this.launchSpeed = launchSpeed;
        this.ship = ship;
        this.system = ship.getSystem();
        this.droneVariant = droneVariant;
        this.maxDeployedDrones = maxDeployedDrones;
        this.reserveDroneCount = (int) maxDeployedDrones;
        this.engine = Global.getCombatEngine();
    }

    private boolean isActivePreviousFrame = false;
    private boolean isActivationKeyDownPreviousFrame = false;

    public void advance(float amount, List<InputEventAPI> events) {
        tracker.advance(amount);

        if (engine.isPaused()) {
            return;
        }
        if (ship == null || !ship.isAlive()) {
            return;
        }

        boolean isFKeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(Global.getSettings().getControlStringForEnumName("SHIP_USE_SYSTEM")));

        int numDronesActive;
        ArrayList<PSEDrone> deployedDrones;
        switch (droneSystemType) {
            case CORONA:
                if (engine.getPlayerShip().equals(ship)) {
                    coronaSystem.maintainStatusMessage();
                }
                deployedDrones = coronaSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo();

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    coronaSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.DEPLOY);
                }

                if (numDronesActive < maxDeployedDrones && !coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.RECALL) && reserveDroneCount > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        coronaSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        reserveDroneCount -= 1;
                    }
                }

                if (reserveDroneCount <= 0 && ship.equals(engine.getPlayerShip()) && isFKeyDown && !isActivationKeyDownPreviousFrame) {
                    coronaSystem.nextDroneOrder();
                }

                ship.getMutableStats().getZeroFluxSpeedBoost().unmodify(this.toString());
                if (coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.ATTACK)) {
                    ship.setJitterShields(false);
                    ship.setJitterUnder(ship, new Color(0x00D99D), 1f, 8, 1f, 2f);

                    ship.getMutableStats().getZeroFluxSpeedBoost().modifyMult(this.toString(), 0f);

                    if (ship.equals(engine.getPlayerShip())) {
                        engine.maintainStatusForPlayerShip(this, "graphics/icons/hullsys/infernium_injector.png", "ENGINE POWER DIVERTED", "ZERO FLUX BOOST DISABLED", true);
                    }
                }

                updateDeployedDrones(deployedDrones);

                coronaSystem.setDeployedDrones(deployedDrones);

                system.setAmmo(reserveDroneCount);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);

                break;
            case BASTION:
                if (engine.getPlayerShip().equals(ship)) {
                    bastionSystem.maintainStatusMessage();
                }

                deployedDrones = bastionSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo();

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    bastionSystem.setDroneOrders(PSE_DroneBastion.BastionDroneOrders.FRONT);
                }

                if (numDronesActive < maxDeployedDrones && !bastionSystem.getDroneOrders().equals(PSE_DroneBastion.BastionDroneOrders.RECALL) && reserveDroneCount > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        bastionSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        reserveDroneCount -= 1;
                    }
                }

                if (reserveDroneCount <= 0 && ship.equals(engine.getPlayerShip()) && isFKeyDown && !isActivationKeyDownPreviousFrame) {
                    bastionSystem.nextDroneOrder();
                }

                updateDeployedDrones(deployedDrones);

                bastionSystem.setDeployedDrones(deployedDrones);

                system.setAmmo(reserveDroneCount);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);

                break;
            case MVA:
                if (engine.getPlayerShip().equals(ship)) {
                    MVASystem.maintainStatusMessage();
                }

                deployedDrones = MVASystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo();

                if (numDronesActive < maxDeployedDrones && !MVASystem.getDroneOrders().equals(PSE_DroneModularVectorAssembly.ModularVectorAssemblyDroneOrders.RECALL) && reserveDroneCount > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        MVASystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        reserveDroneCount -= 1;
                    }
                }

                if (reserveDroneCount <= 0 && ship.equals(engine.getPlayerShip()) && isFKeyDown && !isActivationKeyDownPreviousFrame) {
                    MVASystem.nextDroneOrder();
                }

                updateDeployedDrones(deployedDrones);

                MVASystem.setDeployedDrones(deployedDrones);

                system.setAmmo(reserveDroneCount);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);
                break;
            case SHROUD:
                if (engine.getPlayerShip().equals(ship)) {
                    shroudSystem.maintainStatusMessage();
                }

                deployedDrones = shroudSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo();

                //check if can spawn new drone
                if (numDronesActive < maxDeployedDrones && !shroudSystem.getDroneOrders().equals(PSE_DroneShroud.ShroudDroneOrders.RECALL) && reserveDroneCount > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        shroudSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        reserveDroneCount -= 1;
                    }
                }

                if (reserveDroneCount <= 0 && ship.equals(engine.getPlayerShip()) && isFKeyDown && !isActivationKeyDownPreviousFrame) {
                    shroudSystem.nextDroneOrder();
                }

                updateDeployedDrones(deployedDrones);

                shroudSystem.setDeployedDrones(deployedDrones);

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    if (shroudSystem.getDroneOrders().equals(PSE_DroneShroud.ShroudDroneOrders.BROADSIDE_MOVEMENT)) {
                        shroudSystem.setDroneOrders(PSE_DroneShroud.ShroudDroneOrders.CIRCLE);
                    }
                }

                switch (shroudSystem.getDroneOrders()) {
                    case CIRCLE:
                        shroudSystem.advanceOrbitAngleBase(amount);
                        break;
                    case BROADSIDE_MOVEMENT:
                        shroudSystem.resetOrbitAngleBase();
                        break;
                    case RECALL:
                        break;
                }

                system.setAmmo(reserveDroneCount);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);
        }

        isActivePreviousFrame = system.isActive();
        isActivationKeyDownPreviousFrame = isFKeyDown;
    }

    private void trackSystemAmmo() {
        //ammo tracking
        // offset the silly automatic subtraction when system activated
        if (system.isActive() && !isActivePreviousFrame) {
            system.setAmmo(system.getAmmo() + 1);
        }
        /*//make sure total drone count can not go above maximum ammo
        if (reserveDroneCount > system.getMaxAmmo() - numDronesActive) {
            system.setAmmo(system.getMaxAmmo() - numDronesActive);
        }*/
    }

    private void updateDeployedDrones(ArrayList<PSEDrone> list) {
        //remove inactive drones from list
        for (PSEDrone drone : list) {
            if (!drone.isAlive()) {
                toRemove.add(drone);
                continue;
            }
            //when drone has finished landing/shrinking animation
            if (drone.isFinishedLanding()) {
                //add to system ammo count / reserve
                reserveDroneCount += 1;

                drone.remove();

                toRemove.add(drone);
            }
        }
        if (!toRemove.isEmpty()) {
            for (PSEDrone drone : toRemove) {
                list.remove(drone);
            }
        }
    }

    private PSEDrone spawnDroneFromShip(String specID) {
        engine.getFleetManager(ship.getOriginalOwner()).setSuppressDeploymentMessages(true);

        Vector2f location;
        float facing;
        if (getLandingBayWeaponSlotAPI() != null) {
            WeaponSlotAPI slot = getLandingBayWeaponSlotAPI();
            location = slot.computePosition(ship);
            facing = slot.getAngle();
        } else {
            location = ship.getLocation();
            facing = ship.getFacing();
        }

        //CombatFleetManager m = (CombatFleetManager) engine.getFleetManager(FleetSide.PLAYER);
        //m.spawnShipOrWing();

        PSEDrone spawnedDrone = new PSEDrone(
                engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(specID, location, facing),
                ship
        );
        spawnedDrone.setAnimatedLaunch();
        spawnedDrone.setLaunchingShip(ship);

        Vector2f launchVelocity = ship.getVelocity();
        VectorUtils.clampLength(launchVelocity, launchSpeed);
        spawnedDrone.getVelocity().set(launchVelocity);

        switch (droneSystemType) {
            case CORONA:
                spawnedDrone.setShipAI(new PSE_DroneCoronaDroneAI(spawnedDrone));
                break;
            case BASTION:
                spawnedDrone.setShipAI(new PSE_DroneBastionDroneAI(spawnedDrone));
                break;
            case MVA:
                spawnedDrone.setShipAI(new PSE_DroneModularVectorAssemblyDroneAI(spawnedDrone));
                break;
            case SHROUD:
                spawnedDrone.setShipAI(new PSE_DroneShroudDroneAI(spawnedDrone));
                break;
        }

        spawnedDrone.setDroneSource(ship);
        spawnedDrone.setDrone();

        engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(false);
        return spawnedDrone;
    }

    public WeaponSlotAPI getLandingBayWeaponSlotAPI() {
        List<WeaponSlotAPI> weapons = ship.getHullSpec().getAllWeaponSlotsCopy();
        if (!weapons.isEmpty()) {
            //these aren't actually bays, but since launch bays have no way of getting their location system mounts are used
            List<WeaponSlotAPI> bays = new ArrayList<>();
            for (WeaponSlotAPI weapon : weapons) {
                if (weapon.isSystemSlot()) {
                    bays.add(weapon);
                }
            }

            if (!bays.isEmpty()) {
                //pick random entry in bay list
                Random index = new Random();
                return bays.get(index.nextInt(bays.size()));
            }
        }
        return null;
    }

    public Vector2f getLandingLocation() {
        if (getLandingBayWeaponSlotAPI() != null) {
            return getLandingBayWeaponSlotAPI().computePosition(ship);
        }
        return ship.getLocation();
    }
}
