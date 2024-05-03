package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;

public class PSE_NeuralCycler extends BaseHullMod {

    public static final float BALLISTIC_RANGE_BONUS = -10f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, BALLISTIC_RANGE_BONUS);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        float targetRange = 0f;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.MEDIUM && weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.BALLISTIC && weapon.getSlot().isHardpoint()) {
                targetRange = weapon.getRange();
            }
        }
        ship.addListener(new NeuralCyclerBaseRangeModifer(targetRange));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "medium ballistic turrets";
        if (index == 1) return "medium ballistic hardpoint";
        if (index == 2) return (int)BALLISTIC_RANGE_BONUS + "%";
        return null;
    }

    static class NeuralCyclerBaseRangeModifer implements WeaponBaseRangeModifier {

        private final float targetRange;

        public NeuralCyclerBaseRangeModifer(float targetRange) {
            this.targetRange = targetRange;
        }

        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1;
        }

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            float bonus = Math.max(targetRange - weapon.getSpec().getMaxRange(), 0);
            if (weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.BALLISTIC && weapon.getSlot().isTurret() && weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.MEDIUM) {
                return bonus;
            } else {
                return 0;
            }
        }

    }

}
