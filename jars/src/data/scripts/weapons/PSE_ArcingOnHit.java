package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class PSE_ArcingOnHit implements OnHitEffectPlugin {
    private static final float ARC_RANGE = 800f;
    private static final float ARC_WIDTH = 20f;
    private static final Color ARC_CORE_COLOUR = new Color(213, 170, 74,255);
    private static final Color ARC_FRINGE_COLOUR = new Color(253, 204, 161,255);

    private void spawnArc(DamagingProjectileAPI projectile, CombatEngineAPI engine, ShipAPI source, CombatEntityAPI target) {
        engine.spawnEmpArc(
                source,
                source.getLocation(),
                source,
                target,
                DamageType.ENERGY,
                projectile.getDamage().getDamage(),
                0f,
                500f,
                "tachyon_lance_emp_impact",
                ARC_WIDTH,
                ARC_FRINGE_COLOUR,
                ARC_CORE_COLOUR
        );
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        List<ShipAPI> blacklist = new LinkedList<>();
        int num = MathUtils.getRandomNumberInRange(1, 5);
        ShipAPI source = projectile.getSource();

        for (int i = 1; i <= num; i++) {
            ShipAPI fighter = null;

            List<ShipAPI> enemyShips = AIUtils.getNearbyEnemies(projectile.getSource(), ARC_RANGE);

            float tracker = Float.MAX_VALUE;
            for (ShipAPI enemyShip : enemyShips) {
                if (!enemyShip.isFighter() || blacklist.contains(enemyShip)) {
                    continue;
                }

                float distance = MathUtils.getDistance(enemyShip, projectile.getSource());
                if (distance < tracker) {
                    tracker = distance;
                    fighter = enemyShip;
                }
            }

            if (fighter == null || source == null) break;
            spawnArc(projectile, engine, source, fighter);

            blacklist.add(fighter);
            source = fighter;
        }
    }
}