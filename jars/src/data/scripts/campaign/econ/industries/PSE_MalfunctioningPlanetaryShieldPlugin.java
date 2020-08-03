package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.awt.*;

public class PSE_MalfunctioningPlanetaryShieldPlugin extends BaseIndustry {
    public static final float ACTIVE_DEFENCE_BONUS = 2f;
    private IntervalUtil deactivationTracker = new IntervalUtil(300f, 900f); //30 days to 90 days
    //private IntervalUtil deactivationTracker = new IntervalUtil(10f, 10f); //30 days to 90 days
    private boolean isMalfunctioned = false;

    @Override
    public void apply() {
        super.apply(false);

        int size = market.getSize();
        applyIncomeAndUpkeep(size);

        float defenceBonus;
        if (!isDisrupted() && !isMalfunctioned) {
            defenceBonus = ACTIVE_DEFENCE_BONUS;
        } else {
            defenceBonus = 1f;
        }
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                getModId(), defenceBonus, getNameForModifier()
        );

        if (isFunctional() && !isMalfunctioned) {
            applyVisuals(market.getPlanetEntity());
        } else {
            unapply();
        }
    }

    @Override
    public void unapply() {
        super.unapply();

        unapplyVisuals(market.getPlanetEntity());

        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector().isPaused()) {
            return;
        }

        deactivationTracker.advance(amount);

        if (deactivationTracker.intervalElapsed()) {
            isMalfunctioned = !isMalfunctioned;

            String message = "";
            int time = (int) deactivationTracker.getIntervalDuration() / 10;
            if (isMalfunctioned) {
                unapplyVisuals(market.getPlanetEntity());
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());

                message = "The planetary shield at New Caledonia has malfunctioned! " + time + " days of repair time estimated.";
            } else if (!isDisrupted()) {
                applyVisuals(market.getPlanetEntity());
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                        getModId(), ACTIVE_DEFENCE_BONUS, getNameForModifier()
                );

                message = "The planetary shield at New Caledonia has been repaired.";
            }

            if ((Global.getSector().getPlayerFleet().getStarSystem() != null &&
                    Global.getSector().getPlayerFleet().getStarSystem().equals(market.getStarSystem())
                    ) || Global.getSector().getPlayerFaction().getRelationship("pearson_exotronics") >= 0.5f ||
                    (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals("pearson_exotronics"))
            ) {
                Global.getSector().getCampaignUI().addMessage(message);
            }
        }
    }

    public static void applyVisuals(PlanetAPI planet) {
        if (planet == null) return;
        planet.getSpec().setShieldTexture(Global.getSettings().getSpriteName("industry", "PSE_malfunctioning_shield_texture"));
        planet.getSpec().setShieldThickness(0.1f);
        planet.getSpec().setShieldColor(new Color(255,255,255,175));
        planet.applySpecChanges();
    }

    public static void unapplyVisuals(PlanetAPI planet) {
        if (planet == null) return;
        planet.getSpec().setShieldTexture(null);
        planet.getSpec().setShieldThickness(0f);
        planet.getSpec().setShieldColor(null);
        planet.applySpecChanges();
    }
}
