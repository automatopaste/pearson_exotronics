package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

import static org.lwjgl.opengl.GL11.*;

public class FieldRenderer extends BaseCombatLayeredRenderingPlugin {

    private final SpriteAPI tex;
    private final ShipAPI mothership;
    private float fieldAngle;
    private final float arc;
    private final float minRadius;
    private final float maxRadius;

    float t;
    float speed = -0.1f;

    float alpha = 0f; // starts invisible, call brighter() to start rendering with a fadein

    public FieldRenderer(SpriteAPI tex, ShipAPI mothership, float arc, float maxRadius) {
        this.tex = tex;
        this.mothership = mothership;

        this.arc = arc;

        minRadius = Math.max(mothership.getShieldRadiusEvenIfNoShield() - 50f, 0f);
        this.maxRadius = maxRadius;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (alpha == 0f) return;

        t += speed * Global.getCombatEngine().getElapsedInLastFrame();

        Vector2f loc = mothership.getLocation();

        glPushMatrix();
        glTranslatef(loc.x, loc.y, 0);

        glEnable(GL_TEXTURE_2D);
        tex.bindTexture();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Color c = new Color(0, 255, 89, 137);
        glColor4ub((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)(int)(c.getAlpha() * alpha));

        final int numVertices = 12;
        final float divisor = arc / numVertices;

        float midRange = 0.3f;
        float d = midRange * (maxRadius - minRadius) + minRadius;

        float angle;
        Vector2f v;

        glBegin(GL_TRIANGLE_STRIP);
        for (int i = 0; i < numVertices + 1; i++) {
            angle = fieldAngle - (arc * 0.5f) + (i * divisor);

            float s = (float)i / numVertices;

            v = VectorUtils.rotate(new Vector2f(minRadius, 0f), angle);
            glTexCoord2f(t, s);
            glColor4ub((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)0);
            glVertex2f(v.x, v.y);

            v = VectorUtils.rotate(new Vector2f(d, 0f), angle);
            glTexCoord2f(t + midRange, s);
            glColor4ub((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)(c.getAlpha() * alpha));
            glVertex2f(v.x, v.y);
        }
        glEnd();

        glBegin(GL_TRIANGLE_STRIP);
        for (int i = 0; i < numVertices + 1; i++) {
            angle = fieldAngle - (arc * 0.5f) + (i * divisor);

            float s = (float)i / numVertices;

            v = VectorUtils.rotate(new Vector2f(d, 0f), angle);
            glTexCoord2f(t + midRange, s);
            glColor4ub((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)(c.getAlpha() * alpha));
            glVertex2f(v.x, v.y);

            v = VectorUtils.rotate(new Vector2f(maxRadius, 0f), angle);
            glTexCoord2f(t + 1f, s);
            glColor4ub((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue(), (byte)0);
            glVertex2f(v.x, v.y);
        }
        glEnd();

        glPopMatrix();
    }

    public void brighter(float speed) {
        alpha = Math.min(alpha + Global.getCombatEngine().getElapsedInLastFrame() * speed, 1f);
    }

    public void darker(float speed) {
        alpha = Math.max(alpha - Global.getCombatEngine().getElapsedInLastFrame() * speed, 0f);
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }

    @Override
    public boolean isExpired() {
        return !mothership.isAlive();
    }

    public void setFieldAngle(float fieldAngle) {
        this.fieldAngle = fieldAngle;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}