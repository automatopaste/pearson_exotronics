package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneShroud;
import data.scripts.util.PSE_DroneUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class PSE_DroneShroudDroneAI implements ShipAIPlugin {

    private static final Color JITTER_COLOUR = new Color(0, 255, 120, 255);

    private final PSEDrone drone;
    private final ShipAPI ship;
    private CombatEngineAPI engine;

    //USED FOR MOVEMENT AND POSITIONING AI
    private IntervalUtil velocityRotationIntervalTracker = new IntervalUtil(0.01f, 0.05f);
    private IntervalUtil delayBeforeLandingTracker = new IntervalUtil(2f, 2f);
    private WeaponSlotAPI landingSlot;

    private String UNIQUE_SYSTEM_ID;

    private static Random random;

    public PSE_DroneShroudDroneAI(PSEDrone passedDrone) {
        random = new Random();

        this.engine = Global.getCombatEngine();

        this.drone = passedDrone;

        this.ship = drone.getDroneSource();

        this.UNIQUE_SYSTEM_ID = "PSE_DroneShroud_" + ship.hashCode();

        drone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
    }

    @Override
    public void advance(float amount) {
        this.engine = Global.getCombatEngine();
        if (engine.isPaused()) {
            return;
        }

        ////////////////////
        ///INITIALISATION///
        ///////////////////


        //get ship system object
        PSE_DroneShroud shipDroneShroudSystem = (PSE_DroneShroud) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (shipDroneShroudSystem == null) {
            return;
        }

        //assign specific values
        int droneIndex = shipDroneShroudSystem.getIndex(drone);

        List<PSEDrone> deployedDrones = shipDroneShroudSystem.deployedDrones;

        float[] orbitAngleArray = new float[deployedDrones.size()];
        float angleDivisor = 360f / deployedDrones.size();
        for (int i = 0; i < deployedDrones.size(); i++) {
            orbitAngleArray[i] = angleDivisor * i;
        }

        float[] broadsideFacingAngleArray = new float[deployedDrones.size()];
        for (int i = 0; i < deployedDrones.size(); i++) {
            float angle = orbitAngleArray[i];
            if (angle >= 30f && angle <= 150f) {
                broadsideFacingAngleArray[i] = 90f;
            } else if (angle >= 210f && angle <= 330f) {
                broadsideFacingAngleArray[i] = 270f;
            } else {
                broadsideFacingAngleArray[i] = angle;
            }
        }

        float orbitAngle = orbitAngleArray[droneIndex]  + shipDroneShroudSystem.getOrbitAngleBase();

        float orbitRadius = -30f + ship.getShieldRadiusEvenIfNoShield();

        //UNUSED AS YET CONSTANT FOR BEHAVIOUR WHEN HOST SIGNAL LOST
        float sanity = 1f;

        //GET DRONE ORDERS STATE
        PSE_DroneShroud.ShroudDroneOrders shroudDroneOrders = shipDroneShroudSystem.getDroneOrders();


        /////////////////////////
        ///TARGETING BEHAVIOUR///
        /////////////////////////




        ////////////////////////
        ///MOVEMENT BEHAVIOUR///
        ////////////////////////


        //PERFORM LOGIC BASED ON MOTHERSHIP SHIPSYSTEM STATE - SELECT TARGET LOCATION
        float angle;
        float radius;
        Vector2f movementTargetLocation;

        ship.getMutableStats().getMaxSpeed().unmodify(this.toString());
        ship.getMutableStats().getTurnAcceleration().unmodify(this.toString());
        ship.getMutableStats().getMaxTurnRate().unmodify(this.toString());
        ship.getMutableStats().getDeceleration().unmodify(this.toString());
        ship.getMutableStats().getAcceleration().unmodify(this.toString());

        float droneFacing = drone.getFacing();
        float targetFacing = 0f;
        switch (shroudDroneOrders) {
            case CIRCLE:
                angle = orbitAngle + ship.getFacing();

                radius = orbitRadius;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                landingSlot = null;

                if (drone.getShield().isOff()) {
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }

                targetFacing = orbitAngle + ship.getFacing();

                break;
            case BROADSIDE_MOVEMENT:
                angle = orbitAngle + ship.getFacing();

                radius = orbitRadius - 10f;

                delayBeforeLandingTracker.setElapsed(0f);

                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);
                landingSlot = null;

                if (drone.getShield().isOff()) {
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }

                drone.setJitter(this, JITTER_COLOUR, 0.5f, 2, 1f);
                drone.getEngineController().extendFlame(this, 2f, 1f, 3f);

                ship.getMutableStats().getMaxSpeed().modifyFlat(this.toString(), 1f);
                ship.getMutableStats().getTurnAcceleration().modifyFlat(this.toString(), 1f);
                ship.getMutableStats().getMaxTurnRate().modifyFlat(this.toString(), 2f);
                ship.getMutableStats().getDeceleration().modifyFlat(this.toString(), 3f);
                ship.getMutableStats().getAcceleration().modifyFlat(this.toString(), 2f);

                ship.getEngineController().extendFlame(this, 1.2f, 1f, 1.2f);

                targetFacing = broadsideFacingAngleArray[droneIndex] + ship.getFacing();

                drone.getMutableStats().getShieldDamageTakenMult().modifyFlat(this.toString(), 0.5f);

                //LIGHTNING SPAWNING
                float EMPChance = drone.getHardFluxLevel() - 0.5f;
                EMPChance *= 0.15f;
                EMPChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                if (Math.random() < EMPChance) {
                    float arcEMPDamage = 200f * (1f + EMPChance);
                    engine.spawnEmpArc(
                            drone.getShipAPI(),
                            drone.getLocation(),
                            drone.getShipAPI(),
                            ship,
                            DamageType.ENERGY,
                            0f,
                            arcEMPDamage,
                            drone.getShieldRadiusEvenIfNoShield() * 2f,
                            "tachyon_lance_emp_impact",
                            25f,
                            new Color(0, 255, 220, 200),
                            new Color(200, 255, 235, 255)
                    );
                }

                break;
            case RECALL:
                PSE_DroneUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker);

                if (landingSlot == null) {
                    landingSlot = shipDroneShroudSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);

                if (drone.getShield().isOn()) {
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }

                targetFacing = orbitAngle + ship.getFacing();

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        //ROTATION moved here for streamlined switching
        PSE_DroneUtils.rotateToFacing(drone, targetFacing, droneFacing, 0.05f);

        PSE_DroneUtils.move(drone, droneFacing, movementTargetLocation, sanity, velocityRotationIntervalTracker);
    }

    //OVERRIDES

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        ShipwideAIFlags flags = new ShipwideAIFlags();
        flags.setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
        return flags;
    }

    //not relevant
    @Override
    public void cancelCurrentManeuver() {
    }

    //not relevant
    @Override
    public ShipAIConfig getConfig() {
        return null;
    }

    //not relevant
    @Override
    public void setDoNotFireDelay(float amount) {
    }

    //called when AI activated on player ship
    @Override
    public void forceCircumstanceEvaluation() {
    }
}


