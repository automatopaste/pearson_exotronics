package data.scripts.drones.rift;

import cmu.drones.ai.BasicDroneAI;
import cmu.drones.ai.DroneAIUtils;
import cmu.misc.MiscUtils;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RiftDroneAI extends BasicDroneAI {

    private static final float[] mode1Angles = new float[]{40f, -40f, 120f, -120f};
    private static final float[] mode1Facing = new float[]{25f, -25f, 60f, -60f};
    private static final float[] mode1Radii = new float[]{20f, 20f, 60f, 60f};

    private static final float[] mode2Radii = new float[]{20f, 20f, 20f, 20f};

    private static final float TARGETING_ARC_DEV = 120f;

    private static final String WEAPON_ID = "ioncannon";
    private float weaponRange;

    private final RiftShipsystem shipsystem;

    private Vector2f destLocation;
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public RiftDroneAI(ShipAPI drone, ShipAPI mothership, RiftShipsystem shipsystem) {
        super(drone, mothership);

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();
        for (WeaponAPI weapon : droneWeapons) {
            if (weapon.getId().contentEquals(WEAPON_ID)) {
                weaponRange = weapon.getRange();
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
            case DEFENCE:
                target = DroneAIUtils.getEnemyTarget(mothership, drone, weaponRange, true, false, false, TARGETING_ARC_DEV);

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

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(WEAPON_ID)) {
                        group.toggleOn();
                    }
                }

                if (target != null) drone.getMouseTarget().set(target.getLocation());

                break;
            case ECCM_ARRAY:
                destFacing = shipsystem.getAngle(index) + mothership.getFacing();

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        mode2Radii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        destFacing
                );

                landing = null;

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(WEAPON_ID)) {
                        group.toggleOff();
                    }
                }

                Vector2f d = new Vector2f(1f, 0f);
                VectorUtils.rotate(d, drone.getFacing());

                d.scale(400f);

                Vector2f.add(d, drone.getLocation(), d);

                drone.getMouseTarget().set(d);

                break;
            case RECALL:
            default:
                if (landing == null) {
                    landing = shipsystem.getForgeTracker().getLaunchSlot();
                }

                destLocation = new Vector2f(landing.computePosition(mothership));
                destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

                for (WeaponGroupAPI group : drone.getWeaponGroupsCopy()) {
                    if (group.getActiveWeapon().getSpec().getWeaponId().equals(WEAPON_ID)) {
                        group.toggleOff();
                    }
                }

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
        return "PSE_Rift";
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
        return shipsystem != null && shipsystem.getDroneOrders() == RiftShipsystem.RiftOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.5f;
    }
}
