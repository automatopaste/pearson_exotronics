package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;

public class PSE_SpectrumPDBeamEffect implements BeamEffectPlugin {
    private IntervalUtil arcTracker = new IntervalUtil(0.3f, 0.4f);
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target == null) {
            return;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (ship.isFighter() && ship.isAlive() && !ship.getEngineController().isFlamedOut()) {
                arcTracker.advance(amount);

                if (arcTracker.intervalElapsed()) {
                    engine.spawnEmpArcPierceShields(
                            beam.getSource(),
                            beam.getTo(),
                            beam.getSource(),
                            target,
                            DamageType.ENERGY,
                            25,
                            200,
                            100f,
                            "PSE_spectrumpd_impact",
                            1f,
                            beam.getFringeColor(),
                            beam.getCoreColor()
                    );
                }
            }
        }
    }
}
