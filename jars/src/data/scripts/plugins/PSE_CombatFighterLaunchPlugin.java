package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.ai.PSE_FighterLandingAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class PSE_CombatFighterLaunchPlugin extends BaseEveryFrameCombatPlugin {
    private List<ShipAPI> launching = new ArrayList<>();
    //private Map<ShipAPI, Vector2f> landing = new HashMap<>(); //wip functionality to manually land fighters (weird nullpointer crashes from switching fighter AI???)

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        Random r = new Random();

        for (ShipAPI ship : engine.getShips()) {
            /*if (ship.isFighter() && ship.isLanding() && !ship.isFinishedLanding()) {
                ShipAPI carrier = null;

                if (!landing.containsKey(ship)) {
                    Vector2f landingLocation = null;

                    for (ShipAPI near : AIUtils.getNearbyAllies(ship, 350f)) {
                        if (!(near.getVariant().hasHullMod("PSE_ConvertedDroneHangars") || near.getVariant().hasHullMod("converted_hangar"))) {
                            continue;
                        }

                        List<WeaponSlotAPI> hangars = new ArrayList<>();

                        for (WeaponSlotAPI slot : near.getHullSpec().getAllWeaponSlotsCopy()) {
                            if (slot.getWeaponType().equals(WeaponAPI.WeaponType.LAUNCH_BAY)) hangars.add(slot);
                        }

                        if (hangars.isEmpty()) {
                            continue;
                        } else {
                            landingLocation = hangars.get(r.nextInt(hangars.size())).computePosition(near);
                        }

                        carrier = near;
                    }

                    landing.put(ship, landingLocation);

                    ship.setShipAI(new PSE_FighterLandingAI(landingLocation, ship));
                } else {
                    for (ShipAPI near : AIUtils.getNearbyAllies(ship, 350f)) {
                        if (!(near.getVariant().hasHullMod("PSE_ConvertedDroneHangars") || near.getVariant().hasHullMod("converted_hangar"))) {
                            continue;
                        }

                        List<WeaponSlotAPI> hangars = new ArrayList<>();

                        for (WeaponSlotAPI slot : near.getHullSpec().getAllWeaponSlotsCopy()) {
                            if (slot.getWeaponType().equals(WeaponAPI.WeaponType.LAUNCH_BAY)) hangars.add(slot);
                        }

                        if (!hangars.isEmpty()) {
                            carrier = near;
                        }
                    }
                }

                if (carrier != null) {
                    rotateShipToFacing(ship, carrier.getFacing());
                }
            }*/

            if (ship.getNumFighterBays() > 0 && (ship.getVariant().hasHullMod("PSE_ConvertedDroneHangars") || ship.getVariant().hasHullMod("converted_hangar"))) {
                List<WeaponSlotAPI> hangars = new ArrayList<>();

                for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (slot.getWeaponType().equals(WeaponAPI.WeaponType.LAUNCH_BAY)) hangars.add(slot);
                }

                Vector2f launchLocation;
                if (hangars.isEmpty()) {
                    launchLocation = ship.getLocation();
                } else {
                    launchLocation = hangars.get(r.nextInt(hangars.size())).computePosition(ship);
                }

                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    FighterWingAPI wing = bay.getWing();
                    if (wing == null) continue;

                    for (ShipAPI fighter : wing.getWingMembers()) {
                        if (fighter == null) continue;

                        if (!launching.contains(fighter) && fighter.isLiftingOff()) {
                            fighter.getLocation().set(launchLocation);

                            launching.add(fighter);
                        }
                    }
                }
            }
        }

        List<ShipAPI> tmp1 = new ArrayList<>();
        for (ShipAPI fighter : launching) {
            if (fighter == null || !engine.isEntityInPlay(fighter) || !fighter.isLiftingOff()) {
                tmp1.add(fighter);
            }
        }
        launching.removeAll(tmp1);

        /*List<ShipAPI> tmp2 = new ArrayList<>();
        for (ShipAPI fighter : landing.keySet()) {
            if (fighter == null) continue;

            if (fighter.isFinishedLanding()) fighter.resetDefaultAI();

            if (!engine.isEntityInPlay(fighter) || !fighter.isLanding() || fighter.isFinishedLanding()) {
                tmp2.add(fighter);
            }
        }
        for (ShipAPI fighter : tmp2) {
            landing.remove(fighter);
        }*/
    }

    //method from drone movement algorithms
    public static void rotateShipToFacing(ShipAPI fighter, float absoluteFacingTargetAngle) {
        float delta = MathUtils.getShortestRotation(fighter.getFacing(), absoluteFacingTargetAngle);
        fighter.setFacing(fighter.getFacing() + delta * 0.5f);
    }
}
