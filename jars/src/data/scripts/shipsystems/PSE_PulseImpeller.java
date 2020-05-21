package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

/*
this was a bad idea
 */
public class PSE_PulseImpeller extends BaseShipSystemScript {
    public static final float BOOST_SPEED_CAP_MULT = 2f;
    public static final float FLAME_LENGTH_FRACTION = 2f;
    public static final float FLAME_GLOW_FRACTION = 2f;
    public static final float JITTER_RANGE = 2f;
    public static final Color CONTRAIL_COLOUR = new Color (0x63FFFC32, true);
    public static final Color JITTER_COLOUR = new Color (0x5000FFB9, true);

    boolean started = false;
    boolean ended = false;

    float boostFraction = 1f;
    float boostLimit = 0f;

    IntervalUtil afterimageInterval = new IntervalUtil(0.1f, 0.15f);
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float modlevel = effectLevel * 100f;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        if (state.equals(State.IN)) {
            List<ShipEngineControllerAPI.ShipEngineAPI> engineList = ship.getEngineController().getShipEngines();
            if (!started) {
                //todo - play sound

                boostFraction = 1 - (ship.getNumFlameouts() / (float) engineList.size());

                boostLimit = ship.getMaxSpeedWithoutBoost() * BOOST_SPEED_CAP_MULT;

                started = true;
            }

            for (ShipEngineAPI engine : engineList) {
                ship.getEngineController().extendFlame(engine, FLAME_LENGTH_FRACTION, 1f, FLAME_GLOW_FRACTION);
            }

            afterimageInterval.advance(0.1f);
            if (afterimageInterval.intervalElapsed()) {
                ship.addAfterimage(JITTER_COLOUR, MathUtils.getRandomNumberInRange(-1f * JITTER_RANGE, JITTER_RANGE), MathUtils.getRandomNumberInRange(-1f * JITTER_RANGE, JITTER_RANGE), ship.getVelocity().getX() * -0.5f, ship.getVelocity().getY() * -0.5f, 5f, 0.1f, 0f, 0.8f, true, false, false);
            }
        } else if (state.equals(State.OUT)) {
            if (!ended) {
                if (VectorUtils.isZeroVector(ship.getVelocity())) {
                    ship.getVelocity().y += 0.1f;
                    VectorUtils.rotate(ship.getVelocity(), MathUtils.getShortestRotation(VectorUtils.getFacing(ship.getVelocity()), ship.getFacing()));
                }

                Vector2f facing = new Vector2f(0f, 1f);
                VectorUtils.rotate(facing, MathUtils.getShortestRotation(VectorUtils.getFacing(facing), ship.getFacing()));
                facing.normalise();
                facing.scale(ship.getMass() * BOOST_SPEED_CAP_MULT);

                Vector2f composite = new Vector2f(0f, 1f);
                Vector2f.add(ship.getVelocity(), facing, composite);

                CombatUtils.applyForce(ship, composite, ship.getMass() * BOOST_SPEED_CAP_MULT);

                ended = true;
            }

            afterimageInterval.advance(0.01f);
            if (afterimageInterval.intervalElapsed()) {
                Color modColour = new Color(
                        MathUtils.clamp(CONTRAIL_COLOUR.getRed() * modlevel, 1f, 255f) / 255f,
                        MathUtils.clamp(CONTRAIL_COLOUR.getGreen() * modlevel, 1f, 255f) / 255f,
                        MathUtils.clamp(CONTRAIL_COLOUR.getBlue() * modlevel, 1f, 255f) / 255f
                    );

                ship.addAfterimage(modColour, MathUtils.getRandomNumberInRange(-1f * JITTER_RANGE, JITTER_RANGE), MathUtils.getRandomNumberInRange(-1f * JITTER_RANGE, JITTER_RANGE), ship.getVelocity().getX() * -0.5f, ship.getVelocity().getY() * -0.5f, 5f, 0.1f, 0f, 0.8f, true, false, false);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        started = false;
        ended = false;
        boostFraction = 1f;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship != null) {
            return !ship.getEngineController().isFlamedOut();
        }
        return false;
    }
}
