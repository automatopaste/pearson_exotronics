package data.scripts.drones.trophy;

import cmu.drones.ai.BasicDroneAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.shroud.ShroudShipsystem;
import data.scripts.drones.trophy.TrophyShipsystem;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class TrophyDroneAI extends BasicDroneAI {

    private final float[] mode1Offsets = new float[] { 120f, -120f };

    private final float[] mode2Offsets = new float[] { 60f, -60f };
    private final float[] mode2Angles = new float[] { 20f, -20f };

    private static final Color EMP_COLOUR_1 = new Color(180, 255, 215, 255);
    private static final Color EMP_COLOUR_2 = new Color(0, 232, 122, 255);
    private final IntervalUtil empInterval = new IntervalUtil(1.0f, 1.5f);

    private final TrophyShipsystem shipsystem;

    private Vector2f destLocation = new Vector2f();
    private float destFacing;
    private WeaponSlotAPI landing = null;

    public TrophyDroneAI(ShipAPI drone, ShipAPI mothership, TrophyShipsystem shipsystem) {
        super(drone, mothership);

        this.shipsystem = shipsystem;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        CombatEngineAPI engine = Global.getCombatEngine();

        int index = shipsystem.getIndexForDrone(drone);
        if (index == -1) {
            landing = shipsystem.getForgeTracker().getLaunchSlot();

            destLocation = new Vector2f(landing.computePosition(mothership));
            destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

            return;
        }

        if (mothership == null) return;

        switch (shipsystem.getDroneOrders()) {
            case LANTERN:
            {
                Vector2f cursor = mothership.getMouseTarget();
                Vector2f d = Vector2f.sub(cursor, mothership.getLocation(), new Vector2f());
                float distance = mothership.getShieldRadiusEvenIfNoShield() - 100f;
                if (!VectorUtils.isZeroVector(d)) d.normalise();
                d.scale(distance);
                float angle = VectorUtils.getFacing(d);

                Vector2f offset = new Vector2f(mode1Offsets[index], 0f);
                VectorUtils.rotate(offset, angle + 90f);
                Vector2f.add(offset, d, d);

                destFacing = angle;

                Vector2f.add(d, mothership.getLocation(), destLocation);

                landing = null;

                if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                    drone.getShield().toggleOn();
                }

                empInterval.advance(engine.getElapsedInLastFrame());
                if (empInterval.intervalElapsed()) {
                    for (ShipAPI ship : shipsystem.getForgeTracker().getDeployed()) {
                        if (ship.equals(drone)) continue;

                        Global.getCombatEngine().spawnEmpArcVisual(drone.getLocation(), drone, ship.getLocation(), ship, 10f, EMP_COLOUR_1, EMP_COLOUR_2);
                    }
                }

                break;
            }
            case SHIELD:
            {
                Vector2f target;
                if (mothership.getShipTarget() != null) {
                    target = new Vector2f(mothership.getShipTarget().getLocation());
                } else {
                    target = new Vector2f(mothership.getMouseTarget());
                }

                Vector2f d = Vector2f.sub(target, mothership.getLocation(), new Vector2f());
                float distance = mothership.getShieldRadiusEvenIfNoShield() + 10f;
                if (!VectorUtils.isZeroVector(d)) d.normalise();
                d.scale(distance);
                float angle = VectorUtils.getFacing(d);

                Vector2f offset = new Vector2f(mode2Offsets[index], 0f);
                VectorUtils.rotate(offset, angle + 90f);
                Vector2f.add(offset, d, d);

                destFacing = angle + mode2Angles[index];

                Vector2f.add(d, mothership.getLocation(), destLocation);

                landing = null;

                if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                    drone.getShield().toggleOn();
                }

                empInterval.setElapsed(0f);
                break;
            }
            case RECALL:
            default:
                if (landing == null) {
                    landing = shipsystem.getForgeTracker().getLaunchSlot();
                }

                if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                    drone.getShield().toggleOff();
                }

                destLocation = new Vector2f(landing.computePosition(mothership));
                destFacing = MathUtils.clampAngle(landing.getAngle() + mothership.getFacing());

                empInterval.setElapsed(0f);
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
        return "PSE_Trophy";
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
        return shipsystem != null && shipsystem.getDroneOrders() == TrophyShipsystem.TrophyOrders.RECALL;
    }

    @Override
    protected float getDelayBeforeLanding() {
        return 0.5f;
    }

}
