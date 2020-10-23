package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PSE_CombatFighterLaunchPlugin extends BaseEveryFrameCombatPlugin {
    private List<ShipAPI> launching = new ArrayList<>();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Random r = new Random();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getNumFighterBays() <= 0) continue;

            if (ship.getVariant().hasHullMod("PSE_ConvertedDroneHangars")) {
                List<WeaponSlotAPI> hangars = new ArrayList<>();
                for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (slot.getWeaponType().equals(WeaponAPI.WeaponType.LAUNCH_BAY)) hangars.add(slot);
                }

                Vector2f launch;
                if (hangars.isEmpty()) {
                    launch = ship.getLocation();
                } else {
                    launch = hangars.get(r.nextInt(hangars.size())).computePosition(ship);
                }

                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    FighterWingAPI wing = bay.getWing();
                    if (wing == null) continue;

                    for (ShipAPI fighter : wing.getWingMembers()) {
                        if (!launching.contains(fighter) && fighter.isLiftingOff()) {
                            fighter.getLocation().set(launch);

                            launching.add(fighter);
                        }
                    }
                }
            }
        }

        List<ShipAPI> tmp = new ArrayList<>();
        for (ShipAPI fighter : launching) {
            if (fighter == null || !engine.isEntityInPlay(fighter) || !fighter.isLiftingOff()) {
                tmp.add(fighter);
            }
        }
        launching.removeAll(tmp);
    }
}
