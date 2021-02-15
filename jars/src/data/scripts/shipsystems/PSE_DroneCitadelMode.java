package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.List;

public class PSE_DroneCitadelMode extends BaseShipSystemScript {
    private static final String WEAPON_ID = "PSE_helsing_flak_rifle";

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        if (ship.isHulk() || !ship.isAlive()) {
            return;
        }

        stats.getBallisticWeaponRangeBonus().modifyFlat(id, -20000);
        stats.getWeaponTurnRateBonus().modifyFlat(id, -20000);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        if (ship.isHulk() || !ship.isAlive()) {
            return;
        }

        ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
        stats.getWeaponTurnRateBonus().unmodify(id);
    }
}
