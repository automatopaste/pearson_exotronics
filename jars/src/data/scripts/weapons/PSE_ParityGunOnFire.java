package data.scripts.weapons;

import cmu.drones.systems.DroneSystem;
import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.drones.vector.VectorShipsystem;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PSE_ParityGunOnFire implements OnFireEffectPlugin {

    private static final Color MUZZLE_GLOW_COLOUR = new Color(255, 230, 163, 255);
    private static final Color MUZZLE_GLOW_COLOUR_EXTRA = new Color(255, 251, 149, 255);
    private static final float MUZZLE_GLOW_SIZE = 150f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        VectorShipsystem droneSystem = (VectorShipsystem) SystemData.getDroneSystem(weapon.getShip(), engine);
        if (droneSystem == null) return;

        if (droneSystem.getDroneOrders() == VectorShipsystem.VectorOrders.RESONATOR) {
            projectile.setDamageAmount(projectile.getDamageAmount() * 2f);
        }

        Vector2f muzzleLocation = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();

        engine.spawnExplosion(muzzleLocation, shipVel, MUZZLE_GLOW_COLOUR, MUZZLE_GLOW_SIZE, 0.2f);
        engine.addHitParticle(muzzleLocation, shipVel, MUZZLE_GLOW_SIZE * 2f, 0.25f, 1.2f, MUZZLE_GLOW_COLOUR_EXTRA);
        engine.addSmoothParticle(muzzleLocation, shipVel, 6f, 0.3f, 1.8f, MUZZLE_GLOW_COLOUR_EXTRA);
    }
}
