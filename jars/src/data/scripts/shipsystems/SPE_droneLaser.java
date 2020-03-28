package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.Iterator;
import java.util.List;

public class SPE_droneLaser extends BaseShipSystemScript {
    private String ACTIVE_WEAPON_ID = "hil";
    private String DEFAULT_WEAPON_ID = "pdburst";

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
                if (weapon.getId().equals(DEFAULT_WEAPON_ID)) {
                    weapon.disable(true);
                }
                if (weapon.getId().equals(ACTIVE_WEAPON_ID)) {
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
                if (weapon.getId().equals(DEFAULT_WEAPON_ID)) {
                    weapon.repair();
                }
                if (weapon.getId().equals(ACTIVE_WEAPON_ID)) {
                    weapon.disable(true);
                }
            }
        }
    }
}
