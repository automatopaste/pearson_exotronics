package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
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
    enum PSE_DroneSystemTypes {
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
    private PSE_DroneSystemTypes droneSystemType;
    IntervalUtil tracker;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private String droneVariant;
    private float launchSpeed;
    private float maxDeployedDrones;

    private ArrayList<PSEDrone> toRemove = new ArrayList<>();

    public PSE_DroneManagerPlugin(Object object, float maxDeployedDrones, float launchDelay, float launchSpeed, ShipAPI ship, String droneVariant) {
        if (object instanceof PSE_DroneCorona) {
            this.coronaSystem = (PSE_DroneCorona) object;
            this.droneSystemType = PSE_DroneSystemTypes.CORONA;
        } else if (object instanceof PSE_DroneBastion) {
            this.bastionSystem = (PSE_DroneBastion) object;
            this.droneSystemType = PSE_DroneSystemTypes.BASTION;
        } else if (object instanceof PSE_DroneModularVectorAssembly) {
            this.MVASystem = (PSE_DroneModularVectorAssembly) object;
            this.droneSystemType = PSE_DroneSystemTypes.MVA;
        } else  if (object instanceof PSE_DroneShroud) {
            this.shroudSystem = (PSE_DroneShroud) object;
            this.droneSystemType = PSE_DroneSystemTypes.SHROUD;
        } else {
            throw new NullPointerException("Unlucky: PSE undefined system launcher");
        }

        this.tracker = new IntervalUtil(launchDelay, launchDelay);

        this.launchSpeed = launchSpeed;
        this.ship = ship;
        this.system = ship.getSystem();
        this.droneVariant = droneVariant;
        this.maxDeployedDrones = maxDeployedDrones;
        this.engine = Global.getCombatEngine();
    }

    boolean isActivePreviousFrame = false;
    boolean isActivationKeyDownPreviousFrame = false;

    public void advance(float amount, List<InputEventAPI> events) {
        tracker.advance(amount);

        if (engine.isPaused()) {
            return;
        }
        if (ship == null || !ship.isAlive()) {
            return;
        }

        int numDronesActive;
        ArrayList<PSEDrone> deployedDrones;
        switch (droneSystemType) {
            case CORONA:
                if (engine.getPlayerShip().equals(ship)) {
                    coronaSystem.maintainStatusMessage();
                }
                deployedDrones = coronaSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    coronaSystem.setDroneOrders(PSE_DroneCorona.CoronaDroneOrders.DEPLOY);
                }

                if (numDronesActive < maxDeployedDrones && !coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        coronaSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (coronaSystem.getShip().getSystem().getAmmo() == 0 && ship.equals(engine.getPlayerShip()) && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
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

                deployedDrones = getModifiedDeployedDrones(deployedDrones);

                coronaSystem.setDeployedDrones(deployedDrones);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);

                break;
            case BASTION:
                if (engine.getPlayerShip().equals(ship)) {
                    bastionSystem.maintainStatusMessage();
                }

                deployedDrones = bastionSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    bastionSystem.setDroneOrders(PSE_DroneBastion.BastionDroneOrders.FRONT);
                }

                if (numDronesActive < maxDeployedDrones && !bastionSystem.getDroneOrders().equals(PSE_DroneBastion.BastionDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        bastionSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (bastionSystem.getShip().getSystem().getAmmo() == 0 && ship.equals(engine.getPlayerShip()) && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    bastionSystem.nextDroneOrder();
                }

                deployedDrones = getModifiedDeployedDrones(deployedDrones);

                bastionSystem.setDeployedDrones(deployedDrones);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);

                break;
            case MVA:
                if (engine.getPlayerShip().equals(ship)) {
                    MVASystem.maintainStatusMessage();
                }

                deployedDrones = MVASystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                if (numDronesActive < maxDeployedDrones && !MVASystem.getDroneOrders().equals(PSE_DroneModularVectorAssembly.ModularVectorAssemblyDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        MVASystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (MVASystem.getShip().getSystem().getAmmo() == 0 && ship.equals(engine.getPlayerShip()) && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    MVASystem.nextDroneOrder();
                }

                deployedDrones = getModifiedDeployedDrones(deployedDrones);

                MVASystem.setDeployedDrones(deployedDrones);

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);
                break;
            case SHROUD:
                if (engine.getPlayerShip().equals(ship)) {
                    shroudSystem.maintainStatusMessage();
                }

                deployedDrones = shroudSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                if (numDronesActive < maxDeployedDrones && !shroudSystem.getDroneOrders().equals(PSE_DroneShroud.ShroudDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        shroudSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (shroudSystem.getShip().getSystem().getAmmo() == 0 && ship.equals(engine.getPlayerShip()) && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    shroudSystem.nextDroneOrder();
                }

                deployedDrones = getModifiedDeployedDrones(deployedDrones);

                shroudSystem.setDeployedDrones(deployedDrones);

                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    shroudSystem.setDroneOrders(PSE_DroneShroud.ShroudDroneOrders.CIRCLE);
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

                engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);
        }

        isActivePreviousFrame = system.isActive();
        isActivationKeyDownPreviousFrame = Keyboard.isKeyDown(Keyboard.KEY_F);
    }

    public void trackSystemAmmo(int numDronesActive) {
        //ammo tracking
        // offset the silly automatic subtraction when system activated
        if (system.isActive() && !isActivePreviousFrame) {
            system.setAmmo(system.getAmmo() + 1);
        }
        //make sure total drone count can not go above maximum ammo
        if (system.getAmmo() > system.getMaxAmmo() - numDronesActive) {
            system.setAmmo(system.getMaxAmmo() - numDronesActive);
        }
    }

    public ArrayList<PSEDrone> getModifiedDeployedDrones(ArrayList<PSEDrone> list) {
        //remove inactive drones from list
        for (PSEDrone drone : list) {
            if (!drone.isAlive()) {
                toRemove.add(drone);
                continue;
            }
            //when drone has finished landing/shrinking animation
            if (drone.isFinishedLanding()) {
                //add to system ammo count / reserve
                system.setAmmo(system.getAmmo() + 1);

                drone.remove();

                toRemove.add(drone);
            }
        }
        if (!toRemove.isEmpty()) {
            for (PSEDrone drone : toRemove) {
                list.remove(drone);
            }
        }

        return list;
    }

    public PSEDrone spawnDroneFromShip(String specID) {
        engine.getFleetManager(ship.getOriginalOwner()).setSuppressDeploymentMessages(true);
        PSEDrone spawnedDrone = new PSEDrone(
                engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(specID, getLandingLocation(), ship.getFacing()),
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
