package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.SPEDroneAPI;
import data.scripts.ai.SPE_droneCoronaDroneAI;
import data.scripts.ai.SPE_dummyAI;
import data.scripts.shipsystems.SPE_droneCorona;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SPE_droneManagerPlugin extends BaseEveryFrameCombatPlugin {
    private enum SPE_DroneSystemTypes {
        CORONA
    }

    private CombatEngineAPI engine;
    private SPE_DroneSystemTypes droneSystemType;
    IntervalUtil tracker;
    private SPE_droneCorona coronaSystem;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private String droneVariant;
    private float launchSpeed;
    private float maxDeployedDrones;

    private ArrayList<SPEDroneAPI> toRemove = new ArrayList<>();

    public SPE_droneManagerPlugin(Object object, float maxDeployedDrones, float launchDelay, float launchSpeed, ShipAPI ship, String droneVariant) {
        if (object instanceof SPE_droneCorona) {
            this.coronaSystem = (SPE_droneCorona) object;
            this.droneSystemType = SPE_DroneSystemTypes.CORONA;
        } else {
            throw new NullPointerException("Unlucky: SPE undefined system launcher");
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

    public void advance(float amount, List<InputEventAPI> events) {
        tracker.advance(amount);

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
                    system.setAmmo(system.getAmmo() - 1);
                }


                //remove inactive drones from list
                for (SPEDroneAPI drone : coronaSystem.getDeployedDrones()) {
                    if (!drone.isAlive()) {
                        toRemove.add(drone);
                        continue;
                    }
                    //when drone has finished landing/shrinking animation
                    if (drone.isFinishedLanding()) {
                        //add to system ammo count / reserve
                        system.setAmmo(system.getAmmo() + 1);

                        //set AI to one with no moving parts that would cause nullpointers
                        drone.setShipAI(new SPE_dummyAI());
                        drone.remove();

                        toRemove.add(drone);
                    }
                }
                if (!toRemove.isEmpty()) {
                    for (SPEDroneAPI drone : toRemove) {
                        coronaSystem.getDeployedDrones().remove(drone);
                    }
                }

                if (numDronesActive < maxDeployedDrones && !coronaSystem.getDroneOrders().equals(SPE_droneCorona.CoronaDroneOrders.RECALL) && system.getAmmo() > 0) {
                    if (tracker.getElapsed() >= tracker.getIntervalDuration()) {
                        tracker.setElapsed(0);
                        spawnDroneFromShip();

                        //subtract from reserve drone count on launch
                        system.setAmmo(system.getAmmo() - 1);
                    }
                }

                isActivePreviousFrame = system.isActive();
                break;
        }
    }

    public void spawnDroneFromShip() {
        engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(true);
        SPEDroneAPI spawnedDrone = new SPEDroneAPI(
                engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing("SPE_deuces_wing", ship.getLocation(), ship.getFacing()),
                ship
        );
        spawnedDrone.setAnimatedLaunch();
        spawnedDrone.setLaunchingShip(ship);

        Vector2f launchVelocity = ship.getVelocity();
        VectorUtils.clampLength(launchVelocity, launchSpeed);
        spawnedDrone.getVelocity().set(launchVelocity);

        spawnedDrone.setShipAI(new SPE_droneCoronaDroneAI(spawnedDrone.getFleetMember(), spawnedDrone));

        spawnedDrone.setDroneSource(ship);
        spawnedDrone.setDrone();

        coronaSystem.getDeployedDrones().add(spawnedDrone);

        engine.getFleetManager(FleetSide.PLAYER).setSuppressDeploymentMessages(false);
    }

    //unused, but useful when i get around to it
    public FighterLaunchBayAPI getLandingBay(ShipAPI mothership) {
        List<FighterLaunchBayAPI> bays = mothership.getLaunchBaysCopy();
        if (!bays.isEmpty()) {
            int index = new Random().nextInt(bays.size() + 1);
            return bays.get(index);
        }
        return null;
    }
}
