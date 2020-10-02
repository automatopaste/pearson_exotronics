package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.plugins.PSE_CombatEffectsPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.ArrayList;
import java.util.List;

public class PSE_MiniStarburstEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float PROXIMITY_FUSE_DISTANCE = 25f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        List<DamagingProjectileAPI> temp = new ArrayList<>();

        for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
            if (projectile.getWeapon() != null && projectile.getWeapon().equals(weapon)) {
                for (MissileAPI missile : engine.getMissiles()) {
                    if (MathUtils.getDistance(missile, projectile) <= PROXIMITY_FUSE_DISTANCE && missile.getOwner() != weapon.getShip().getOwner()) {
                        for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(projectile, PROXIMITY_FUSE_DISTANCE)) {
                            engine.applyDamage(
                                    m,
                                    projectile.getLocation(),
                                    projectile.getDamageAmount(),
                                    projectile.getDamageType(),
                                    projectile.getEmpAmount(),
                                    false,
                                    false,
                                    projectile.getSource(),
                                    true
                            );
                        }

                        PSE_CombatEffectsPlugin.spawnMiniStarburstFlakExplosion(projectile.getLocation());

                        temp.add(projectile);
                    }
                }
                for (ShipAPI ship : engine.getShips()) {
                    if (!ship.isFighter()) {
                        continue;
                    }
                    if (MathUtils.getDistance(ship, projectile) <= PROXIMITY_FUSE_DISTANCE && ship.getOwner() != weapon.getShip().getOwner()) {
                        for (ShipAPI s : AIUtils.getNearbyEnemies(projectile, PROXIMITY_FUSE_DISTANCE)) {
                            engine.applyDamage(
                                    s,
                                    projectile.getLocation(),
                                    projectile.getDamageAmount(),
                                    projectile.getDamageType(),
                                    projectile.getEmpAmount(),
                                    false,
                                    false,
                                    projectile.getSource(),
                                    true
                            );
                        }

                        PSE_CombatEffectsPlugin.spawnMiniStarburstFlakExplosion(projectile.getLocation());

                        temp.add(projectile);
                    }
                }
            }
        }

        for (DamagingProjectileAPI projectile : temp) {
            engine.removeEntity(projectile);
        }
    }
}
