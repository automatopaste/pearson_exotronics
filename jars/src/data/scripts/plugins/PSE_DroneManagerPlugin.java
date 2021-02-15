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
import data.scripts.PSEModPlugin;
import data.scripts.ai.*;
import data.scripts.shipsystems.*;
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
        SHROUD,
        CITADEL
    }

    public static final String LAUNCH_DELAY_STAT_KEY = "PSE_launchDelayStatKey";
    public static final String REGEN_DELAY_STAT_KEY = "PSE_regenDelayStatKey";

    private PSE_DroneCorona coronaSystem;
    private PSE_DroneShroud shroudSystem;
    private PSE_DroneCitadel citadelSystem;

    private final PSE_BaseDroneSystem baseDroneSystem;

    private final CombatEngineAPI engine;
    private final DroneSystemTypes droneSystemType;
    private final IntervalUtil launchTracker;
    private final ShipAPI ship;
    private ShipSystemAPI system;
    private final String droneVariant;
    private final float launchSpeed;
    private final float maxDeployedDrones;
    private int reserveDroneCount;
    private final float baseCooldown;

    private final ArrayList<PSEDrone> toRemove = new ArrayList<>();
    private final Color defaultShieldColour;

    public PSE_DroneManagerPlugin(PSE_BaseDroneSystem baseSystem) {
        if (baseSystem != null) {
            baseDroneSystem = baseSystem;
        } else {
            throw new NullPointerException(PSEModPlugin.MOD_ERROR_PREFIX + "Unlucky: system object null");
        }

        if (baseSystem instanceof PSE_DroneCorona) {
            this.coronaSystem = (PSE_DroneCorona) baseSystem;
            this.droneSystemType = DroneSystemTypes.CORONA;
        } else if (baseSystem instanceof PSE_DroneBastion) {
            this.droneSystemType = DroneSystemTypes.BASTION;
        } else if (baseSystem instanceof PSE_DroneModularVectorAssembly) {
            this.droneSystemType = DroneSystemTypes.MVA;
        } else if (baseSystem instanceof PSE_DroneShroud) {
            this.shroudSystem = (PSE_DroneShroud) baseSystem;
            this.droneSystemType = DroneSystemTypes.SHROUD;
        } else if (baseSystem instanceof PSE_DroneCitadel) {
            this.citadelSystem = (PSE_DroneCitadel) baseSystem;
            this.droneSystemType = DroneSystemTypes.CITADEL;
        } else {
            throw new NullPointerException(PSEModPlugin.MOD_ERROR_PREFIX + "Unlucky: undefined system type");
        }

        this.launchSpeed = baseDroneSystem.launchSpeed;

        this.launchTracker = new IntervalUtil(baseDroneSystem.launchDelay, baseDroneSystem.launchDelay);
        launchTracker.forceIntervalElapsed();

        this.ship = baseDroneSystem.ship;

        this.system = ship.getSystem();
        baseCooldown = system.getCooldown();

        this.droneVariant = baseDroneSystem.droneVariant;
        this.maxDeployedDrones = baseDroneSystem.maxDeployedDrones;
        this.reserveDroneCount = (int) maxDeployedDrones;
        this.engine = Global.getCombatEngine();

        if (ship.getShield() != null) {
            defaultShieldColour = ship.getShield().getInnerColor();
        } else {
            defaultShieldColour = new Color(255, 255, 255, 255);
        }
    }

    private boolean isActivePreviousFrame = false; //prevents triggering twice on first activation
    private boolean isActivationKeyDownPreviousFrame = false;
    private float cooldownLastFrame = 0f;

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused()) {
            return;
        }
        if (ship == null || !ship.isAlive()) {
            return;
        }

        //stat modifications
        float regenDelayStatMod = ship.getMutableStats().getDynamic().getMod(REGEN_DELAY_STAT_KEY).computeEffective(1f);
        float launchDelayStatMod = ship.getMutableStats().getDynamic().getMod(LAUNCH_DELAY_STAT_KEY).computeEffective(1f);
        system = ship.getSystem();
        system.setCooldown(baseCooldown * regenDelayStatMod);

        boolean isActivationKeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(Global.getSettings().getControlStringForEnumName("SHIP_USE_SYSTEM")));

        int numDronesActive;
        ArrayList<PSEDrone> deployedDrones;



        if (engine.getPlayerShip().equals(ship)) {
            baseDroneSystem.maintainStatusMessage();
        }
        deployedDrones = baseDroneSystem.getDeployedDrones();
        numDronesActive = deployedDrones.size();

        trackSystemAmmo();

        if (ship.getFluxTracker().isOverloadedOrVenting()) {
           baseDroneSystem.setDefaultDeployMode();
        }

        //check if can spawn new drone
        if (numDronesActive < maxDeployedDrones && !baseDroneSystem.isRecallMode() && reserveDroneCount > 0) {
            if (launchTracker.getElapsed() >= launchTracker.getIntervalDuration()) {
                launchTracker.setElapsed(0);
                baseDroneSystem.getDeployedDrones().add(spawnDroneFromShip(droneVariant));

                //subtract from reserve drone count on launch
                reserveDroneCount -= 1;
                if (reserveDroneCount < maxDeployedDrones - 1 && system.getCooldownRemaining() <= 0f) system.setCooldownRemaining(system.getCooldown());
            }

            launchTracker.advance(amount / launchDelayStatMod);
        }

        if (ship.equals(engine.getPlayerShip()) && isActivationKeyDown && !isActivationKeyDownPreviousFrame) {
            baseDroneSystem.nextDroneOrder();
        }

        if (!baseDroneSystem.isRecallMode()) {
            baseDroneSystem.applyActiveStatBehaviour();
        } else {
            baseDroneSystem.unapplyActiveStatBehaviour();
        }

        updateDeployedDrones(deployedDrones);

        baseDroneSystem.setDeployedDrones(deployedDrones);

        system.setAmmo(deployedDrones.size());

        engine.getCustomData().put("PSE_DroneList_" + ship.hashCode(), deployedDrones);

        switch (droneSystemType) {
            case CORONA:
                if (coronaSystem.getDroneOrders().equals(PSE_DroneCorona.CoronaDroneOrders.ATTACK)) {
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

                break;
            case SHROUD:
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

                break;
            case CITADEL:
                switch (citadelSystem.getDroneOrders()) {
                    case ANTI_FIGHTER:
                    case RECALL:
                        citadelSystem.resetOrbitAngleBase();

                        break;
                    case SHIELD:
                        citadelSystem.advanceOrbitAngleBase(amount);

                        break;

                }

                break;
            default:
                break;
        }

        isActivePreviousFrame = system.isActive();
        isActivationKeyDownPreviousFrame = isActivationKeyDown;
    }

    private void trackSystemAmmo() {
        //ammo tracking
        // offset the silly automatic subtraction when system activated
        if (system.isActive() && !isActivePreviousFrame) {
            system.setAmmo(system.getAmmo() + 1);
        }

        if (reserveDroneCount >= (maxDeployedDrones - 1)) {
            system.setCooldownRemaining(cooldownLastFrame);
        }
        if (system.getAmmo() >= system.getMaxAmmo()) {
            system.setCooldownRemaining(cooldownLastFrame - engine.getElapsedInLastFrame());
        }

        if (system.getCooldownRemaining() <= 0f && system.getCooldownRemaining() < cooldownLastFrame && reserveDroneCount < (maxDeployedDrones - 1)) {
            reserveDroneCount++;

            system.setCooldownRemaining(system.getCooldown());
        }

        cooldownLastFrame = system.getCooldownRemaining();
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
                spawnedDrone.setShipAI(new PSE_DroneCoronaDroneAI(spawnedDrone, baseDroneSystem));
                break;
            case BASTION:
                spawnedDrone.setShipAI(new PSE_DroneBastionDroneAI(spawnedDrone, baseDroneSystem));
                break;
            case MVA:
                spawnedDrone.setShipAI(new PSE_DroneModularVectorAssemblyDroneAI(spawnedDrone, baseDroneSystem));
                break;
            case SHROUD:
                spawnedDrone.setShipAI(new PSE_DroneShroudDroneAI(spawnedDrone, baseDroneSystem));
                break;
            case CITADEL:
                spawnedDrone.setShipAI(new PSE_DroneCitadelDroneAI(spawnedDrone, baseDroneSystem));
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

    public int getReserveDroneCount() {
        return reserveDroneCount;
    }
}