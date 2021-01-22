package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.LinkedList;
import java.util.List;

public class PSE_ArcingPDBeamEffectPlugin implements BeamEffectPlugin {
    private static final float MIN_INTERVAL = 0.05f;
    private static final float MAX_INTERVAL = 0.1f;
    private static final float ARC_RANGE = 800f;

    IntervalUtil tracker = new IntervalUtil(MIN_INTERVAL, MAX_INTERVAL);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        tracker.advance(amount);

        if (tracker.intervalElapsed() && beam.didDamageThisFrame() && beam.getDamageTarget() != null && beam.getDamageTarget() instanceof ShipAPI) {
            //spawnArc(beam, engine, beam.getSource(), beam.getDamageTarget());

            List<ShipAPI> blacklist = new LinkedList<>();
            int num = MathUtils.getRandomNumberInRange(1, 5);
            ShipAPI source = beam.getSource();

            for (int i = 1; i <= num; i++) {
                ShipAPI fighter = null;

                List<ShipAPI> enemyShips = AIUtils.getNearbyEnemies(beam.getSource(), ARC_RANGE);

                float tracker = Float.MAX_VALUE;
                for (ShipAPI enemyShip : enemyShips) {
                    if (!enemyShip.isFighter() || blacklist.contains(enemyShip)) {
                        continue;
                    }

                    float distance = MathUtils.getDistance(enemyShip, beam.getSource());
                    if (distance < tracker) {
                        tracker = distance;
                        fighter = enemyShip;
                    }
                }

                if (fighter == null || source == null) break;
                spawnArc(beam, engine, source, fighter);

                blacklist.add(fighter);
                source = fighter;
            }
        }
    }

    private void spawnArc(BeamAPI beam, CombatEngineAPI engine, ShipAPI source, CombatEntityAPI target) {
        engine.spawnEmpArc(
                source,
                source.getLocation(),
                source,
                target,
                DamageType.ENERGY,
                beam.getDamage().getDamage(),
                0f,
                500f,
                "tachyon_lance_emp_impact",
                beam.getWidth(),
                beam.getFringeColor(),
                beam.getCoreColor()
        );
    }
}