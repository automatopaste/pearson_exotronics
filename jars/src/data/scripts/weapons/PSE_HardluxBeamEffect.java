package data.scripts.weapons;

import cmu.shaders.particles.PolygonParticle;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.PSE_PolygonParticlePlugin;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PSE_HardluxBeamEffect implements BeamEffectPlugin {
    private static final float KINETIC_PERCENT_BONUS = 0.15f;

    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        ShipAPI ship = beam.getSource();
        if (ship == null) return;

        if (!ship.hasListenerOfClass(HardluxBeamEffectDamageDealtMod.class)) {
            ship.addListener(new HardluxBeamEffectDamageDealtMod(ship));
        }

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            Vector2f l = beam.getFrom();
            Vector2f to = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());

            PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(
                    3,
                    CombatEngineLayers.UNDER_SHIPS_LAYER,
                    new Color(223, 255, 223, 242),
                    0.2f
            );

            params.sizeInit = new Vector2f(6f, 8f);
            params.sizeFinal = new Vector2f(0f, 0f);

            params.color = new Color(11, 23, 11, 213);

            params.angVel = 1000f;
            params.lifetime = 0.3f;

            for (int i = 0; i < 7; i++) {
                Vector2f t = new Vector2f(to);
                t.scale((float) Math.random());
                Vector2f.add(l, t, t);

                Vector2f v = new Vector2f(50f, 0f);
                VectorUtils.rotate(v, (float) Math.random() * 360f);

                params.vel = v;

                PolygonParticle p = new PolygonParticle(t, params);
                PSE_PolygonParticlePlugin.get().addParticle(p);
            }
        }
    }

    public static class HardluxBeamEffectDamageDealtMod implements DamageDealtModifier {
        protected ShipAPI ship;

        public HardluxBeamEffectDamageDealtMod(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(
                Object param,
                CombatEntityAPI target,
                DamageAPI damage,
                Vector2f point,
                boolean shieldHit
        ) {
            if (param instanceof BeamAPI) {
                BeamAPI beam = (BeamAPI) param;

                if (beam.getWeapon().getSpec().getWeaponId().equals("PSE_hardlux")) {
                    if (shieldHit) {
                        Global.getCombatEngine().applyDamage(
                                target,
                                point,
                                damage.getBaseDamage() * KINETIC_PERCENT_BONUS,
                                DamageType.KINETIC,
                                0f,
                                false,
                                false,
                                ship
                        );

                        damage.setForceHardFlux(true);
                    }
                }
            }
            return null;
        }
    }
}
