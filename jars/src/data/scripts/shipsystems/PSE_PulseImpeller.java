package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;

import java.awt.*;
import java.util.List;


public class PSE_PulseImpeller extends BaseShipSystemScript {
    public static final float BOOST_SPEED = 300f;
    public static final float FLAME_LENGTH_FRACTION = 2f;
    public static final float FLAME_GLOW_FRACTION = 2f;

    public static final Color CONTRAIL_COLOUR = new Color (0xE2FF8B);
    public static final Color JITTER_COLOUR = new Color (0x00FFB9);

    boolean started = false;
    boolean ended = false;
    boolean boostForward = true;

    float boostFraction = 1f;

    IntervalUtil afterimageInterval = new IntervalUtil(0.1f, 0.15f);
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float boostLimit = 0f;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (!ended) {
            boostLimit = ship.getVelocity().length() + BOOST_SPEED;
        }

        if (state.equals(State.IN)) {
            List<ShipEngineControllerAPI.ShipEngineAPI> engineList = ship.getEngineController().getShipEngines();
            if (!started) {
                //todo - play sound

                boostFraction = 1 - (ship.getNumFlameouts() / (float) engineList.size());

                started = true;
            }

            for (ShipEngineAPI engine : engineList) {
                ship.getEngineController().extendFlame(engine, FLAME_LENGTH_FRACTION, 1f, FLAME_GLOW_FRACTION);
            }

        } else if (state.equals(State.ACTIVE)) {
            ship.setJitterUnder(this, JITTER_COLOUR, 10f * effectLevel, 6, 1f);
        } else if (state.equals(State.OUT)) {
            if (!ended) {
                VectorUtils.resize(ship.getVelocity(), boostLimit * boostFraction * effectLevel);

                ended = true;
            }
            afterimageInterval.advance(0.01f);
            if (afterimageInterval.intervalElapsed()) {
                ship.addAfterimage(CONTRAIL_COLOUR, 0f, 0f, ship.getVelocity().getX() * -0.5f, ship.getVelocity().getY() * -0.5f, 5f, 0.1f, 0f, 0.8f, true, false, false);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        started = false;
        ended = false;
        boostForward = true;
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
