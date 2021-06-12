package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public class PSE_HardluxBeamEffect implements BeamEffectPlugin {
    private static final float KINETIC_PERCENT_BONUS = 0.15f;
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        ShipAPI ship = beam.getSource();
        if (ship == null) return;

        if (!ship.hasListenerOfClass(HardluxBeamEffectDamageDealtMod.class)) {
            ship.addListener(new HardluxBeamEffectDamageDealtMod(ship));
        }
    }

    public static class HardluxBeamEffectDamageDealtMod implements DamageDealtModifier {
        protected ShipAPI ship;

        public HardluxBeamEffectDamageDealtMod(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(
                Object param,
                CombatEntityAPI target,
                DamageAPI damage,
                Vector2f point,
                boolean shieldHit
        ) {
            if (!(param instanceof DamagingProjectileAPI) && param instanceof BeamAPI) {
                if (shieldHit) {
                    Global.getCombatEngine().applyDamage(
                            target,
                            point,
                            damage.getBaseDamage() * KINETIC_PERCENT_BONUS,
                            DamageType.KINETIC,
                            0f,
                            false,
                            false,
                            ship
                    );
                }

                BeamAPI beam = (BeamAPI) param;

                if (beam.getWeapon().getSpec().getWeaponId().equals("PSE_hardlux")) damage.setForceHardFlux(true);
            }
            return null;
        }
    }
}
