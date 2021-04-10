package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class PSE_CombatEffectsPlugin extends BaseEveryFrameCombatPlugin {
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

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getCustomData().put(ENGINE_DATA_KEY, new PSE_EngineData());
        CombatLayeredRenderingPlugin layerPlugin = new PSE_LayeredEffectsPlugin(this);
        engine.addLayeredRenderingPlugin(layerPlugin);
    }

    public void render(CombatEngineLayers layer) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        PSE_EngineData data = (PSE_EngineData) engine.getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        for (PSE_PrimitiveParticle particle : data.primitiveParticles) {
            if (layer != particle.layer) continue;

            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glBegin(GL_POLYGON);

            float alpha = particle.getSmoothAlpha();
            alpha = MathUtils.clamp(alpha, 0f, 1f);
            glColor(particle.color, alpha * 0.25f, true);

            for (int i = 0; i < particle.poly; i++) {
                Vector2f vertex = particle.getVertex(i);
                glVertex2f(vertex.x, vertex.y);
            }

            glEnd();

            glBegin(GL_LINE_LOOP);

            glColor(particle.color, alpha, true);

            for (int i = 0; i < particle.poly; i++) {
                Vector2f vertex = particle.getVertex(i);
                glVertex2f(vertex.x, vertex.y);
            }

            glEnd();

            glDisable(GL_BLEND);
            glPopAttrib();
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        PSE_EngineData data = (PSE_EngineData) engine.getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }
        data.effectsPlugin = this;

        /*for (ShipAPI ship : engine.getShips()) {
            if (Math.random() < 0.01f) {
                data.primitiveParticles.add(new PSE_PrimitiveParticle(
                        new Vector2f(ship.getLocation()),
                        VectorUtils.rotate((Vector2f) new Vector2f(ship.getVelocity()).scale(-1f), MathUtils.getRandomNumberInRange(-60f, 60f)),
                        (Vector2f) new Vector2f(ship.getVelocity()).scale(-0.5f),
                        5f,
                        new Color(0, 255, 135),
                        CombatEngineLayers.BELOW_SHIPS_LAYER,
                        MathUtils.getRandomNumberInRange(5f,30f),
                        MathUtils.getRandomNumberInRange(3, 7),
                        MathUtils.getRandomNumberInRange(0f, 360f),
                        MathUtils.getRandomNumberInRange(-360f, 360f),
                        MathUtils.getRandomNumberInRange(-30f, 30f),
                        0.3f,
                        1f
                ));
            }
        }*/

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

        ListIterator<PSE_PrimitiveParticle> particleIterator = data.primitiveParticles.listIterator();
        while (particleIterator.hasNext()) {
            PSE_PrimitiveParticle particle = particleIterator.next();

            particle.currtime -= amount;
            if (particle.currtime <= 0f) {
                particleIterator.remove();
                continue;
            }

            particle.advance(amount);
        }
    }

    public static class PSE_PrimitiveParticle {
        Vector2f location;
        Vector2f velocity;
        Vector2f acceleration;
        float lifetime;
        float maxVertexRadius;
        float vertexRadius;
        int poly;
        Color color;
        CombatEngineLayers layer;
        float rotation;
        float rotationVelocity;
        float rotationAcceleration;
        float alphaRampRatio;
        float maxAlpha;

        float currtime;
        float lifeRatio; //1f -> 0f

        PSE_PrimitiveParticle(
                Vector2f location,
                Vector2f velocity,
                Vector2f acceleration,
                float lifetime,
                Color color,
                CombatEngineLayers layer,
                float vertexRadius,
                int poly,
                float rotation,
                float rotationVelocity,
                float rotationAcceleration,
                float alphaRampRatio,
                float maxAlpha
        ) {
            this.location = location;
            this.velocity = velocity;
            this.acceleration = acceleration;
            this.lifetime = lifetime;
            this.color = color;
            this.layer = layer;
            this.maxVertexRadius = vertexRadius;
            this.poly = poly;
            this.rotation = rotation;
            this.rotationVelocity = rotationVelocity;
            this.rotationAcceleration = rotationAcceleration;
            this.alphaRampRatio = alphaRampRatio;
            this.maxAlpha = maxAlpha;

            currtime = lifetime;
            lifeRatio = 1f;
        }

        public void advance(float amount) {
            lifeRatio = currtime / lifetime;

            rotationVelocity += rotationAcceleration * amount;
            rotation += rotationVelocity * amount;

            Vector2f.add(velocity, (Vector2f) new Vector2f(acceleration).scale(amount), velocity);
            Vector2f.add(location, (Vector2f) new Vector2f(velocity).scale(amount), location);

            vertexRadius = maxVertexRadius * lifeRatio;
        }

        public Vector2f getVertex(int index) {
            float divisor = 360f / poly;
            float angle = divisor * index;

            Vector2f up = (Vector2f) new Vector2f(0f, 1f).scale(maxVertexRadius);
            up = VectorUtils.rotate(up, angle);
            up = VectorUtils.rotate(up, rotation);

            return Vector2f.add(up, location, null);
        }

        public float getSmoothAlpha() {
            float alpha;
            if (lifeRatio > 1 - alphaRampRatio) {
                alpha = (1f - lifeRatio) / alphaRampRatio;
            } else {
                alpha = lifeRatio / (1f - alphaRampRatio);
            }

            MathUtils.clamp(alpha, 0f, maxAlpha);

            return alpha;
        }
    }

    public static void spawnPrimitiveParticle(
            Vector2f location,
            Vector2f velocity,
            Vector2f acceleration,
            float lifetime,
            Color color,
            CombatEngineLayers layer,
            float vertexRadius,
            int poly,
            float rotation,
            float rotationVelocity,
            float rotationAcceleration,
            float alphaRampRatio,
            float maxAlpha
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.primitiveParticles.add(new PSE_PrimitiveParticle(location, velocity, acceleration, lifetime, color, layer, vertexRadius, poly, rotation, rotationVelocity, rotationAcceleration, alphaRampRatio, maxAlpha));
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
        List<PSE_PrimitiveParticle> primitiveParticles = new LinkedList<>();
        PSE_CombatEffectsPlugin effectsPlugin;
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

    public void spawnParticleRing(PSE_ParticleRing ring) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.explosions.add(ring);
    }
}
