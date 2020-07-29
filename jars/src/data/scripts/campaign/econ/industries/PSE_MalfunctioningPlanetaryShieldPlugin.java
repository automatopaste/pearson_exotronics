package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.awt.*;

public class PSE_MalfunctioningPlanetaryShieldPlugin extends BaseIndustry {
    public static final float ACTIVE_DEFENCE_BONUS = 2f;
    private IntervalUtil deactivationTracker = new IntervalUtil(300f, 900f); //30 days to 90 days

    @Override
    public void apply() {
        super.apply(false);

        int size = market.getSize();
        applyIncomeAndUpkeep(size);

        float defenceBonus;
        if (!isDisrupted()) {
            defenceBonus = ACTIVE_DEFENCE_BONUS;
        } else {
            defenceBonus = 0f;
        }
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                getModId(), defenceBonus, getNameForModifier()
        );
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector().isPaused()) {
            return;
        }

        deactivationTracker.advance(amount);

        if (deactivationTracker.intervalElapsed()) {
            if (isDisrupted()) {
                super.disruptionFinished();
            } else {
                super.setDisrupted(deactivationTracker.getIntervalDuration() / 10f);
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
