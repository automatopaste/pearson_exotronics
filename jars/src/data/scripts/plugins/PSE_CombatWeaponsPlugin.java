package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.PSE_MiscUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class PSE_CombatWeaponsPlugin extends BaseEveryFrameCombatPlugin {
    private static final float MINI_FLAK_EXPLOSION_MAX_RADIUS = 50f;
    private static final float MINI_FLAK_EXPLOSION_DAMAGE_MAX = 80f;
    private static final float MINI_FLAK_EXPLOSION_DAMAGE_MIN = 40f;
    private static final float MINI_FLAK_FUSE_RANGE = 35f;
    private static final String MINI_FLAK_PROJECTILE_ID =  "PSE_starburst_shot";
    private static final String MINI_FLAK_SOUND_ID = "PSE_starburst_mini_explode";

    private CombatEngineAPI engine;
    private PSE_CombatEffectsPlugin effectsPlugin;


    private void miniFlakExplode(DamagingProjectileAPI projectile, CombatEntityAPI target) {
        Vector2f location = projectile.getLocation();

        engine.removeEntity(projectile);

        if (target == null) {
            effectsPlugin.spawnMiniStarburstFlakExplosion(location);
        } else {

            effectsPlugin.spawnMiniStarburstFlakExplosion(location);

            Global.getSoundPlayer().playSound(
                    MINI_FLAK_SOUND_ID,
                    1f,
                    1f,
                    location,
                    projectile.getVelocity()
            );

            List<ShipAPI> ships = AIUtils.getNearbyEnemies(projectile, MINI_FLAK_FUSE_RANGE);
            List<CombatEntityAPI> targets = CombatUtils.getAsteroidsWithinRange(location, MINI_FLAK_FUSE_RANGE);
            targets.addAll(CombatUtils.getMissilesWithinRange(location, MINI_FLAK_FUSE_RANGE));

            for (ShipAPI ship : ships) {
                float damage;
                if (ship.getShield() != null && ship.getShield().isWithinArc(location)) {
                    float dist = MathUtils.getDistance(
                            PSE_MiscUtils.getNearestPointOnRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield(), location),
                            location
                    );
                    float frac = (MINI_FLAK_EXPLOSION_MAX_RADIUS - dist) / MINI_FLAK_EXPLOSION_MAX_RADIUS;
                    frac *= MINI_FLAK_EXPLOSION_DAMAGE_MAX - MINI_FLAK_EXPLOSION_DAMAGE_MIN;
                    damage = frac + MINI_FLAK_EXPLOSION_DAMAGE_MIN;
                } else {
                    float dist = MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds(ship, location), location);
                    float frac = (MINI_FLAK_EXPLOSION_MAX_RADIUS - dist) / MINI_FLAK_EXPLOSION_MAX_RADIUS;
                    frac *= MINI_FLAK_EXPLOSION_DAMAGE_MAX - MINI_FLAK_EXPLOSION_DAMAGE_MIN;
                    damage = frac + MINI_FLAK_EXPLOSION_DAMAGE_MIN;
                }

                engine.applyDamage(
                        ship,
                        location,
                        damage,
                        DamageType.FRAGMENTATION,
                        0f,
                        false,
                        false,
                        projectile.getSource(),
                        true
                );
            }
            for (CombatEntityAPI entity : targets) {
                float dist = MathUtils.getDistance(
                        PSE_MiscUtils.getNearestPointOnCollisionRadius(entity, location),
                        location
                );
                float frac = (MINI_FLAK_EXPLOSION_MAX_RADIUS - dist) / MINI_FLAK_EXPLOSION_MAX_RADIUS;
                frac *= MINI_FLAK_EXPLOSION_DAMAGE_MAX - MINI_FLAK_EXPLOSION_DAMAGE_MIN;
                float damage = frac + MINI_FLAK_EXPLOSION_DAMAGE_MIN;

                engine.applyDamage(
                        entity,
                        location,
                        damage,
                        DamageType.FRAGMENTATION,
                        0f,
                        false,
                        false,
                        projectile.getSource(),
                        true
                );
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        PSE_CombatEffectsPlugin.PSE_EngineData data = (PSE_CombatEffectsPlugin.PSE_EngineData) engine.getCustomData().get(PSE_CombatEffectsPlugin.ENGINE_DATA_KEY);
        effectsPlugin = data.effectsPlugin;

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();

        for (DamagingProjectileAPI projectile : projectiles) {
            String spec = projectile.getProjectileSpecId();
            Vector2f location = projectile.getLocation();

            switch (spec) {
                case MINI_FLAK_PROJECTILE_ID:
                    if (projectile.didDamage()) break;

                    if (projectile.isFading()) {
                        miniFlakExplode(projectile,null);
                    }

                    List<CombatEntityAPI> entities = new ArrayList<>();
                    entities.addAll(CombatUtils.getShipsWithinRange(location, MINI_FLAK_FUSE_RANGE));
                    entities.addAll(CombatUtils.getMissilesWithinRange(location, MINI_FLAK_FUSE_RANGE));
                    entities.addAll(CombatUtils.getAsteroidsWithinRange(location, MINI_FLAK_FUSE_RANGE));

                    for (CombatEntityAPI entity : entities) {
                        if (entity.equals(projectile.getSource()) || entity.getCollisionClass() == CollisionClass.NONE) continue;

                        if (entity.getOwner() == projectile.getOwner()) { //entity is friendly
                            continue;
                        }

                        if (entity.getShield() != null) { //check for shield collision per frame
                            ShieldAPI shield = entity.getShield();
                            Vector2f frameVel = projectile.getVelocity();
                            frameVel.normalise();
                            frameVel.scale(amount);
                            Vector2f projectedLocation = Vector2f.add(frameVel, projectile.getLocation(), new Vector2f());
                            float radius = MathUtils.getDistance(entity.getShield().getLocation(), projectedLocation);

                            if (radius <= shield.getRadius() && shield.isWithinArc(projectedLocation)) {
                                if (projectile.getOwner() != entity.getOwner()) {
                                    miniFlakExplode(projectile, entity);
                                }
                            } else if (MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds((ShipAPI) entity, location), location) <= MINI_FLAK_FUSE_RANGE) {
                                miniFlakExplode(projectile, entity);
                            }
                        } else {
                            if (entity instanceof ShipAPI) {
                                ShipAPI ship = (ShipAPI) entity;

                                if (MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds(ship, location), location) <= MINI_FLAK_FUSE_RANGE) {
                                    miniFlakExplode(projectile, entity);
                                }
                            } else {
                                Vector2f collisionPoint = PSE_MiscUtils.getNearestPointOnCollisionRadius(entity, location);
                                if (MathUtils.getDistance(collisionPoint, location) <= MINI_FLAK_FUSE_RANGE) {
                                    miniFlakExplode(projectile, entity);
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
