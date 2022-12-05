package data.scripts.drones.bastion;

import cmu.drones.ai.BasicDroneAI;
import cmu.drones.ai.DroneAIUtils;
import cmu.drones.systems.SystemData;
import cmu.misc.MiscUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

public class BastionDroneAI extends BasicDroneAI {

    private static final float[] mode1Angles = new float[]{25f, 75f, -25f, -75f};
    private static final float[] mode2Angles = new float[]{25f, 135f, -25f, -135f};
    private static final float[] mode1Facing = new float[]{0f, 0f, 0f, 0f};
    private static final float[] mode2Facing = new float[]{25f, 135f, -25f, -135f};
    private static final float[] mode1Radii = new float[]{-35f, -35f, -35f, -35f};
    private static final float[] mode2Radii = new float[]{-35f, -35f, -35f, -35f};

    private static final float TARGETING_ARC_DEV = 120f;

    private static final String PD_WEAPON_ID = "pdlaser";
    private float PDWeaponRange;

    private final BastionShipsystem shipsystem;

    private Vector2f destLocation;
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public BastionDroneAI(ShipAPI drone, ShipAPI mothership, BastionShipsystem shipsystem) {
        super(drone, mothership);

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();
        for (WeaponAPI weapon : droneWeapons) {
            if (weapon.getId().contentEquals(PD_WEAPON_ID)) {
                PDWeaponRange = weapon.getRange();
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

        float relativeAngleFromShip = VectorUtils.getFacing(Vector2f.sub(drone.getLocation(), mothership.getLocation(), new Vector2f()));

        CombatEntityAPI target;
        switch (shipsystem.getDroneOrders()) {
            case FRONT:
                target = DroneAIUtils.getEnemyTarget(mothership, drone, PDWeaponRange, false, false, false, TARGETING_ARC_DEV);

                //arc logic
                if (target != null && MiscUtils.isEntityInArc(target, drone.getLocation(), relativeAngleFromShip, TARGETING_ARC_DEV)) {
                    Vector2f disp = Vector2f.sub(target.getLocation(), drone.getLocation(), new Vector2f());
                    destFacing = VectorUtils.getFacing(disp);
                } else {
                    destFacing = mode1Facing[index] + mothership.getFacing();
                }

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        mode1Radii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode1Angles[index] + mothership.getFacing()
                );

                landing = null;

                break;
            case CARDINAL:
                if (mothership.getShipTarget() != null) target = mothership.getShipTarget();
                else target = DroneAIUtils.getEnemyTarget(mothership, drone, PDWeaponRange, false, false, false, TARGETING_ARC_DEV);

                //arc logic
                if (target != null && MiscUtils.isEntityInArc(target, drone.getLocation(), relativeAngleFromShip, TARGETING_ARC_DEV)) {
                    Vector2f disp = Vector2f.sub(target.getLocation(), drone.getLocation(), new Vector2f());
                    destFacing = VectorUtils.getFacing(disp);
                } else {
                    destFacing = mode2Facing[index] + mothership.getFacing();
                }

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        mode2Radii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode2Angles[index] + mothership.getFacing()
                );

                landing = null;

                break;
            case RECALL:
            default:
                if (landing == null) {
                    landing = shipsystem.getForgeTracker().getLaunchSlot();
                }

                destLocation = new Vector2f(landing.computePosition(mothership));
                destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

                break;
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
        return "PSE_Bastion";
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
        return shipsystem != null && shipsystem.getDroneOrders() == BastionShipsystem.BastionOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.5f;
    }
}
