package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.plugins.PSE_CombatEffectsPlugin;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import data.scripts.util.PSE_MiscUtils;
import data.scripts.util.PSE_SpecLoadingUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PSE_ParityGunEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float NOT_TARGETING_DAMAGE_MULT = 0.65f;
    private static final float NOT_TARGETING_SPEED_MULT = 0.6f;

    private static final String TRAIL_SPRITE_ID = "projectile_trail_charge";

    private static final Color CHARGEUP_GLOW_COLOUR = new Color(255, 234, 119, 255);
    private static final Color CHARGEUP_GLOW_COLOUR_EXTRA = new Color(255, 251, 228, 255);
    private static final float CHARGEUP_PARTICLE_DISTANCE_MIN = 30f;
    private static final float CHARGEUP_PARTICLE_DISTANCE_MAX = 80f;
    private static final float CHARGEUP_PARTICLE_VEL_MAX = 400f;
    private static final float CHARGEUP_PARTICLE_SIZE_MAX= 10f;
    private static final float CHARGEUP_PARTICLE_SIZE_MIN = 4f;
    private static final float CHARGEUP_PARTICLE_DURATION = 0.2f;

    private static final Color MUZZLE_GLOW_COLOUR = new Color(255, 230, 163, 255);
    private static final Color MUZZLE_GLOW_COLOUR_EXTRA = new Color(255, 251, 149, 255);
    private static final float MUZZLE_GLOW_SIZE = 150f;

    private static final Color DRONE_ARC_COLOUR =  new Color(255, 237, 51, 120);

    private IntervalUtil mainParticleInterval = new IntervalUtil(0.005f, 0.01f);
    private IntervalUtil bigParticleInterval = new IntervalUtil(0.05f, 0.1f);
    private IntervalUtil droneParticleInterval = new IntervalUtil(0.15f, 0.3f);

    private static final float PROJECTILE_ARC_CHANCE = 0.75f;
    private static final float PROJECTILE_ARC_DISTANCE_MIN = 30f;
    private static final float PROJECTILE_ARC_DISTANCE_MAX = 80f;

    private boolean hasFired = false;
    private boolean isTargeting = false;
    private float oldChargeLevel = 0f;
    private float oldCooldown = 0f;
    private List<DamagingProjectileAPI> projectiles = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }
        if (weapon.getShip() == null || !weapon.getShip().isAlive()) {
            return;
        }

        String key = "PSE_DroneList_" + weapon.getShip().hashCode();
        List<PSEDrone> droneList = (List<PSEDrone>) engine.getCustomData().get(key);
        String UNIQUE_SYSTEM_ID = "PSE_MVA_" + weapon.getShip().hashCode();
        PSE_DroneModularVectorAssembly droneSystem = (PSE_DroneModularVectorAssembly) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (droneSystem != null) {
            isTargeting = droneSystem.getDroneOrders().equals(PSE_DroneModularVectorAssembly.ModularVectorAssemblyDroneOrders.TARGETING);
        }

        float chargeLevel = weapon.getChargeLevel();
        float cooldown = weapon.getCooldownRemaining();

        Vector2f muzzleLocation = weapon.getLocation();

        Vector2f shipVel = weapon.getShip().getVelocity();

        if (chargeLevel > oldChargeLevel || cooldown < oldCooldown) {
            if (weapon.isFiring()) {
                if (cooldown != oldCooldown) {
                    mainParticleInterval.advance(amount * 0.2f);
                    bigParticleInterval.advance(amount * 0.2f);
                    droneParticleInterval.advance(amount * 0.2f);
                } else {
                    mainParticleInterval.advance(amount);
                    bigParticleInterval.advance(amount);
                    droneParticleInterval.advance(amount);
                }

                if (mainParticleInterval.intervalElapsed()) {
                    Vector2f loc = PSE_MiscUtils.getRandomVectorInCircleRangeWithDistanceMult(CHARGEUP_PARTICLE_DISTANCE_MAX, CHARGEUP_PARTICLE_DISTANCE_MIN, muzzleLocation, chargeLevel);

                    Vector2f vel = Vector2f.sub(muzzleLocation, loc, new Vector2f());
                    vel.normalise();
                    vel.scale(CHARGEUP_PARTICLE_VEL_MAX * chargeLevel);
                    Vector2f.add(vel, shipVel, vel);

                    float size = CHARGEUP_PARTICLE_SIZE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_SIZE_MAX - CHARGEUP_PARTICLE_SIZE_MIN));

                    if (cooldown != oldCooldown) {
                        size *= 0.5f;
                        vel.scale(0.5f);
                    }

                    float rotation = VectorUtils.getFacing(vel);

                    //engine.addSmoothParticle(loc, vel, size,10f, CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR);
                    PSE_CombatEffectsPlugin.spawnPrimitiveParticle(loc, vel, new Vector2f(), CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR, CombatEngineLayers.ABOVE_SHIPS_LAYER, size, 3, rotation, 0f, 0f, 0.2f, 0.5f);
                }
                if (bigParticleInterval.intervalElapsed()) {
                    Vector2f loc = PSE_MiscUtils.getRandomVectorInCircleRangeWithDistanceMult(CHARGEUP_PARTICLE_DISTANCE_MAX, CHARGEUP_PARTICLE_DISTANCE_MIN, muzzleLocation, chargeLevel);

                    Vector2f vel = Vector2f.sub(muzzleLocation, loc, new Vector2f());
                    vel.normalise();
                    vel.scale(0.3f * CHARGEUP_PARTICLE_VEL_MAX * chargeLevel);
                    Vector2f.add(vel, shipVel, vel);

                    float size = 1.5f * CHARGEUP_PARTICLE_SIZE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_SIZE_MAX - CHARGEUP_PARTICLE_SIZE_MIN));

                    if (cooldown != oldCooldown) {
                        size *= 0.5f;
                        vel.scale(0.5f);
                    }

                    float rotation = VectorUtils.getFacing(vel);

                    //engine.addSmoothParticle(loc, vel, size,20f, CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR_EXTRA);
                    PSE_CombatEffectsPlugin.spawnPrimitiveParticle(loc, vel, new Vector2f(), CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR_EXTRA, CombatEngineLayers.ABOVE_SHIPS_LAYER, size, 6, rotation, 0f, 0f, 0.2f, 0.5f);
                }

                if (droneParticleInterval.intervalElapsed() && isTargeting) {
                    CombatEntityAPI dummy = engine.spawnAsteroid(0, muzzleLocation.x, muzzleLocation.y, shipVel.x, shipVel.y);

                    for (PSEDrone drone : droneList) {
                        if (!drone.getLaunchingShip().equals(weapon.getShip())) {
                            continue;
                        }

                        engine.spawnEmpArc(
                                drone.getShipAPI(),
                                drone.getLocation(),
                                drone,
                                dummy,
                                DamageType.ENERGY,
                                0f,
                                0f,
                                300f,
                                null,
                                10f,
                                DRONE_ARC_COLOUR,
                                DRONE_ARC_COLOUR
                        );
                    }
                    engine.removeEntity(dummy);
                }
            }
        }

        if (!hasFired && chargeLevel >= 1f && oldChargeLevel < 1f) {
            hasFired = true;
            engine.spawnExplosion(muzzleLocation, shipVel, MUZZLE_GLOW_COLOUR, MUZZLE_GLOW_SIZE, 0.2f);
            engine.addHitParticle(muzzleLocation, shipVel, MUZZLE_GLOW_SIZE * 2f, 0.25f, 1.2f, MUZZLE_GLOW_COLOUR_EXTRA);
            engine.addSmoothParticle(muzzleLocation, shipVel, 6f, 0.3f, 1.8f, MUZZLE_GLOW_COLOUR_EXTRA);
        } else {
            hasFired = false;
        }

        //engine.maintainStatusForPlayerShip("thingiepse2", "graphics/icons/hullsys/drone_pd_high.png", "BOOSTED", isTargeting + "", true);

        for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
            if (projectile.getWeapon() != null && projectile.getWeapon().equals(weapon)) {
                if (isTargeting) {
                    if (!projectiles.contains(projectile)) {
                        projectiles.add(projectile);
                    }
                } else {
                    if (!projectiles.contains(projectile)) {
                        projectile.setDamageAmount(weapon.getDamage().getDamage() * NOT_TARGETING_DAMAGE_MULT);
                        //projectile.getVelocity().scale(NOT_TARGETING_SPEED_MULT);

                        projectiles.add(projectile);
                    }
                }

                if (Math.random() > PROJECTILE_ARC_CHANCE) {
                    Vector2f loc = PSE_MiscUtils.getRandomVectorInCircleRange(PROJECTILE_ARC_DISTANCE_MAX, PROJECTILE_ARC_DISTANCE_MIN, projectile.getLocation());
                    CombatEntityAPI dummy = engine.spawnAsteroid(0, loc.x, loc.y, projectile.getVelocity().x, projectile.getVelocity().y);

                    engine.spawnEmpArc(
                            projectile.getSource(),
                            projectile.getLocation(),
                            projectile,
                            dummy,
                            DamageType.ENERGY,
                            0f,
                            0f,
                            300f,
                            null,
                            10f,
                            DRONE_ARC_COLOUR,
                            DRONE_ARC_COLOUR
                    );

                    engine.removeEntity(dummy);
                }
            }
        }

        List<DamagingProjectileAPI> temp = new ArrayList<>();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (!engine.getProjectiles().contains(projectile)) {
                temp.add(projectile);
            }
        }
        for (DamagingProjectileAPI projectile : temp) {
            projectiles.remove(projectile);
        }

        oldChargeLevel = chargeLevel;
        oldCooldown = cooldown;
    }
}