package data.scripts.world.PSE;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.world.PSE.systems.PSE_Adelaide;

public class PSE_WorldGen implements SectorGeneratorPlugin {

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI league = sector.getFaction(Factions.PERSEAN);
        FactionAPI pearson_exotronics = sector.getFaction("pearson_exotronics");

        pearson_exotronics.setRelationship(path.getId(), RepLevel.HOSTILE);
        pearson_exotronics.setRelationship(hegemony.getId(), RepLevel.FRIENDLY);
        pearson_exotronics.setRelationship(pirates.getId(), RepLevel.HOSTILE);
        pearson_exotronics.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
        pearson_exotronics.setRelationship(league.getId(), RepLevel.FAVORABLE);
        pearson_exotronics.setRelationship(church.getId(), RepLevel.SUSPICIOUS);
    }
    @Override
    public void generate(SectorAPI sector) {
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("pearson_exotronics");

        initFactionRelationships(sector);

        new PSE_Adelaide().generate(sector);
    }
}
