package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class PSE_HelsingFlakRifleOnHit implements OnHitEffectPlugin {
    private static final float DAMAGE_AMOUNT = 50f;
    private static final DamageType DAMAGE_TYPE = DamageType.HIGH_EXPLOSIVE;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!shieldHit && target instanceof ShipAPI && ((ShipAPI) target).isFighter()) {
            engine.applyDamage(target, point, DAMAGE_AMOUNT, DAMAGE_TYPE, 0f, false, true, projectile.getSource());
        }
    }
}