package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.PSE_MiscUtils;
import data.scripts.util.PSE_SpecLoadingUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PSE_CombatWeaponsPlugin extends BaseEveryFrameCombatPlugin {
    private static final float MINI_FLAK_EXPLOSION_MAX_RADIUS = 50f;
    private static final float MINI_FLAK_EXPLOSION_DAMAGE_MAX = 80f;
    private static final float MINI_FLAK_EXPLOSION_DAMAGE_MIN = 40f;
    private static final float MINI_FLAK_FUSE_RANGE = 25f;
    private static final String MINI_FLAK_PROJECTILE_ID =  "PSE_novaburst_shot";
    private static final String MINI_FLAK_SOUND_ID = "PSE_novaburst_mini_explode";
    private static final DamageType MINI_FLAK_DAMAGE_TYPE = DamageType.FRAGMENTATION;

    private static final float HELSING_FLAK_EXPLOSION_MAX_RADIUS = 50f;
    private static final float HELSING_FLAK_EXPLOSION_DAMAGE_MAX = 50f;
    private static final float HELSING_FLAK_EXPLOSION_DAMAGE_MIN = 30f;
    private static final float HELSING_FLAK_FUSE_RANGE = 35f;
    private static final String HELSING_FLAK_PROJECTILE_ID =  "PSE_helsing_flak_rifle_shot";
    private static final String HELSING_FLAK_SOUND_ID = "PSE_helsing_flak_rifle_explode";
    private static final DamageType HELSING_FLAK_DAMAGE_TYPE = DamageType.HIGH_EXPLOSIVE;

    private static final String CASCADE_PROJECTILE_ID = "PSE_cascade_accelerator_shot";
    private static final float CASCADE_PROJECTILE_ACCELERATION = 750f;
    private static final float CASCADE_PROJECTILE_MIN_DISTANCE = 500f;

    private CombatEngineAPI engine;
    private PSE_CombatEffectsPlugin effectsPlugin;

    private void flakExplode(
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            String soundId,
            float fuseRange,
            float explosionMaxRadius,
            float explosionMinDamage,
            float explosionMaxDamage,
            DamageType damageType
    ) {
        Vector2f location = projectile.getLocation();

        engine.removeEntity(projectile);

        if (target != null) {
            Global.getSoundPlayer().playSound(
                    soundId,
                    1f,
                    1f,
                    location,
                    projectile.getVelocity()
            );

            List<ShipAPI> ships = AIUtils.getNearbyEnemies(projectile, fuseRange);
            List<CombatEntityAPI> targets = CombatUtils.getAsteroidsWithinRange(location, fuseRange);
            targets.addAll(CombatUtils.getMissilesWithinRange(location, fuseRange));

            for (ShipAPI ship : ships) {
                float damage;
                if (ship.getShield() != null && ship.getShield().isWithinArc(location)) {
                    float dist = MathUtils.getDistance(
                            PSE_MiscUtils.getNearestPointOnRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield(), location),
                            location
                    );
                    float frac = (explosionMaxRadius - dist) / explosionMaxRadius;
                    frac *= explosionMaxDamage - explosionMinDamage;
                    damage = frac + explosionMinDamage;
                } else {
                    float dist = MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds(ship, location), location);
                    float frac = (explosionMaxRadius - dist) / explosionMaxRadius;
                    frac *= explosionMaxDamage - explosionMinDamage;
                    damage = frac + explosionMinDamage;
                }

                engine.applyDamage(
                        ship,
                        location,
                        damage,
                        damageType,
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
                float frac = (explosionMaxRadius - dist) / explosionMaxRadius;
                frac *= explosionMaxDamage - explosionMinDamage;
                float damage = frac + explosionMinDamage;

                engine.applyDamage(
                        entity,
                        location,
                        damage,
                        damageType,
                        0f,
                        false,
                        false,
                        projectile.getSource(),
                        true
                );
            }
        }
    }

    private void miniFlakExplode(DamagingProjectileAPI projectile, CombatEntityAPI target) {
        Vector2f location = projectile.getLocation();

        effectsPlugin.spawnMiniStarburstFlakExplosion(location);

        flakExplode(projectile,
                target,
                MINI_FLAK_SOUND_ID,
                MINI_FLAK_FUSE_RANGE,
                MINI_FLAK_EXPLOSION_MAX_RADIUS,
                MINI_FLAK_EXPLOSION_DAMAGE_MIN,
                MINI_FLAK_EXPLOSION_DAMAGE_MAX,
                MINI_FLAK_DAMAGE_TYPE
        );
    }

    private void helsingFlakExplode(DamagingProjectileAPI projectile, CombatEntityAPI target) {
        Vector2f location = projectile.getLocation();

        effectsPlugin.spawnHelsingFlakExplosion(location);

        flakExplode(projectile,
                target,
                HELSING_FLAK_SOUND_ID,
                HELSING_FLAK_FUSE_RANGE,
                HELSING_FLAK_EXPLOSION_MAX_RADIUS,
                HELSING_FLAK_EXPLOSION_DAMAGE_MIN,
                HELSING_FLAK_EXPLOSION_DAMAGE_MAX,
                HELSING_FLAK_DAMAGE_TYPE
        );

        engine.applyDamage(
                target,
                location,
                projectile.getDamageAmount(),
                projectile.getDamageType(),
                0f,
                false,
                false,
                projectile.getSource(),
                true
        );
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        /*for (ShipAPI ship : engine.getShips()) {
            ship.setFluxVentTextureSheet("thonk");
            ship.setVentCoreColor(Color.YELLOW);
            ship.setVentFringeColor(Color.YELLOW);
        }*/

        PSE_CombatEffectsPlugin.PSE_EngineData data = (PSE_CombatEffectsPlugin.PSE_EngineData) engine.getCustomData().get(PSE_CombatEffectsPlugin.ENGINE_DATA_KEY);
        effectsPlugin = data.effectsPlugin;

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        List<DamagingProjectileAPI> remove = new ArrayList<>();
        List<CombatEntityAPI> entities;

        float dist;
        DamagingProjectileAPI proj2;
        Vector2f acc;
        for (DamagingProjectileAPI projectile : projectiles) {
            String spec = projectile.getProjectileSpecId();
            Vector2f location = projectile.getLocation();

            if (spec == null) {
                return;
            }

            switch (spec) {
                case MINI_FLAK_PROJECTILE_ID:
                    if (projectile.didDamage()) break;

                    if (projectile.isFading()) {
                        miniFlakExplode(projectile,null);
                    }

                    entities = new ArrayList<>();
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
                            Vector2f frameVel = new Vector2f(projectile.getVelocity());
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
                case HELSING_FLAK_PROJECTILE_ID:
                    if (projectile.didDamage()) break;

                    //if (projectile.isFading()) {
                    //    helsingFlakExplode(projectile,null);
                    //}

                    entities = new ArrayList<CombatEntityAPI>(CombatUtils.getShipsWithinRange(location, HELSING_FLAK_FUSE_RANGE));
                    //entities.addAll(CombatUtils.getMissilesWithinRange(location, HELSING_FLAK_FUSE_RANGE));
                    //entities.addAll(CombatUtils.getAsteroidsWithinRange(location, HELSING_FLAK_FUSE_RANGE));

                    for (CombatEntityAPI entity : entities) {
                        if (entity.equals(projectile.getSource()) || entity.getCollisionClass() == CollisionClass.NONE) continue;

                        if (entity.getOwner() == projectile.getOwner()) { //entity is friendly
                            continue;
                        }

                        if (entity.getShield() != null && entity instanceof ShipAPI && ((ShipAPI) entity).isFighter()) { //check for shield collision per frame
                            ShieldAPI shield = entity.getShield();
                            Vector2f frameVel = new Vector2f(projectile.getVelocity());
                            frameVel.normalise();
                            frameVel.scale(amount);
                            Vector2f projectedLocation = Vector2f.add(frameVel, projectile.getLocation(), new Vector2f());
                            float radius = MathUtils.getDistance(entity.getShield().getLocation(), projectedLocation);

                            if (radius <= shield.getRadius() && shield.isWithinArc(projectedLocation)) {
                                if (projectile.getOwner() != entity.getOwner()) {
                                    helsingFlakExplode(projectile, entity);
                                }
                            } else if (MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds((ShipAPI) entity, location), location) <= HELSING_FLAK_FUSE_RANGE) {
                                helsingFlakExplode(projectile, entity);
                            }
                        } else {
                            if (entity instanceof ShipAPI) {
                                ShipAPI ship = (ShipAPI) entity;

                                if (ship.isFighter() && MathUtils.getDistance(PSE_MiscUtils.getNearestPointOnShipBounds(ship, location), location) <= HELSING_FLAK_FUSE_RANGE) {
                                    helsingFlakExplode(projectile, entity);
                                }
                            }
                        }
                    }
                    break;
                case CASCADE_PROJECTILE_ID:
                    proj2 = null;
                    dist = Float.MAX_VALUE;
                    for (DamagingProjectileAPI p : engine.getProjectiles()) {
                        if (p.getProjectileSpecId() == null || !p.getProjectileSpecId().equals(CASCADE_PROJECTILE_ID) || p.equals(projectile)) continue;

                        float d = MathUtils.getDistanceSquared(projectile, p);
                        if (d < dist && d > CASCADE_PROJECTILE_MIN_DISTANCE) {
                            dist = d;
                            proj2 = p;
                        }
                    }
                    if (proj2 == null) break;

                    acc = Vector2f.sub(proj2.getLocation(), location, new Vector2f());
                    if (VectorUtils.isZeroVector(acc)) break;

                    acc.normalise();
                    acc.scale(CASCADE_PROJECTILE_ACCELERATION * amount);
                    Vector2f.add(acc, projectile.getVelocity(), projectile.getVelocity());

                    if (MathUtils.getDistance(projectile.getLocation(), projectile.getSpawnLocation()) > projectile.getWeapon().getRange()) {
                        remove.add(projectile);

                        spawnCascadeEffects(projectile);
                    }
                    break;
                default:
                    break;
            }
        }

        for (DamagingProjectileAPI p : remove) {
            engine.removeEntity(p);
        }
    }

    public static void spawnCascadeEffects(DamagingProjectileAPI projectile) {
        int n = MathUtils.getRandomNumberInRange(3, 6);
        for (int i = 0; i < n; i++) {
            Vector2f loc = MathUtils.getPointOnCircumference(projectile.getLocation(), 10f, (float) (Math.random() * 360f));

            PSE_CombatEffectsPlugin.spawnPrimitiveParticle(
                    loc,
                    projectile.getVelocity(),
                    (Vector2f) new Vector2f(projectile.getVelocity()).scale(-0.01f),
                    1f,
                    projectile.getWeapon().getMuzzleFlashSpec().getParticleColor(),
                    CombatEngineLayers.ABOVE_PARTICLES_LOWER,
                    3f,
                    3f / 1f,
                    n,
                    (float) (Math.random() * 360f),
                    60f,
                    -60f,
                    0.3f,
                    0f,
                    0.1f,
                    3f
            );
        }
    }
}