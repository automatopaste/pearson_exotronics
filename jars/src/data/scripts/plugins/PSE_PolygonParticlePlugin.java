package data.scripts.plugins;

import cmu.CMUtils;
import cmu.plugins.renderers.PolygonParticleRenderer;
import cmu.shaders.particles.BaseParticle;
import cmu.shaders.particles.ComputeFunction;
import cmu.shaders.particles.PolygonParticle;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import javafx.scene.shape.Polygon;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;

public class PSE_PolygonParticlePlugin extends BaseCombatLayeredRenderingPlugin {

    public static final String DATA_KEY = "PSE_polygons";

    public static final boolean SHADER_MODE = true;
    private final PolygonParticleRenderer polygonRenderer;

    private final List<PolygonParticle> particles;

    public PSE_PolygonParticlePlugin() {
        particles = new ArrayList<>();

        if (SHADER_MODE) {
            polygonRenderer = (PolygonParticleRenderer) CMUtils.initBuiltinParticleRenderer(CMUtils.BuiltinParticleRenderers.POLYGON, CombatEngineLayers.BELOW_SHIPS_LAYER);
            polygonRenderer.setBlendEquation(GL_FUNC_ADD);
        } else {
            polygonRenderer = null;
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
        CombatEngineAPI engine = Global.getCombatEngine();

        engine.getCustomData().put(DATA_KEY, this);
    }

//    private IntervalUtil interval = new IntervalUtil(15f, 15f);

    @Override
    public void advance(float amount) {
        if (SHADER_MODE) {
//            interval.advance(amount);
//            if (!interval.intervalElapsed()) return;
//
//            for (int i = 0; i < 1000; i++) {
//                spawnRandomParticle();
//            }

            return;
        }

        for (Iterator<PolygonParticle> iterator = particles.iterator(); iterator.hasNext();) {
            PolygonParticle p = iterator.next();

            if (p.age > p.lifetime) iterator.remove();
            else p.advance(amount);
        }
    }

//    private void spawnRandomParticle() {
//        Random r = new Random();
//
//        Color c1 = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255), 200);
//        Color c2 = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255), 200);
//
//        int poly = r.nextInt(6);
//        PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(poly, CombatEngineLayers.BELOW_SHIPS_LAYER, c2, 0.8f);
//        params.color = c1;
//        float i1 = r.nextFloat() * 100f;
//        params.sizeInit = new Vector2f(i1, i1);
//        float i2 = r.nextFloat() * 100f;
//        params.sizeFinal = new Vector2f(i2, i2);
//        params.lifetime = 10f;
//        params.vel = new Vector2f(r.nextFloat() * 20f - 10f, r.nextFloat() * 20f - 10f);
//        params.acc = new Vector2f(r.nextFloat() * 20f - 10f, r.nextFloat() * 20f - 10f);
//        params.angVel = r.nextFloat() * 60f - 30f;
//        params.angAcc = r.nextFloat() * 60f - 30f;
//        params.angle = r.nextFloat() * 360f;
//
//        Vector2f loc = new Vector2f(800f, 0f);
//        VectorUtils.rotate(loc, r.nextFloat() * 360f);
//
//        polygonRenderer.addParticle(new PolygonParticle(loc, params));
//    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (SHADER_MODE) {
            return;
        }

        fixedRender(layer, viewport);
    }

    private void fixedRender(CombatEngineLayers layer, ViewportAPI viewport) {
        openGL();

        for (PolygonParticle p : particles) {
            if (layer != p.layer) continue;

            glPushMatrix();
            glTranslatef(viewport.convertWorldXtoScreenX(p.location.x), viewport.convertWorldYtoScreenY(p.location.y), 0f);

            glBegin(GL_POLYGON);

            glColor(p.color, p.alpha * p.alphaMult, true);

            float divisor = 360f / p.poly;

            Vector4f[] vs = new Vector4f[p.poly];
            for (int i = 0; i < p.poly; i++) {
                Vector4f vertex = getVertex(i, p, divisor, p.size.y, viewport);
                glVertex2f(vertex.x, vertex.y);
                vs[i] = vertex;
            }

            glEnd();

            glBegin(GL_TRIANGLE_STRIP);

            glColor(p.edgeColor, p.alpha * p.alphaMult, false);

            for (Vector4f v : vs) {
                glVertex2f(v.x, v.y);
                glVertex2f(v.z, v.w);
            }
            glVertex2f(vs[0].x, vs[0].y);
            glVertex2f(vs[0].z, vs[0].w);

            glEnd();

            glPopMatrix();
        }

        closeGL();
    }

    private Vector4f getVertex(int index, PolygonParticle p, float divisor, float lineWidth, ViewportAPI viewport) {
        float angle = divisor * index;

        Vector2f u1 = (Vector2f) new Vector2f(0f, 1f).scale(p.size.x / viewport.getViewMult());
        Vector2f u2 = (Vector2f) new Vector2f(0f, 1f).scale((p.size.x + lineWidth) / viewport.getViewMult());

        u1 = VectorUtils.rotate(u1, angle + p.angle - 90f);
        u2 = VectorUtils.rotate(u2, angle + p.angle - 90f);

        return new Vector4f(u1.x, u1.y, u2.x, u2.y);
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CombatEngineLayers.class);
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public void addParticle(PolygonParticle p) {
        if (SHADER_MODE) {
            polygonRenderer.addParticle(p);
        } else {
            particles.add(p);
        }
    }

    public static void add(Vector2f location, PolygonParticle.PolygonParams params) {
        get().addParticle(new PolygonParticle(location, params));
    }

    public static PSE_PolygonParticlePlugin get() {
        return (PSE_PolygonParticlePlugin) Global.getCombatEngine().getCustomData().get(DATA_KEY);
    }

    public static class FollowComputeFunction extends ComputeFunction.SmoothAlphaComputeFunction {

        private final CombatEntityAPI entity;

        public FollowComputeFunction(CombatEntityAPI entity) {
            this.entity = entity;
        }

        @Override
        public void advance(float delta, BaseParticle data) {
            super.advance(delta, data);

            data.location.set(entity.getLocation());
            data.angle = entity.getFacing();
        }
    }

    private static void openGL() {
        final int w = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int h = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glViewport(0, 0, w, h);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, w, 0, h, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTranslatef(0.01f, 0.01f, 0);
    }

    private static void closeGL() {
        glDisable(GL_BLEND);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }
}
