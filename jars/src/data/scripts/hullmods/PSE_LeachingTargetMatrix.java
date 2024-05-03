package data.scripts.hullmods;

import cmu.drones.systems.DroneSystem;
import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.PSE_RingVisual;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PSE_LeachingTargetMatrix extends BaseHullMod {
    private static final Color RING_COLOUR = new Color(191, 253, 225, 255);
    //private static final Color HIGHLIGHT_COLOUR = Global.getSettings().getColor("hColor");
    private static final Color HIGHLIGHT_COLOUR = new Color(0, 255, 85, 255);
    private static final Color DEBUFF_COLOUR = new Color(255, 115, 0, 255);
    private static final String bullet = "  - ";

    private final IntervalUtil tracker = new IntervalUtil(1f, 1f);

    private PSE_RingVisual visual = null;
    private float radius = 0f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        if (ship == null || !ship.isAlive() || ship.isHulk()) {
            if (visual != null) {
                visual.expire();
                visual = null;
            }
            return;
        }

        if (engine.getPlayerShip().equals(ship)) {
            if (visual == null) {
                visual = new PSE_RingVisual(
                        radius,
                        ship,
                        48,
                        0.1f,
                        0.2f,
                        RING_COLOUR,
                        4f
                );

                engine.addLayeredRenderingPlugin(visual);
            }
        } else {
            if (visual != null) {
                visual.expire();
                visual = null;
            }
        }

        if (!tracker.intervalElapsed()) return;

        List<ShipAPI> drones = new ArrayList<>();

        for (Map<ShipAPI, DroneSystem> m : SystemData.getDroneSystems(engine).values()) {
            for (DroneSystem d : m.values()) {
                drones.addAll(d.getForgeTracker().getDeployed());
            }
        }

        ShipAPI target = null;
        for (ShipAPI drone : drones) {
            drone.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f));
            drone.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f));
            drone.getMutableStats().getBeamWeaponRangeBonus().modifyFlat(this.getClass().toString(), ship.getMutableStats().getBeamWeaponRangeBonus().computeEffective(1f));

            if (!drone.getUsableWeapons().isEmpty()) target = drone;
        }

        int count = 0;
        radius = 0f;
        if (target != null) {
            for (WeaponAPI weapon : target.getUsableWeapons()) {
                radius += weapon.getRange();
                count++;
            }
        }
        if (count != 0) {
            radius /= count;
        }

        if (radius == 0f) {
            if (visual != null) {
                visual.expire();
                visual = null;
            }
        } else {
            visual.setRadius(radius);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        float ballisticRangeMult = ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f) * 100f;
        float energyRangeMult = ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f) * 100f;
        float beamRangeBonus = ship.getMutableStats().getBeamWeaponRangeBonus().computeEffective(0f);

        String ballisticDesc;
        String ballisticAmount;
        if (ballisticRangeMult >= 100f) {
            ballisticAmount = (int) (ballisticRangeMult - 100f) + "%";
            ballisticDesc = "increased";
        } else {
            ballisticAmount = (int) (100f - ballisticRangeMult) + "";
            ballisticDesc = "decreased";
        }

        String energyDesc;
        String energyAmount;
        if (energyRangeMult >= 100f) {
            energyAmount = (int) (energyRangeMult - 100f) + "%";
            energyDesc = "increased";
        } else {
            energyAmount = (int) (100f - energyRangeMult) + "";
            energyDesc = "decreased";
        }

        String beamDesc;
        String beamAmount;
        if (beamRangeBonus >= 0f) {
            beamAmount = (int) beamRangeBonus + "";
            beamDesc = "increased";
        } else {
            beamAmount = (int) beamRangeBonus + "";
            beamDesc = "decreased";
        }

        if (index == 0) return ballisticDesc;
        if (index == 1) return ballisticAmount;
        if (index == 2) return energyDesc;
        if (index == 3) return energyAmount;
        if (index == 4) return beamDesc;
        if (index == 5) return beamAmount;
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        MutableShipStatsAPI stats = ship.getMutableStats();

        checkAddRangeStatBullet(tooltip, (int) (stats.getBallisticWeaponRangeBonus().computeEffective(1f) * 100f - 100f), "Ballistic range ", "%%", true);
        checkAddRangeStatBullet(tooltip, (int) (stats.getEnergyWeaponRangeBonus().computeEffective(1f) * 100f - 100f), "Energy range ", "%%", true);
        checkAddRangeStatBullet(tooltip, (int) (stats.getMissileWeaponRangeBonus().computeEffective(1f) * 100f - 100f), "Missile range ", "%%", true);
        checkAddRangeStatBullet(tooltip, (int) stats.getBeamWeaponRangeBonus().computeEffective(0f), "Beam range ", "", false);
        checkAddRangeStatBullet(tooltip, (int) stats.getBeamPDWeaponRangeBonus().computeEffective(0f), "PD beam range ", "", false);
        checkAddRangeStatBullet(tooltip, (int) stats.getNonBeamPDWeaponRangeBonus().computeEffective(0f), "Non-PD beam range ", "", false);

        MutableStat stat = stats.getWeaponRangeThreshold();
        if (!stat.isUnmodified()) {
            int amount = stat.getModifiedInt();
            tooltip.addPara(bullet + "Weapon ranges normalised around " + amount, 3f, HIGHLIGHT_COLOUR, amount + "");
        }
        stat = stats.getWeaponRangeMultPastThreshold();
        if (!stat.isUnmodified()) {
            float amount = stat.getModifiedValue();
            tooltip.addPara(bullet + "Weapon ranges normalised with a factor of " + "x" + amount, 3f, HIGHLIGHT_COLOUR, "x" + amount + "");
        }
    }

    private void checkAddRangeStatBullet(TooltipMakerAPI tooltip, int amount, String desc, String post, boolean highlightPost) {
        if (amount == 0) return;

        String type;
        boolean buff = amount > 0;
        if (buff) type = "increased by ";
        else type = "decreased by ";

        Color color = buff ? HIGHLIGHT_COLOUR : DEBUFF_COLOUR;

        String text = bullet + desc + type + amount + post;
        if (highlightPost) tooltip.addPara(text, 3f, color, amount + post.replaceFirst("%", ""));
        else tooltip.addPara(text, 3f, color, amount + "");
    }
}