package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class PSE_RingVisual extends BaseCombatLayeredRenderingPlugin {
    private float radius;
    private final int poly;
    private final ShipAPI ship;
    private float minAlpha;
    private final float maxAlpha;
    private final Color baseColor;

    private boolean hidden = false;
    private boolean expire = false;

    private final IntervalUtil cycle;

    public PSE_RingVisual(float radius, ShipAPI ship, int poly, float minAlpha, float maxAlpha, Color baseColor, float cycleInterval) {
        this.radius = radius;
        this.baseColor = baseColor;
        this.poly = poly;
        this.ship = ship;
        this.minAlpha = minAlpha;
        this.maxAlpha = maxAlpha;
        cycle = new IntervalUtil(cycleInterval, cycleInterval);
    }

    @Override
    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        cycle.advance(amount);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (hidden || layer != CombatEngineLayers.BELOW_SHIPS_LAYER) return;

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_POLYGON_SMOOTH);
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL_TRIANGLE_STRIP);

        float rotation = ship.getFacing();

        float mult = PSE_MiscUtils.getSinAlpha(Global.getCombatEngine().getTotalElapsedTime(false), cycle.getIntervalDuration(), minAlpha, maxAlpha);
        glColor(baseColor, mult, true);

        Vector2f v = getVertex(poly, radius, rotation);
        glVertex2f(v.x, v.y);

        for (int i = 0; i < poly; i++) {
            Vector2f vertex = getVertex(i, radius, rotation);
            glVertex2f(vertex.x, vertex.y);
        }

        v = getVertex(0, radius, rotation);
        glVertex2f(v.x, v.y);
        v = getVertex(1, radius, rotation);
        glVertex2f(v.x, v.y);

        glEnd();

        glDisable(GL_BLEND);
        glPopAttrib();
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }

    public Vector2f getVertex(int index, float radius, float rotation) {
        float divisor = 360f / poly;
        float angle = divisor * index;

        Vector2f up = (Vector2f) new Vector2f(0f, 1f).scale(radius);
        up = VectorUtils.rotate(up, angle);
        up = VectorUtils.rotate(up, rotation);
        Vector2f.add(up, ship.getLocation(), up);

        return up;
    }

    @Override
    public boolean isExpired() {
        return expire && cycle.getElapsed() > cycle.getIntervalDuration();
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    public void expire() {
        expire = true;
        minAlpha = 0f;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
