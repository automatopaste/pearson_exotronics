package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.misc.BreadcrumbIntel;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.PSE_SODCamp;
import data.scripts.campaign.intel.bar.events.PSE_SpecialAgentBarEvent;

import java.awt.*;
import java.util.Set;

public class PSE_SODCampLocationBreadcrumbIntel extends BreadcrumbIntel {
    private PSE_SODCamp camp;

    public PSE_SODCampLocationBreadcrumbIntel(SectorEntityToken foundAt, SectorEntityToken target) {
        super(foundAt, target);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float opad = 10f;

        //info.addImage("graphics/PSE/factions/PSE_logo_sod", width, opad);

        if (camp.getCurrentLifetime() >= 0) {
            LabelAPI p1 = info.addPara(
                    "You have discovered a clandestine intelligence base on " + camp.getAssociatedMarket().getName() +
                    " operated by the Special Operations Division. The base is estimated to have " + camp.getCurrentLifetime() +
                    " months of active time remaining, after which it will disperse. For a time, you can purchase surplus military" +
                    " equipment from them.", opad
            );
            p1.setHighlight(
                    camp.getAssociatedMarket().getName(),
                    "Special Operations Division",
                    camp.getCurrentLifetime() + ""
            );
            p1.setHighlightColors(
                    camp.getAssociatedMarket().getFaction().getBaseUIColor(),
                    Global.getSector().getFaction("pearson_division").getBaseUIColor(),
                    h
            );
        } else {
            LabelAPI p2 = info.addPara(
                    "You had discovered a clandestine intelligence base on " + camp.getAssociatedMarket().getName() +
                            " operated by the Special Operations Division. The base has since dispersed.", opad
            );
            p2.setHighlightColor(g);
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(getName(), getTitleColor(mode),0f);

        //addBulletPoints(info, mode);
    }

    @Override
    public String getName() {
        return "SOD Hidden Camp Location";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if (!tags.contains(Tags.INTEL_FLEET_LOG)) {
            tags.add(Tags.INTEL_FLEET_LOG);
        }
        tags.add(Tags.INTEL_MILITARY);
        return tags;
    }

    public void setCamp(PSE_SODCamp camp) {
        this.camp = camp;
    }
}
