package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDroneAPI;
import data.scripts.ai.PSE_DroneBastionDroneAI;
import data.scripts.ai.PSE_DroneCoronaDroneAI;
import data.scripts.shipsystems.PSE_DroneBastion;
import data.scripts.shipsystems.PSE_DroneCorona;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PSE_DroneManagerPlugin extends BaseEveryFrameCombatPlugin {
    enum PSE_DroneSystemTypes {
        CORONA,
        BASTION
    }

    private CombatEngineAPI engine;
    private PSE_DroneSystemTypes droneSystemType;
    IntervalUtil tracker;
    private PSE_DroneCorona coronaSystem;
    private PSE_DroneBastion bastionSystem;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private String droneVariant;
    private float launchSpeed;
    private float maxDeployedDrones;

    private ArrayList<PSEDroneAPI> toRemove = new ArrayList<>();

    public PSE_DroneManagerPlugin(Object object, float maxDeployedDrones, float launchDelay, float launchSpeed, ShipAPI ship, String droneVariant) {
        if (object instanceof PSE_DroneCorona) {
            this.coronaSystem = (PSE_DroneCorona) object;
            this.droneSystemType = PSE_DroneSystemTypes.CORONA;
        } else if (object instanceof PSE_DroneBastion) {
            this.bastionSystem = (PSE_DroneBastion) object;
            this.droneSystemType = PSE_DroneSystemTypes.BASTION;
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
        if (ship == null) {
            return;
        }

        int numDronesActive;
        ArrayList<PSEDroneAPI> deployedDrones;
        switch (droneSystemType) {
            case CORONA:
                coronaSystem.maintainStatusMessage();

                deployedDrones = coronaSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                getModifiedDeployedDrones(deployedDrones);

                if (numDronesActive < maxDeployedDrones && !coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        coronaSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (coronaSystem.getShip().getSystem().getAmmo() == 0 && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    coronaSystem.nextDroneOrder();
                }

                if (coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.ATTACK)) {
                    ship.setJitterShields(false);
                    ship.setJitterUnder(ship, new Color(0x00D99D), 1f, 8, 1f, 2f);

                    PSE_MiscUtils.applyFluxPerSecondPerFrame(ship, coronaSystem.getFluxPerSecond(), amount);
                }

                coronaSystem.setDeployedDrones(deployedDrones);

                break;
            case BASTION:
                bastionSystem.maintainStatusMessage();

                deployedDrones = bastionSystem.getDeployedDrones();
                numDronesActive = deployedDrones.size();

                trackSystemAmmo(numDronesActive);

                getModifiedDeployedDrones(getModifiedDeployedDrones(deployedDrones));

                if (numDronesActive < maxDeployedDrones && !bastionSystem.getDroneOrders().equals(PSE_DroneBastion.BastionDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        bastionSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (bastionSystem.getShip().getSystem().getAmmo() == 0 && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    bastionSystem.nextDroneOrder();
                }

                bastionSystem.setDeployedDrones(getModifiedDeployedDrones(deployedDrones));
                break;
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

    public ArrayList<PSEDroneAPI> getModifiedDeployedDrones(ArrayList<PSEDroneAPI> list) {
        //remove inactive drones from list
        for (PSEDroneAPI drone : list) {
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
            for (PSEDroneAPI drone : toRemove) {
                list.remove(drone);
            }
        }
        return list;
    }

    public PSEDroneAPI spawnDroneFromShip(String specID) {
        engine.getFleetManager(ship.getOriginalOwner()).setSuppressDeploymentMessages(true);
        PSEDroneAPI spawnedDrone = new PSEDroneAPI(
                engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(specID, getLandingLocation(), ship.getFacing()),
                ship
        );
        spawnedDrone.setAnimatedLaunch();
        spawnedDrone.setLaunchingShip(ship);

        Vector2f launchVelocity = ship.getVelocity();
        VectorUtils.clampLength(launchVelocity, launchSpeed);
        spawnedDrone.getVelocity().set(launchVelocity);

        if (droneSystemType.equals(PSE_DroneSystemTypes.CORONA)) {
            spawnedDrone.setShipAI(new PSE_DroneCoronaDroneAI(spawnedDrone));
        } else if (droneSystemType.equals(PSE_DroneSystemTypes.BASTION)) {
            spawnedDrone.setShipAI(new PSE_DroneBastionDroneAI(spawnedDrone));
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
