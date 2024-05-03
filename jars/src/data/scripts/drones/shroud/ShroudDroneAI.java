package data.scripts.drones.shroud;

import cmu.drones.ai.BasicDroneAI;
import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class ShroudDroneAI extends BasicDroneAI {

    private static final float orbitRadius = -50f;
    private static final float orbitSpeed = 10f;

    private final ShroudShipsystem shipsystem;

    private float orbit = 0f;

    private Vector2f destLocation;
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public ShroudDroneAI(ShipAPI drone, ShipAPI mothership, ShroudShipsystem shipsystem) {
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

        float d = 360f / shipsystem.getForgeTracker().getDeployed().size();
        float angle = d * index;

        switch (shipsystem.droneOrders) {
            case CIRCLE:
                orbit += amount * orbitSpeed;
                if (orbit >= 360f) orbit -= 360f;

                destFacing = MathUtils.clampAngle(orbit + angle + mothership.getFacing());
                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        orbitRadius + mothership.getShieldRadiusEvenIfNoShield(),
                        orbit + angle + mothership.getFacing()
                );

                landing = null;

                drone.getShield().toggleOn();

                break;
            case BROADSIDE:
                orbit = 0f;

                float f;
                int o = (int) (angle);
                if (o == 0) {
                    f = 0f;
                } else if (o == 180) {
                    f = 180f;
                } else if (o < 180) {
                    f = 90f;
                } else {
                    f = 270f;
                }
                destFacing = MathUtils.clampAngle(f + mothership.getFacing());

                destLocation = MathUtils.getPointOnCircumference(
                        mothership.getLocation(),
                        orbitRadius + mothership.getShieldRadiusEvenIfNoShield(),
                        orbit + angle + mothership.getFacing()
                );

                landing = null;

                if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                    drone.getShield().toggleOn();
                }

                break;
            case RECALL:
                if (landing == null) {
                    landing = shipsystem.getForgeTracker().getLaunchSlot();
                }

                destLocation = new Vector2f(landing.computePosition(mothership));
                destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

                if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                    drone.getShield().toggleOff();
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
        return "PSE_Shroud";
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
        return shipsystem != null && shipsystem.getDroneOrders() == ShroudShipsystem.ShroudOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.3f;
    }
}
