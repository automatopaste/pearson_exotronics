package data.scripts.plugins;

import cmu.CMUtils;
import cmu.plugins.renderers.ImplosionParticleRenderer;
import cmu.shaders.particles.BaseParticle;
import cmu.shaders.particles.ComputeFunction;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.coreui.V;
import com.fs.starfarer.ui.P;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class HaloRenderer implements CombatLayeredRenderingPlugin {

    private final ShipAPI ship;
    private final IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);
    private final WeightedRandomPicker<Pair<Vector2f, Vector2f>> picker;
    private final Random random = new Random(69);

    private ImplosionParticleRenderer renderer;

    public HaloRenderer(ShipAPI ship) {
        this.ship = ship;

        picker = new WeightedRandomPicker<>();

        for (BoundsAPI.SegmentAPI segment : ship.getExactBounds().getSegments()) {
            Vector2f v = Vector2f.sub(segment.getP1(), segment.getP2(), new Vector2f());
            float length = v.length();
            Pair<Vector2f, Vector2f> pair = new Pair<>(segment.getP1(), segment.getP2());

            picker.add(pair, length);
        }

        renderer = (ImplosionParticleRenderer) CMUtils.initBuiltinParticleRenderer(CMUtils.BuiltinParticleRenderers.IMPLOSION, CombatEngineLayers.BELOW_SHIPS_LAYER);
    }

    @Override
    public void init(CombatEntityAPI entity) {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isExpired() {
        return !ship.isAlive() || ship.isHulk();
    }

    @Override
    public void advance(float amount) {
        //interval.advance(amount);
        if (interval.intervalElapsed()) {
            for (int i = 0; i < 11; i++) {
                Pair<Vector2f, Vector2f> p = picker.pick(random);

                Vector2f d = Vector2f.sub(p.one, p.two, new Vector2f());
                d.scale(random.nextFloat());
                Vector2f v = Vector2f.add(d, p.two, new Vector2f());

                VectorUtils.rotate(v, ship.getFacing());

                Vector2f.add(v, ship.getLocation(), v);

                BaseParticle.ParticleParams params = new BaseParticle.ParticleParams();
                params.lifetime = 1f;
                params.color = new Color(117, 255, 177, 255);
                params.sizeInit = new Vector2f(50f, 50f);
                params.sizeFinal = new Vector2f(0f, 0f);
                Vector2f vel = VectorUtils.rotate(new Vector2f(100f, 0f), VectorUtils.getFacing(d));
                params.vel = vel;
                params.acc = new Vector2f(-vel.x, -vel.y);
                params.computeFunction = new ComputeFunction.SmoothAlphaComputeFunction();
                params.angVel = 6f;

                renderer.addParticle(v, params);
            }
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {

    }
}
