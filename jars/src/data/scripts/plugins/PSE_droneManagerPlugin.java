package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.PSEDroneAPI;
import data.scripts.ai.PSE_droneCoronaDroneAI;
import data.scripts.shipsystems.PSE_droneCorona;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class PSE_droneManagerPlugin extends BaseEveryFrameCombatPlugin {
    enum PSE_DroneSystemTypes {
        CORONA
    }

    private CombatEngineAPI engine;
    private PSE_DroneSystemTypes droneSystemType;
    IntervalUtil tracker;
    private PSE_droneCorona coronaSystem;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private String droneVariant;
    private float launchSpeed;
    private float maxDeployedDrones;

    private ArrayList<PSEDroneAPI> toRemove = new ArrayList<>();

    public PSE_droneManagerPlugin(Object object, float maxDeployedDrones, float launchDelay, float launchSpeed, ShipAPI ship, String droneVariant) {
        if (object instanceof PSE_droneCorona) {
            this.coronaSystem = (PSE_droneCorona) object;
            this.droneSystemType = PSE_DroneSystemTypes.CORONA;
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

        int numDronesActive;
        switch (droneSystemType) {
            case CORONA:
                coronaSystem.maintainStatusMessage();

                numDronesActive = coronaSystem.getDeployedDrones().size();

                //ammo tracking
                // offset the silly automatic subtraction when system activated
                if (system.isActive() && !isActivePreviousFrame) {
                    system.setAmmo(system.getAmmo() + 1);
                }
                //make sure total drone count can not go above maximum ammo
                if (system.getAmmo() > system.getMaxAmmo() - numDronesActive) {
                    system.setAmmo(system.getMaxAmmo() - numDronesActive);
                }


                //remove inactive drones from list
                for (PSEDroneAPI drone : coronaSystem.getDeployedDrones()) {
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
                        coronaSystem.getDeployedDrones().remove(drone);
                    }
                }

                if (numDronesActive < maxDeployedDrones && !coronaSystem.getDroneOrders().equals(PSE_droneCorona.CoronaDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        spawnDroneFromShip();

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                if (coronaSystem.getShip().getSystem().getAmmo() == 0 && Keyboard.isKeyDown(Keyboard.KEY_F) && isActivationKeyDownPreviousFrame != Keyboard.isKeyDown(Keyboard.KEY_F)) {
                    coronaSystem.nextDroneOrder();
                }

                isActivePreviousFrame = system.isActive();
                isActivationKeyDownPreviousFrame = Keyboard.isKeyDown(Keyboard.KEY_F);
                break;
        }
    }

    public void spawnDroneFromShip() {
        engine.getFleetManager(ship.getOriginalOwner()).setSuppressDeploymentMessages(true);
        PSEDroneAPI spawnedDrone = new PSEDroneAPI(
                engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing("PSE_deuces_wing", getLandingLocation(), ship.getFacing()),
                ship
        );
        spawnedDrone.setAnimatedLaunch();
        spawnedDrone.setLaunchingShip(ship);

        Vector2f launchVelocity = ship.getVelocity();
        VectorUtils.clampLength(launchVelocity, launchSpeed);
        spawnedDrone.getVelocity().set(launchVelocity);

        spawnedDrone.setShipAI(new PSE_droneCoronaDroneAI(spawnedDrone.getFleetMember(), spawnedDrone));

        spawnedDrone.setDroneSource(ship);
        spawnedDrone.setDrone();

        coronaSystem.getDeployedDrones().add(spawnedDrone);

        engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(false);
    }

    public WeaponAPI getLandingBayWeaponAPI() {
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (!weapons.isEmpty()) {
            //these aren't actually bays, but since launch bays have no way of getting their location deco weapons are used
            List<WeaponAPI> bays = new ArrayList<>();
            for (WeaponAPI weapon : weapons) {
                if (weapon.getType().equals(WeaponAPI.WeaponType.DECORATIVE)) {
                    bays.add(weapon);
                }
            }

            //pick random entry in bay list
            int index = new Random().nextInt(bays.size());
            return bays.get(index);
        }
        return null;
    }

    public Vector2f getLandingLocation() {
        if (getLandingBayWeaponAPI() != null) {
            return getLandingBayWeaponAPI().getLocation();
        }
        return ship.getLocation();
    }
}
