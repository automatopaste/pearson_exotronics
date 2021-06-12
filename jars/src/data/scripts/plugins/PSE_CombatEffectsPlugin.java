package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
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
        if (data == null) return;

        for (PSE_RenderObject object : data.renderObjects) {
            if (layer != object.layer || !object.shouldRender(engine)) continue;

            object.render(engine);

            //throw new ClassCastException("troled");
        }
    }

    IntervalUtil interval = new IntervalUtil(1f, 1f);

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

        if (interval.intervalElapsed()) {
            for (ShipAPI ship : engine.getShips()) {
                SpriteAPI sprite = ship.getSpriteAPI();
                spawnSpriteTrail(
                        sprite,
                        new Vector2f(sprite.getWidth(), sprite.getHeight()),
                        ship.getLocation(),
                        ship.getVelocity(),
                        new Vector2f(),
                        ship.getFacing(),
                        ship.getAngularVelocity(),
                        0f,
                        new IntervalUtil(0.2f, 0.2f),
                        0.5f,
                        CombatEngineLayers.UNDER_SHIPS_LAYER,
                        new Color(255, 0, 0, 255),
                        0.1f,
                        0f,
                        1f,
                        2f
                );
            }
        }
    }

    public abstract static class PSE_RenderObject {
        CombatEngineLayers layer;

        public abstract void advance(float amount);

        public abstract void render(CombatEngineAPI engine);

        public abstract boolean shouldRemove(float amount);

        public abstract boolean shouldRender(CombatEngineAPI engine);
    }

    public static class PSE_PrimitiveParticle extends PSE_RenderObject{
        float maxVertexRadius;
        float vertexRadius;
        float vertexRadiusSpeed;
        int poly;
        Color color;

        float currTime;

        Vector2f location;
        Vector2f velocity;
        Vector2f acceleration;
        float lifetime;
        float rotation;
        float rotationVelocity;
        float rotationAcceleration;
        float alphaRampRatio;
        float minAlpha;
        float maxAlpha;
        float lineWidth;

        PSE_PrimitiveParticle (
                Vector2f location,
                Vector2f velocity,
                Vector2f acceleration,
                float lifetime,
                Color color,
                CombatEngineLayers layer,
                float vertexRadius,
                float vertexRadiusSpeed,
                int poly,
                float rotation,
                float rotationVelocity,
                float rotationAcceleration,
                float alphaRampRatio,
                float minAlpha,
                float maxAlpha,
                float lineWidth
        ) {
            this.location = new Vector2f(location);
            this.velocity = new Vector2f(velocity);
            this.acceleration = new Vector2f(acceleration);
            this.color = color;
            this.layer = layer;
            this.maxVertexRadius = vertexRadius;
            this.vertexRadiusSpeed = vertexRadiusSpeed;
            this.poly = poly;

            this.lifetime = lifetime;
            currTime = lifetime;
            this.rotation = rotation;
            this.rotationVelocity = rotationVelocity;
            this.rotationAcceleration = rotationAcceleration;
            this.alphaRampRatio = alphaRampRatio;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.lineWidth = lineWidth;
        }

        @Override
        public void advance(float amount) {
            vertexRadius -= vertexRadiusSpeed * amount;

            currTime -= amount;

            rotationVelocity += rotationAcceleration * amount;
            rotation += rotationVelocity * amount;

            Vector2f.add(velocity, (Vector2f) new Vector2f(acceleration).scale(amount), velocity);
            Vector2f.add(location, (Vector2f) new Vector2f(velocity).scale(amount), location);
        }

        @Override
        public void render(CombatEngineAPI engine) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glEnable(GL_LINE_SMOOTH);
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glBegin(GL_POLYGON);

            float ratio = currTime / lifetime;
            float alpha = PSE_MiscUtils.getSmoothAlpha(ratio, alphaRampRatio, minAlpha, maxAlpha);
            glColor(color, alpha * 0.25f, true);

            for (int i = 0; i < poly; i++) {
                Vector2f vertex = getVertex(i);
                glVertex2f(vertex.x, vertex.y);
            }

            glEnd();

            glBegin(GL_LINE_LOOP);

            glColor(color, alpha, true);

            glLineWidth(lineWidth / engine.getViewport().getViewMult());

            for (int i = 0; i < poly; i++) {
                Vector2f vertex = getVertex(i);
                glVertex2f(vertex.x, vertex.y);
            }

            glEnd();

            glDisable(GL_BLEND);
            glPopAttrib();
        }

        public Vector2f getVertex(int index) {
            float divisor = 360f / poly;
            float angle = divisor * index;

            Vector2f up = (Vector2f) new Vector2f(0f, 1f).scale(vertexRadius);
            up = VectorUtils.rotate(up, angle);
            up = VectorUtils.rotate(up, rotation);

            return Vector2f.add(up, location, null);
        }

        @Override
        public boolean shouldRemove(float amount) {
            return currTime <= 0f;
        }

        @Override
        public boolean shouldRender(CombatEngineAPI engine) {
            return engine.getViewport().isNearViewport(location, maxVertexRadius);
        }
    }

    public static class PSE_EntityTrackingPrimitiveParticle extends PSE_PrimitiveParticle {
        private final CombatEntityAPI entity;

        PSE_EntityTrackingPrimitiveParticle(
                CombatEntityAPI entity,
                float lifetime,
                Color color,
                CombatEngineLayers layer,
                float vertexRadius,
                float vertexRadiusSpeed,
                int poly,
                float alphaRampRatio,
                float minAlpha,
                float maxAlpha,
                float lineWidth
        ) {
            super(
                    entity.getLocation(),
                    entity.getVelocity(),
                    new Vector2f(),
                    lifetime,
                    color,
                    layer,
                    vertexRadius,
                    vertexRadiusSpeed,
                    poly,
                    entity.getFacing(),
                    0f,
                    0f,
                    alphaRampRatio,
                    minAlpha,
                    maxAlpha,
                    lineWidth
            );

            this.entity = entity;
        }

        @Override
        public void advance(float amount) {
            vertexRadius -= vertexRadiusSpeed * amount;

            location = entity.getLocation();
            rotation = entity.getFacing();
        }
    }

    public static class PSE_ShipBoundRender extends PSE_RenderObject {
        ShipAPI ship;
        float sizeMult;

        public PSE_ShipBoundRender(
                ShipAPI ship,
                float sizeMult
        ) {
            this.ship = ship;
            this.sizeMult = sizeMult;
        }

        @Override
        public void advance(float amount) {
            sizeMult += 1f * amount;
        }

        @Override
        public void render(CombatEngineAPI engine) {
            BoundsAPI bounds = ship.getExactBounds();
            if (bounds == null) return;
            if (!engine.getViewport().isNearViewport(ship.getLocation(), ship.getCollisionRadius())) return;

            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

            bounds.update(ship.getLocation(), ship.getFacing());

            glBegin(GL_LINE_LOOP);

            List<Vector2f> points = new ArrayList<>(bounds.getSegments().size());
            for (BoundsAPI.SegmentAPI segment : bounds.getSegments()) {
                Vector2f toSeg = Vector2f.sub(segment.getP1(), ship.getLocation(), new Vector2f());
                toSeg.scale(sizeMult);
                points.add(Vector2f.add(toSeg, ship.getLocation(), null));
            }

            for (Vector2f point : points) {
                glVertex2f(point.x, point.y);
            }

            glEnd();

            glBegin(GL_POLYGON);

            for (Vector2f point : points) {
                glVertex2f(point.x, point.y);
            }

            glEnd();

            glDisable(GL_BLEND);
            glPopAttrib();
        }

        @Override
        public boolean shouldRemove(float amount) {
            return false;
        }

        @Override
        public boolean shouldRender(CombatEngineAPI engine) {
            return engine.getViewport().isNearViewport(ship.getLocation(), ship.getCollisionRadius());
        }
    }

    public static class PSE_SpriteTrail extends PSE_RenderObject {
        SpriteAPI sprite;
        Vector2f size;
        Vector2f location;
        Vector2f velocity;
        Vector2f acceleration;
        float angle;
        float angleVelocity;
        float angleAcceleration;
        IntervalUtil ghostInterval;
        float ghostLifetime;
        Color outColor;
        float alphaRampRatio;
        float minAlpha;
        float maxAlpha;
        float lifetime;
        float currTime;

        public PSE_SpriteTrail (
                SpriteAPI sprite,
                Vector2f size,
                Vector2f location,
                Vector2f velocity,
                Vector2f acceleration,
                float angle,
                float angleVelocity,
                float angleAcceleration,
                IntervalUtil ghostInterval,
                float ghostLifetime,
                CombatEngineLayers layer,
                Color outColor,
                float alphaRampRatio,
                float minAlpha,
                float maxAlpha,
                float lifetime
        ) {
            this.sprite = sprite;
            this.size = size;
            this.location = location;
            this.velocity = velocity;
            this.acceleration = acceleration;
            this.angle = angle;
            this.angleVelocity = angleVelocity;
            this.angleAcceleration = angleAcceleration;
            this.ghostInterval = ghostInterval;
            this.ghostLifetime = ghostLifetime;
            this.layer = layer;
            this.outColor = outColor;
            this.alphaRampRatio = alphaRampRatio;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.lifetime = lifetime;
            currTime = lifetime;
        }

        @Override
        public void advance(float amount) {
            Vector2f.add(velocity, (Vector2f) new Vector2f(acceleration).scale(amount), velocity);
            Vector2f.add(location, (Vector2f) new Vector2f(velocity).scale(amount), location);

            angleVelocity += angleAcceleration;
            angle += angleVelocity;

            currTime -= amount;

            ghostInterval.advance(amount);
            if (ghostInterval.intervalElapsed()) {
                spawnSpriteObject(sprite, size, location, new Vector2f(), new Vector2f(), angle, angleVelocity, angleAcceleration, ghostLifetime, outColor, layer, alphaRampRatio, minAlpha, maxAlpha);
            }
        }

        @Override
        public void render(CombatEngineAPI engine) {
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, Global.getSettings().getScreenWidth(), 0, Global.getSettings().getScreenHeight(), -1, 1);

            sprite.setAngle(angle);
            sprite.setSize(size.x, size.y);
            sprite.renderAtCenter(location.x, location.y);

            glEnd();
            glDisable(GL_BLEND);
            glPopAttrib();
            glColor(outColor, PSE_MiscUtils.getSqrtAlpha(currTime / lifetime, alphaRampRatio, minAlpha, maxAlpha));
            glPopMatrix();
        }

        @Override
        public boolean shouldRemove(float amount) {
            return currTime <= 0f;
        }

        @Override
        public boolean shouldRender(CombatEngineAPI engine) {
            return engine.getViewport().isNearViewport(location, size.length());
        }
    }

    public static void spawnSpriteTrail(
            SpriteAPI sprite,
            Vector2f size,
            Vector2f location,
            Vector2f velocity,
            Vector2f acceleration,
            float angle,
            float angleVelocity,
            float angleAcceleration,
            IntervalUtil ghostInterval,
            float ghostLifetime,
            CombatEngineLayers layer,
            Color outColor,
            float alphaRampRatio,
            float minAlpha,
            float maxAlpha,
            float lifetime
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.renderObjects.add(new PSE_SpriteTrail(sprite, size, location, velocity, acceleration, angle, angleVelocity, angleAcceleration, ghostInterval, ghostLifetime, layer, outColor, alphaRampRatio, minAlpha, maxAlpha, lifetime));
    }

    public static class PSE_SpriteObject extends PSE_RenderObject{
        SpriteAPI sprite;
        Vector2f size;
        Color color;
        Vector2f location;
        Vector2f velocity;
        Vector2f acceleration;
        float angle;
        float angleVelocity;
        float angleAcceleration;
        float lifetime;
        float currTime;
        float alphaRampRatio;
        float minAlpha;
        float maxAlpha;

        PSE_SpriteObject (
                SpriteAPI sprite,
                Vector2f size,
                Vector2f location,
                Vector2f velocity,
                Vector2f acceleration,
                float angle,
                float angleVelocity,
                float angleAcceleration,
                float lifetime,
                Color color,
                CombatEngineLayers layer,
                float alphaRampRatio,
                float minAlpha,
                float maxAlpha
        ) {
            this.sprite = sprite;
            this.size = size;
            this.location = new Vector2f(location);
            this.velocity = new Vector2f(velocity);
            this.acceleration = new Vector2f(acceleration);
            this.color = color;
            this.layer = layer;
            this.lifetime = lifetime;
            currTime = lifetime;
            this.angle = angle;
            this.angleVelocity = angleVelocity;
            this.angleAcceleration = angleAcceleration;
            this.alphaRampRatio = alphaRampRatio;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
        }

        @Override
        public void advance(float amount) {
            angleVelocity += angleAcceleration * amount;
            angle += angleVelocity * amount;

            currTime -= amount;

            Vector2f.add(velocity, (Vector2f) new Vector2f(acceleration).scale(amount), velocity);
            Vector2f.add(location, (Vector2f) new Vector2f(velocity).scale(amount), location);
        }

        @Override
        public void render(CombatEngineAPI engine) {
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, Global.getSettings().getScreenWidth(), 0, Global.getSettings().getScreenHeight(), -1, 1);

            sprite.setAngle(angle);
            sprite.setSize(size.x, size.y);
            sprite.renderAtCenter(location.x, location.y);

            glEnd();
            glDisable(GL_BLEND);
            glPopAttrib();
            glColor(color, PSE_MiscUtils.getSqrtAlpha(currTime / lifetime, alphaRampRatio, minAlpha, maxAlpha));
            glPopMatrix();
        }

        @Override
        public boolean shouldRemove(float amount) {
            return currTime <= 0f;
        }

        @Override
        public boolean shouldRender(CombatEngineAPI engine) {
            return engine.getViewport().isNearViewport(location, size.length());
        }
    }

    public static void spawnSpriteObject(
            SpriteAPI sprite,
            Vector2f size,
            Vector2f location,
            Vector2f velocity,
            Vector2f acceleration,
            float angle,
            float angleVelocity,
            float angleAcceleration,
            float lifetime,
            Color outColor,
            CombatEngineLayers layer,
            float alphaRampRatio,
            float minAlpha,
            float maxAlpha
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.renderObjects.add(new PSE_SpriteObject(sprite, size, location, velocity, acceleration, angle, angleVelocity, angleAcceleration, lifetime, outColor, layer, alphaRampRatio, minAlpha, maxAlpha));
    }

    public static void spawnPrimitiveParticle (
            Vector2f location,
            Vector2f velocity,
            Vector2f acceleration,
            float lifetime,
            Color color,
            CombatEngineLayers layer,
            float vertexRadius,
            float vertexRadiusSpeed,
            int poly,
            float rotation,
            float rotationVelocity,
            float rotationAcceleration,
            float alphaRampRatio,
            float minAlpha,
            float maxAlpha,
            float lineWidth
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.renderObjects.add(new PSE_PrimitiveParticle(location, velocity, acceleration, lifetime, color, layer, vertexRadius, vertexRadiusSpeed, poly, rotation, rotationVelocity, rotationAcceleration, alphaRampRatio, minAlpha, maxAlpha, lineWidth));
    }

    public static void spawnEntityTrackingPrimitiveParticle(
            CombatEntityAPI entity,
            float lifetime,
            Color color,
            CombatEngineLayers layer,
            float vertexRadius,
            float vertexRadiusSpeed,
            int poly,
            float alphaRampRatio,
            float minAlpha,
            float maxAlpha,
            float lineWidth
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.renderObjects.add(new PSE_EntityTrackingPrimitiveParticle(entity, lifetime, color, layer, vertexRadius, vertexRadiusSpeed, poly, alphaRampRatio, minAlpha, maxAlpha, lineWidth));
    }

    public static void spawnShipBoundRender (
            ShipAPI ship,
            float sizeMult
    ) {
        PSE_EngineData data = (PSE_EngineData) Global.getCombatEngine().getCustomData().get(ENGINE_DATA_KEY);
        if (data == null) {
            return;
        }

        data.renderObjects.add(new PSE_ShipBoundRender(ship, sizeMult));
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

    public static void beamPrimitiveParticlesAdvance(
            IntervalUtil interval,
            int numParticlesPerInterval,
            BeamAPI beam,
            float amount,
            float width,
            int poly,
            float velDeviation,
            float speed,
            Vector2f acc,
            float lifetime,
            CombatEngineLayers layer,
            float radius,
            float rotationVelocity,
            float rotationAcceleration,
            float alphaRampRatio,
            float maxAlpha,
            float lineWidth
    ) {
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        for (int i = 0; i < numParticlesPerInterval; i++) {
            Vector2f to = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());
            to.scale((float) Math.random());

            float currAngle = beam.getWeapon().getCurrAngle();

            Vector2f offset = new Vector2f((float) ((Math.random() * (2f * width)) - width), 0f);
            VectorUtils.rotate(offset, currAngle);

            Vector2f loc = Vector2f.add(to, beam.getWeapon().getLocation(), new Vector2f());
            Vector2f.add(loc, offset, loc);

            Vector2f vel = VectorUtils.resize(to, speed);
            if (velDeviation != 0f) VectorUtils.rotate(vel, (float) ((Math.random() * 2f * velDeviation) - velDeviation));

            spawnPrimitiveParticle(
                    loc,
                    vel,
                    acc,
                    lifetime,
                    beam.getFringeColor(),
                    layer,
                    radius,
                    radius / lifetime,
                    poly,
                    currAngle,
                    rotationVelocity,
                    rotationAcceleration,
                    alphaRampRatio,
                    0f,
                    maxAlpha,
                    lineWidth
            );
        }
    }
}