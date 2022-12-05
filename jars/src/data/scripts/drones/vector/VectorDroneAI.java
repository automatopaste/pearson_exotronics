package data.scripts.drones.vector;

import cmu.drones.ai.BasicDroneAI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class VectorDroneAI extends BasicDroneAI {

    private static final float[] mode1Angles = new float[]{0f, 0f, 0f};
    private static final float[] mode1Facing = new float[]{0f, 0f, 0f};
    private static final float[] mode1Radii = new float[]{-50f, -40f, -30f};

    private static final float[] mode2Angles = new float[]{180f, -50f, 50f};
    private static final float[] mode2Facing = new float[]{0f, -30f, 30f};
    private static final float[] mode2Radii = new float[]{-80f, -85f, -85f};

    private final VectorShipsystem shipsystem;

    private Vector2f destLocation;
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public VectorDroneAI(ShipAPI drone, ShipAPI mothership, VectorShipsystem shipsystem) {
        super(drone, mothership);

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

        switch (shipsystem.getDroneOrders()) {
            case RESONATOR:
                destFacing = mode1Facing[index] + mothership.getFacing();

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        mode1Radii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode1Angles[index] + mothership.getFacing()
                );

                landing = null;

                break;
            case VECTOR_THRUST:
                destFacing = mode2Facing[index] + mothership.getFacing();

                float[] mode2Turn = new float[]{90f, -90f, -90f};

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        mode2Radii[index] + mothership.getShieldRadiusEvenIfNoShield(),
                        mode2Angles[index] + mothership.getFacing()
                );

                if (Vector2f.sub(destLocation, drone.getLocation(), new Vector2f()).lengthSquared() < 25f) {
                    mothership.getMutableStats().getAcceleration().modifyFlat(this.toString(), 10f);
                    mothership.getMutableStats().getTurnAcceleration().modifyFlat(this.toString(), 15f);
                    mothership.getMutableStats().getDeceleration().modifyFlat(this.toString(), 10f);
                    mothership.getMutableStats().getMaxTurnRate().modifyFlat(this.toString(), 7f);
                    mothership.getMutableStats().getMaxSpeed().modifyFlat(this.toString(), 10f);

                    drone.getEngineController().extendFlame(this, 1.5f, 0.2f, 3f);

                    ShipEngineControllerAPI controller = mothership.getEngineController();

                    if (controller.isTurningLeft()) {
                        destFacing -= mode2Turn[index];
                    } else if (controller.isTurningRight()) {
                        destFacing += mode2Turn[index];
                    }
                } else {
                    mothership.getMutableStats().getAcceleration().unmodify(this.toString());
                    mothership.getMutableStats().getTurnAcceleration().unmodify(this.toString());
                    mothership.getMutableStats().getDeceleration().unmodify(this.toString());
                    mothership.getMutableStats().getMaxSpeed().unmodify(this.toString());
                }

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
        return "PSE_Vector";
    }

    @Override
    protected float getHostSearchRange() {
        return 2000f;
    }

    @Override
    protected boolean isLanding() {
        return landing != null;
    }

    @Override
    protected boolean isRecalling() {
        return shipsystem != null && shipsystem.getDroneOrders() == VectorShipsystem.VectorOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.5f;
    }
}
