package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.List;

public class PSE_DroneLaser extends BaseShipSystemScript {
    static final String FOCUS_WEAPON_ID = "hil";
    static final String PD_WEAPON_ID = "pdlaser";

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons != null) {
            for (WeaponAPI weapon : weapons) {
                if (weapon.getId().equals(PD_WEAPON_ID)) {
                    weapon.disable(true);
                }
                if (weapon.getId().equals(FOCUS_WEAPON_ID)) {
                    weapon.repair();
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (weapons != null) {
            for (WeaponAPI weapon : weapons) {
                if (weapon.getId().equals(PD_WEAPON_ID)) {
                    weapon.repair();
                }
                if (weapon.getId().equals(FOCUS_WEAPON_ID)) {
                    weapon.disable(true);
                }
            }
        }
    }
}
