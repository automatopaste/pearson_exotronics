package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.PSE_CombatWeaponsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PSE_CascadeAcceleratorEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final int MIN_PROJECTILES = 15;

    private final List<DamagingProjectileAPI> projectiles = new ArrayList<>();
    private final IntervalUtil tracker = new IntervalUtil(0.05f, 0.2f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) return;

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
            if (projectile == null || projectile.getWeapon() == null) continue;

            if (projectile.getWeapon().equals(weapon) && !projectiles.contains(projectile)) {
                projectiles.add(projectile);
            }
        }

        List<DamagingProjectileAPI> remove = new ArrayList<>();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (!engine.isEntityInPlay(projectile)) remove.add(projectile);
        }

        projectiles.removeAll(remove);

        if (projectiles.isEmpty() || projectiles.size() < MIN_PROJECTILES) return;

        Random r = new Random();
        DamagingProjectileAPI projectile = projectiles.get(r.nextInt(projectiles.size()));
        PSE_CombatWeaponsPlugin.spawnCascadeEffects(projectile);
        engine.removeEntity(projectile);
        projectiles.remove(projectile);
    }
}
