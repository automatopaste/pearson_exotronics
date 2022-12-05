package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.PSE_ExplosionEffectsPlugin;
import org.lwjgl.util.vector.Vector2f;

public class PSE_ParticleTestBeamEffect implements BeamEffectPlugin {
    private final IntervalUtil interval = new IntervalUtil(0.05f, 0.1f);
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || engine.isPaused() || beam.getWeapon().getSprite() == null) return;

//        PSE_ExplosionEffectsPlugin.beamPrimitiveParticlesAdvance(
//                interval,
//                2,
//                beam,
//                amount,
//                100f,
//                3,
//                8f,
//                750f,
//                new Vector2f(),
//                0.5f,
//                CombatEngineLayers.ABOVE_PARTICLES_LOWER,
//                4f,
//                0f,
//                720f,
//                0.4f,
//                0.75f,
//                3f
//        );
    }
}
