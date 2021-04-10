package data.scripts.ai;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_BaseDroneSystem;
import data.scripts.shipsystems.PSE_DroneShroud;
import data.scripts.util.PSE_DroneAIUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class PSE_DroneShroudDroneAI extends PSE_BaseDroneAI {
    private static final Color JITTER_COLOUR = new Color(0, 255, 120, 255);

    //USED FOR MOVEMENT AND POSITIONING AI
    private float[] broadsideFacingAngleArray;
    private float orbitAngle;
    private float orbitRadius;
    private PSE_DroneShroud.ShroudDroneOrders droneOrders;
    private int droneIndex;

    public PSE_DroneShroudDroneAI(PSEDrone passedDrone, PSE_BaseDroneSystem baseDroneSystem) {
        super(passedDrone, baseDroneSystem);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        //get ship system object
        PSE_DroneShroud shipDroneShroudSystem = (PSE_DroneShroud) engine.getCustomData().get(getUniqueSystemID());
        if (shipDroneShroudSystem == null) {
            return;
        }
        baseDroneSystem = shipDroneShroudSystem;

        //assign specific values
        droneIndex = shipDroneShroudSystem.getIndex(drone);

        List<PSEDrone> deployedDrones = baseDroneSystem.deployedDrones;

        float[] orbitAngleArray = new float[deployedDrones.size()];
        float angleDivisor = 360f / deployedDrones.size();
        for (int i = 0; i < deployedDrones.size(); i++) {
            orbitAngleArray[i] = angleDivisor * i;
        }

        broadsideFacingAngleArray = new float[deployedDrones.size()];
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

        orbitAngle = orbitAngleArray[droneIndex] + shipDroneShroudSystem.getOrbitAngleBase(droneIndex);

        orbitRadius = -30f + ship.getShieldRadiusEvenIfNoShield();

        //GET DRONE ORDERS STATE
        droneOrders = shipDroneShroudSystem.getDroneOrders();

        ship.getMutableStats().getMaxSpeed().unmodify(this.toString());
        ship.getMutableStats().getTurnAcceleration().unmodify(this.toString());
        ship.getMutableStats().getMaxTurnRate().unmodify(this.toString());
        ship.getMutableStats().getDeceleration().unmodify(this.toString());
        ship.getMutableStats().getAcceleration().unmodify(this.toString());


        switch (droneOrders) {
            case CIRCLE:
                delayBeforeLandingTracker.setElapsed(0f);

                landingSlot = null;

                if (drone.getShield().isOff()) {
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }

                break;
            case BROADSIDE_MOVEMENT:
                delayBeforeLandingTracker.setElapsed(0f);

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


                drone.getMutableStats().getShieldDamageTakenMult().modifyFlat(this.toString(), 0.5f);

                //LIGHTNING SPAWNING
                float EMPChance = drone.getHardFluxLevel() - 0.5f;
                EMPChance *= 0.125f;
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
                PSE_DroneAIUtils.attemptToLand(ship, drone, amount, delayBeforeLandingTracker, engine);

                if (landingSlot == null) {
                    landingSlot = shipDroneShroudSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                if (drone.getShield().isOn()) {
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }

                break;
        }

        doRotationTargeting();

        Vector2f movementTargetLocation = getMovementTargetLocation(amount);
        if (movementTargetLocation != null) {
            PSE_DroneAIUtils.move(drone, drone.getFacing(), movementTargetLocation, velocityRotationIntervalTracker);
        }
    }

    @Override
    protected Vector2f getMovementTargetLocation(float amount) {
        float angle;
        float radius;
        Vector2f movementTargetLocation;

        switch (droneOrders) {
            case CIRCLE:
                angle = orbitAngle + ship.getFacing();
                radius = orbitRadius;
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                break;
            case BROADSIDE_MOVEMENT:
                angle = orbitAngle + ship.getFacing();
                radius = orbitRadius - 10f;
                movementTargetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

                break;
            case RECALL:
                if (landingSlot == null) {
                    landingSlot = baseDroneSystem.getPlugin().getLandingBayWeaponSlotAPI();
                }

                movementTargetLocation = landingSlot.computePosition(ship);

                break;
            default:
                movementTargetLocation = ship.getLocation();
        }

        return movementTargetLocation;
    }

    @Override
    protected void doRotationTargeting() {
        float targetFacing;

        switch (droneOrders) {
            case CIRCLE:
            case RECALL:
                targetFacing = orbitAngle + ship.getFacing();

                break;
            case BROADSIDE_MOVEMENT:
                targetFacing = broadsideFacingAngleArray[droneIndex] + ship.getFacing();

                break;
            default:
                targetFacing = 0f;

                break;
        }

        PSE_DroneAIUtils.rotateToFacing(drone, targetFacing, engine);
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