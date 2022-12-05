package data.scripts.drones.corona;

import cmu.drones.ai.BasicDroneAI;
import cmu.drones.ai.DroneAIUtils;
import cmu.drones.systems.SystemData;
import cmu.misc.MiscUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.combat.ai.system.drones.DroneAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

public class CoronaDroneAI extends BasicDroneAI {

    private static final float[] mode1Angles = new float[]{0f, 50f, -50f};
    private static final float[] mode2Angles = new float[]{0f, 5f, -5f};
    private static final float[] orbitRadii = new float[]{-35f, -35f, -35f};

    private static final String PD_WEAPON_ID = "pdlaser";
    private static final String FOCUS_WEAPON_ID = "hil";

    private static final float TARGETING_ARC_DEV = 120f;

    private float PDWeaponRange;
    private float focusWeaponRange;

    private final CoronaShipsystem shipsystem;

    private Vector2f destLocation;
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public CoronaDroneAI(ShipAPI drone, ShipAPI mothership, CoronaShipsystem shipsystem) {
        super(drone, mothership);

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();
        for (WeaponAPI weapon : droneWeapons) {
            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                PDWeaponRange = weapon.getRange();
            } else if (weapon.getId().contentEquals(FOCUS_WEAPON_ID)) {
                focusWeaponRange = weapon.getRange();
            }
        }

        this.shipsystem = shipsystem;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        int index = shipsystem.getIndexForDrone(drone);
        if (index == -1) {
            landing = shipsystem.getForgeTracker().getLaunchSlot();

            destLocation = new Vector2f(landing.computePosition(mothership));
            destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

            return;
        }

        if (mothership == null) return;

        CombatEntityAPI target;
        switch (shipsystem.getDroneOrders()) {
            case DEFEND:
                target = DroneAIUtils.getEnemyTarget(mothership, drone, PDWeaponRange, false, false, false, TARGETING_ARC_DEV);

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        orbitRadii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode1Angles[index] + mothership.getFacing()
                );

                landing = null;

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(PD_WEAPON_ID)) {
                        group.toggleOn();
                    } else if (group.getActiveWeapon().getSpec().getWeaponId().equals(FOCUS_WEAPON_ID)) {
                        group.toggleOff();
                    }
                }

                break;
            case ATTACK:
//                for (WeaponAPI weapon : drone.getAllWeapons()) if (weapon.getId().contentEquals(PD_WEAPON_ID)) weapon.disable(true);

                if (mothership.getShipTarget() != null) target = mothership.getShipTarget();
                else target = DroneAIUtils.getEnemyTarget(mothership, drone, focusWeaponRange, true, true, false, TARGETING_ARC_DEV);

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        orbitRadii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode2Angles[index] + mothership.getFacing()
                );

                landing = null;

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(PD_WEAPON_ID)) {
                        group.toggleOff();
                    } else if (group.getActiveWeapon().getSpec().getWeaponId().equals(FOCUS_WEAPON_ID)) {
                        if (index == 0) {
                            group.toggleOn();
                        } else {
                            group.toggleOff();
                        }
                    }
                }

                if (target != null && DroneAIUtils.areFriendliesBlockingArc(drone, target, focusWeaponRange)) {
                    drone.setHoldFireOneFrame(true);
                }

                break;
            case RECALL:
            default:
//                for (WeaponAPI weapon : drone.getAllWeapons()) if (weapon.getId().contentEquals(PD_WEAPON_ID)) weapon.repair();
                target = null;

                if (landing == null) {
                    landing = shipsystem.getForgeTracker().getLaunchSlot();
                }

                destLocation = new Vector2f(landing.computePosition(mothership));
                destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(PD_WEAPON_ID)) {
                        group.toggleOff();
                    } else if (group.getActiveWeapon().getSpec().getWeaponId().equals(FOCUS_WEAPON_ID)) {
                        group.toggleOff();
                    }
                }

                break;
        }

        float relativeAngleFromShip = VectorUtils.getFacing(Vector2f.sub(drone.getLocation(), mothership.getLocation(), new Vector2f()));

        //arc logic
        if (target != null && MiscUtils.isEntityInArc(target, drone.getLocation(), relativeAngleFromShip, TARGETING_ARC_DEV)) {
            Vector2f disp = Vector2f.sub(target.getLocation(), drone.getLocation(), new Vector2f());
            destFacing = VectorUtils.getFacing(disp);

            //check for friendlies
            boolean areFriendliesInFiringArc = DroneAIUtils.areFriendliesBlockingArc(drone, target, focusWeaponRange);
            drone.setHoldFireOneFrame(areFriendliesInFiringArc);
        } else {
            destFacing = mothership.getFacing();
        }
    }

    @Override
    protected Vector2f getDestLocation(float amount) {
        return destLocation;
    }

    @Override
    protected float getDestFacing(float amount) {
        return destFacing;
    }

    @Override
    protected String getSystemID() {
        return "PSE_Corona";
    }

    @Override
    protected float getHostSearchRange() {
        return 4000f;
    }

    @Override
    protected boolean isLanding() {
        return landing != null;
    }

    @Override
    protected boolean isRecalling() {
        return shipsystem != null && shipsystem.getDroneOrders() == CoronaShipsystem.CoronaOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.3f;
    }
}
