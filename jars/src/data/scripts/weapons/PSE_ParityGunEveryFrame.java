package data.scripts.weapons;

import cmu.drones.systems.DroneSystem;
import cmu.drones.systems.SystemData;
import cmu.misc.MiscUtils;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.vector.VectorShipsystem;
import data.scripts.plugins.PSE_ExplosionEffectsPlugin;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PSE_ParityGunEveryFrame implements EveryFrameWeaponEffectPlugin {
//    private static final float NOT_TARGETING_DAMAGE_MULT = 0.65f;
//    private static final float NOT_TARGETING_SPEED_MULT = 0.6f;

//    private static final String TRAIL_SPRITE_ID = "projectile_trail_charge";

//    private static final Color CHARGEUP_GLOW_COLOUR = new Color(119, 255, 185, 255);
//    private static final Color CHARGEUP_GLOW_COLOUR_EXTRA = new Color(228, 255, 233, 255);
//    private static final float CHARGEUP_PARTICLE_DISTANCE_MIN = 30f;
//    private static final float CHARGEUP_PARTICLE_DISTANCE_MAX = 80f;
//    private static final float CHARGEUP_PARTICLE_VEL_MAX = 400f;
//    private static final float CHARGEUP_PARTICLE_SIZE_MAX= 10f;
//    private static final float CHARGEUP_PARTICLE_SIZE_MIN = 4f;
//    private static final float CHARGEUP_PARTICLE_DURATION = 0.2f;

    private static final Color DRONE_ARC_COLOUR =  new Color(148, 255, 211, 120);

    private final IntervalUtil droneParticleInterval = new IntervalUtil(0.15f, 0.3f);
    private final IntervalUtil arcInterval = new IntervalUtil(0.05f, 0.1f);

    private static final float PROJECTILE_ARC_CHANCE = 2f;
    private static final float PROJECTILE_ARC_DISTANCE_MIN = 30f;
    private static final float PROJECTILE_ARC_DISTANCE_MAX = 80f;

    private final List<DamagingProjectileAPI> projectiles = new ArrayList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }
        if (weapon.getShip() == null || !weapon.getShip().isAlive()) {
            return;
        }

        VectorShipsystem droneSystem = (VectorShipsystem) SystemData.getDroneSystem(weapon.getShip(), engine);
        if (droneSystem == null) return;

        List<ShipAPI> droneList = new ArrayList<>(droneSystem.getForgeTracker().getDeployed());

        Vector2f muzzleLocation = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();

        droneParticleInterval.advance(amount);
        if (droneSystem.getDroneOrders() == VectorShipsystem.VectorOrders.RESONATOR) {
            if (droneParticleInterval.intervalElapsed()) {
                CombatEntityAPI dummy = engine.spawnAsteroid(0, muzzleLocation.x, muzzleLocation.y, shipVel.x, shipVel.y);

                for (ShipAPI drone : droneList) {
                    engine.spawnEmpArc(
                            drone,
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

        for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
            if (projectile.getWeapon() != null && projectile.getWeapon().equals(weapon)) {
                if (!projectiles.contains(projectile)) {
                    projectiles.add(projectile);
                }

                float a = amount;
                if (droneSystem.getDroneOrders() == VectorShipsystem.VectorOrders.RESONATOR && !droneSystem.getForgeTracker().getDeployed().isEmpty()) {
                    a *= 2f;
                }
                arcInterval.advance(a);

                if (arcInterval.intervalElapsed()) {
                    Vector2f loc = MiscUtils.getRandomVectorInCircleRange(PROJECTILE_ARC_DISTANCE_MAX, PROJECTILE_ARC_DISTANCE_MIN, projectile.getLocation());
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
    }
}