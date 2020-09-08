package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PSE_ParityGunEveryFrame implements EveryFrameWeaponEffectPlugin {
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

    private IntervalUtil mainParticleInterval = new IntervalUtil(0.005f, 0.01f);
    private IntervalUtil bigParticleInterval = new IntervalUtil(0.05f, 0.1f);

    private boolean hasFired = false;
    private float oldChargeLevel = 0f;
    private float oldCooldown = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float chargeLevel = weapon.getChargeLevel();
        float cooldown = weapon.getCooldownRemaining();

        Vector2f muzzleLocation = weapon.getLocation();

        if (chargeLevel > oldChargeLevel || cooldown < oldCooldown) {
            if (weapon.isFiring()) {
                if (cooldown != oldCooldown) {
                    mainParticleInterval.advance(amount * 0.2f);
                    bigParticleInterval.advance(amount * 0.2f);
                } else {
                    mainParticleInterval.advance(amount);
                    bigParticleInterval.advance(amount);
                }


                if (mainParticleInterval.intervalElapsed()) {
                    float dist = (CHARGEUP_PARTICLE_DISTANCE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_DISTANCE_MAX - CHARGEUP_PARTICLE_DISTANCE_MIN)));
                    Vector2f loc = new Vector2f(0f, dist);
                    VectorUtils.rotate(loc, (float) Math.random() * 360f);
                    Vector2f.add(loc, muzzleLocation, loc);

                    Vector2f vel = Vector2f.sub(muzzleLocation, loc, new Vector2f());
                    vel.normalise();
                    vel.scale(CHARGEUP_PARTICLE_VEL_MAX * chargeLevel);
                    Vector2f.add(vel, weapon.getShip().getVelocity(), vel);

                    float size = CHARGEUP_PARTICLE_SIZE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_SIZE_MAX - CHARGEUP_PARTICLE_SIZE_MIN));

                    if (cooldown != oldCooldown) {
                        size *= 0.5f;
                        vel.scale(0.5f);
                    }

                    engine.addSmoothParticle(loc, vel, size,10f, CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR);
                }
                if (bigParticleInterval.intervalElapsed()) {
                    float dist = (CHARGEUP_PARTICLE_DISTANCE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_DISTANCE_MAX - CHARGEUP_PARTICLE_DISTANCE_MIN)));
                    Vector2f loc = new Vector2f(0f, dist);
                    VectorUtils.rotate(loc, (float) Math.random() * 360f);
                    Vector2f.add(loc, muzzleLocation, loc);

                    Vector2f vel = Vector2f.sub(muzzleLocation, loc, new Vector2f());
                    vel.normalise();
                    vel.scale(0.3f * CHARGEUP_PARTICLE_VEL_MAX * chargeLevel);
                    Vector2f.add(vel, weapon.getShip().getVelocity(), vel);

                    float size = 1.5f * CHARGEUP_PARTICLE_SIZE_MIN + (chargeLevel * (CHARGEUP_PARTICLE_SIZE_MAX - CHARGEUP_PARTICLE_SIZE_MIN));

                    if (cooldown != oldCooldown) {
                        size *= 0.5f;
                        vel.scale(0.5f);
                    }

                    engine.addSmoothParticle(loc, vel, size,20f, CHARGEUP_PARTICLE_DURATION, CHARGEUP_GLOW_COLOUR_EXTRA);
                }
            }
        }

        if (!hasFired && chargeLevel >= 1f && oldChargeLevel < 1f) {
            hasFired = true;
            engine.spawnExplosion(muzzleLocation, weapon.getShip().getVelocity(), MUZZLE_GLOW_COLOUR, MUZZLE_GLOW_SIZE, 0.2f);
            engine.addHitParticle(muzzleLocation, weapon.getShip().getVelocity(), MUZZLE_GLOW_SIZE * 2f, 0.25f, 1.2f, MUZZLE_GLOW_COLOUR_EXTRA);
            engine.addSmoothParticle(muzzleLocation, weapon.getShip().getVelocity(), 6f, 0.3f, 1.8f, MUZZLE_GLOW_COLOUR_EXTRA);
        } else {
            hasFired = false;
        }

        oldChargeLevel = chargeLevel;
        oldCooldown = cooldown;
    }
}
