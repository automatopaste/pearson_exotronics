package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PSE_ExplosionEffectsPlugin extends BaseEveryFrameCombatPlugin {
    public static final String ENGINE_DATA_KEY = "PSE_EngineData";

    private static final float MINI_FLAK_TIME = 0.4f;
    private static final float MINI_FLAK_MIN_RADIUS = 10f;
    private static final float MINI_FLAK_MAX_RADIUS = 30f;
    private static final float MINI_FLAK_ANGLE_RANGE = 180f;
    private static final Color MINI_FLAK_EXPLOSION_COLOUR = new Color(194, 97, 60, 180);
    private static final Color MINI_FLAK_PARTICLE_COLOUR = new Color(167, 167, 73, 255);
    private static final int MINI_FLAK_NUM_PARTICLES = 12;

    private static final float HELSING_FLAK_TIME = 0.9f;
    private static final float HELSING_FLAK_MIN_RADIUS = 25f;
    private static final float HELSING_FLAK_MAX_RADIUS = 35f;
    private static final float HELSING_FLAK_ANGLE_RANGE = 360f;
    private static final Color HELSING_FLAK_EXPLOSION_COLOUR = new Color(248, 153, 116, 210);
    private static final Color HELSING_FLAK_PARTICLE_COLOUR = new Color(255, 93, 93, 255);

    private Set<ShipAPI> ships = new HashSet<>();

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getCustomData().put(ENGINE_DATA_KEY, new PSE_EngineData());
        CombatLayeredRenderingPlugin layerPlugin = new PSE_LayeredEffectsPlugin(this);
        engine.addLayeredRenderingPlugin(layerPlugin);

        engine.addLayeredRenderingPlugin(new PSE_PolygonParticlePlugin());
    }

    public void render(CombatEngineLayers layer) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        PSE_EngineData data = (PSE_EngineData) engine.getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) return;

        for (PSE_RenderObject object : data.renderObjects) {
            if (layer != object.layer || !object.shouldRender(engine)) continue;

            object.render(engine);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

//        for (ShipAPI ship : engine.getShips()) {
//            if (!ships.contains(ship)) {
//                engine.addLayeredRenderingPlugin(new HaloRenderer(ship));
//                ships.add(ship);
//            }
//        }

        PSE_EngineData data = (PSE_EngineData) engine.getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }
        data.effectsPlugin = this;

        ListIterator<PSE_Explosion> explosionIterator = data.explosions.listIterator();
        while (explosionIterator.hasNext()) {
            PSE_Explosion explosion = explosionIterator.next();

            explosion.currTime -= amount;
            if (explosion.currTime <= 0f) {
                explosionIterator.remove();
                continue;
            }

            explosion.advance(amount, engine);
        }

        ListIterator<PSE_RenderObject> objectIterator = data.renderObjects.listIterator();
        while (objectIterator.hasNext()) {
            PSE_RenderObject object = objectIterator.next();

            object.advance(amount);

            if (object.shouldRemove(amount)) objectIterator.remove();
        }
    }

    public abstract static class PSE_RenderObject {
        CombatEngineLayers layer;

        public abstract void advance(float amount);

        public abstract void render(CombatEngineAPI engine);

        public abstract boolean shouldRemove(float amount);

        public abstract boolean shouldRender(CombatEngineAPI engine);
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
            particleTracker = new IntervalUtil(0.05f, 0.1f);
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
        int numParticles;

        PSE_ParticleRing(float maxTime, Vector2f location, float minRadius, float maxRadius, Color particleColour, float particleSize, int numParticles) {
            this.maxTime = maxTime;
            this.location = location;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.particleColour = particleColour;
            this.particleSize = particleSize;
            this.numParticles = numParticles;

            currTime = maxTime;
        }

        public void advance(float amount, CombatEngineAPI engine) {
            for (int i = 0; i < numParticles; i++) {
                Vector2f loc = MathUtils.getPointOnCircumference(location, minRadius, i * (360f / numParticles));
                Vector2f vel = Vector2f.sub(loc, location, new Vector2f());
                vel.normalise();
                vel.scale((maxRadius - minRadius) / maxTime);

                Global.getCombatEngine().addSmoothParticle(loc, vel, particleSize, 1f, maxTime, particleColour);
            }
        }
    }

    public static final class PSE_EngineData {
        List<PSE_Explosion> explosions = new LinkedList<>();
        List<PSE_RenderObject> renderObjects = new LinkedList<>();
        PSE_ExplosionEffectsPlugin effectsPlugin;
    }

    public void spawnMiniStarburstFlakExplosion(Vector2f location) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        Global.getCombatEngine().spawnExplosion(
                location,
                new Vector2f(0f, 0f),
                MINI_FLAK_EXPLOSION_COLOUR,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_TIME
        );

        data.explosions.add(new PSE_ExplosionWithParticles(
                MINI_FLAK_TIME,
                location,
                MINI_FLAK_MIN_RADIUS,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_ANGLE_RANGE,
                MINI_FLAK_EXPLOSION_COLOUR,
                MINI_FLAK_PARTICLE_COLOUR
        ));
        data.explosions.add(new PSE_ParticleRing(
                MINI_FLAK_TIME * 0.3f,
                location,
                MINI_FLAK_MIN_RADIUS,
                MINI_FLAK_MAX_RADIUS,
                MINI_FLAK_EXPLOSION_COLOUR,
                10f,
                MINI_FLAK_NUM_PARTICLES
        ));
    }

    public void spawnHelsingFlakExplosion(Vector2f location) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        Global.getCombatEngine().spawnExplosion(
                location,
                new Vector2f(0f, 0f),
                HELSING_FLAK_EXPLOSION_COLOUR,
                HELSING_FLAK_MAX_RADIUS,
                HELSING_FLAK_TIME
        );

        data.explosions.add(new PSE_ExplosionWithParticles(
                HELSING_FLAK_TIME,
                location,
                HELSING_FLAK_MIN_RADIUS,
                HELSING_FLAK_MAX_RADIUS,
                HELSING_FLAK_ANGLE_RANGE,
                HELSING_FLAK_EXPLOSION_COLOUR,
                HELSING_FLAK_PARTICLE_COLOUR
        ));
    }
}