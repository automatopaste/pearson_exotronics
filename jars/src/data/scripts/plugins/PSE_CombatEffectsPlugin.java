package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PSE_CombatEffectsPlugin extends BaseEveryFrameCombatPlugin {
    public static final String ENGINE_DATA_KEY = "PSE_EngineData";

    private static final float MINI_FLAK_TIME = 0.4f;
    private static final float MINI_FLAK_MIN_RADIUS = 10f;
    private static final float MINI_FLAK_MAX_RADIUS = 30f;
    private static final float MINI_FLAK_ANGLE_RANGE = 180f;
    private static final Color MINI_FLAK_EXPLOSION_COLOUR = new Color(194, 97, 60, 180);
    private static final Color MINI_FLAK_PARTICLE_COLOUR = new Color(167, 167, 73, 255);

    private CombatEngineAPI engine;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.getCustomData().put(ENGINE_DATA_KEY, new PSE_EngineData());
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        PSE_EngineData data = (PSE_EngineData) engine.getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }
        data.effectsPlugin = this;

        ListIterator<PSE_Explosion> iterator = data.explosions.listIterator();
        while (iterator.hasNext()) {
            PSE_Explosion explosion = iterator.next();

            explosion.currTime -= amount;
            if (explosion.currTime <= 0f) {
                iterator.remove();
                continue;
            }

            explosion.advance(amount, engine);
        }
    }

    abstract static class PSE_Explosion {
        float currTime;
        float maxTime;
        float minRadius;
        float maxRadius;
        Vector2f location;

        public abstract void advance(float amount, CombatEngineAPI engine);
    }

    public static class PSE_ExplosionWithParticles extends PSE_Explosion {
        float angleRange;
        Color particleColour;
        Color explosionColour;
        private final IntervalUtil particleTracker;

        PSE_ExplosionWithParticles(float maxTime, Vector2f location, float minRadius, float maxRadius, float angleRange, Color explosionColour, Color particleColour) {
            this.maxTime = maxTime;
            this.location = location;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.angleRange = angleRange;
            this.explosionColour = explosionColour;
            this.particleColour = particleColour;

            currTime = maxTime;
            particleTracker = new IntervalUtil(0.01f, 0.05f);
        }

        public void advance(float amount, CombatEngineAPI engine) {
            particleTracker.advance(amount);
            if (!particleTracker.intervalElapsed()) {
                return;
            }

            Vector2f loc = MathUtils.getPointOnCircumference(location, minRadius + ((float) Math.random() * (maxRadius - minRadius)), (float) Math.random() * 360f);
            Vector2f velAngle = Vector2f.sub(loc, location, new Vector2f());
            VectorUtils.rotate(velAngle, (2 * maxRadius * (float) Math.random()) - maxRadius);
            velAngle.normalise();
            velAngle.scale(150f);
            float size = 15f * (currTime / maxTime);
            float lifetime = MINI_FLAK_TIME + 0.2f;

            engine.addSmoothParticle(loc, velAngle, size, (currTime / maxTime), lifetime, particleColour);
        }
    }

    public static class PSE_ParticleRing extends PSE_Explosion {
        Color particleColour;
        float particleSize;

        PSE_ParticleRing(float maxTime, Vector2f location, float minRadius, float maxRadius, Color particleColour, float particleSize) {
            this.maxTime = maxTime;
            this.location = location;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.particleColour = particleColour;
            this.particleSize = particleSize;

            currTime = maxTime;
        }

        public void advance(float amount, CombatEngineAPI engine) {
            int num = 12;
            for (int i = 0; i < num; i++) {
                Vector2f loc = MathUtils.getPointOnCircumference(location, minRadius, i * (360f / num));
                Vector2f vel = Vector2f.sub(loc, location, new Vector2f());
                vel.normalise();
                vel.scale((maxRadius - minRadius) / maxTime);

                Global.getCombatEngine().addSmoothParticle(loc, vel, particleSize, 1f, maxTime, particleColour);
            }
        }
    }

    public static final class PSE_EngineData {
        //final List<PSEDrone> drones = new ArrayList<>();
        List<PSE_Explosion> explosions = new LinkedList<>();
        PSE_CombatEffectsPlugin effectsPlugin;
    }

    public void spawnMiniStarburstFlakExplosion(Vector2f loc) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        Global.getCombatEngine().spawnExplosion(
                loc,
                new Vector2f(0f, 0f),
                MINI_FLAK_EXPLOSION_COLOUR,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_TIME
        );

        data.explosions.add(new PSE_ExplosionWithParticles(
                MINI_FLAK_TIME,
                loc,
                MINI_FLAK_MIN_RADIUS,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_ANGLE_RANGE,
                MINI_FLAK_EXPLOSION_COLOUR,
                MINI_FLAK_PARTICLE_COLOUR
        ));
        data.explosions.add(new PSE_ParticleRing(
                MINI_FLAK_TIME * 0.3f,
                loc,
                MINI_FLAK_MIN_RADIUS,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_EXPLOSION_COLOUR,
                10f
        ));
    }

    public void spawnParticleRing(PSE_ParticleRing ring) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.explosions.add(ring);
    }
}
