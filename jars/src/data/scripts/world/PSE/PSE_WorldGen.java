package data.scripts.world.PSE;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.PSEModPlugin;
import data.scripts.util.PSE_CampaignUtils;
import data.scripts.world.PSE.systems.PSE_Adelaide;

import java.util.ArrayList;
import java.util.List;

public class PSE_WorldGen implements SectorGeneratorPlugin {
    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI league = sector.getFaction(Factions.PERSEAN);
        FactionAPI diktat = sector.getFaction(Factions.DIKTAT);
        FactionAPI pearson_exotronics = sector.getFaction("pearson_exotronics");

        pearson_exotronics.setRelationship(path.getId(), RepLevel.HOSTILE);
        pearson_exotronics.setRelationship(hegemony.getId(), RepLevel.FAVORABLE);
        pearson_exotronics.setRelationship(pirates.getId(), RepLevel.HOSTILE);
        pearson_exotronics.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
        pearson_exotronics.setRelationship(league.getId(), RepLevel.INHOSPITABLE);
        pearson_exotronics.setRelationship(church.getId(), RepLevel.SUSPICIOUS);
        pearson_exotronics.setRelationship(diktat.getId(), RepLevel.HOSTILE);
    }
    @Override
    public void generate(SectorAPI sector) {
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("pearson_exotronics");

        initFactionRelationships(sector);

        new PSE_Adelaide().generate(sector);
    }

    public void generateToExistingSave(SectorAPI sector, boolean useNexerelinTariffs) { //used when generating with console commands
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("pearson_exotronics");

        initFactionRelationships(sector);

        PSE_Adelaide adelaide = new PSE_Adelaide();
        adelaide.generate(sector);
        cleanUp(useNexerelinTariffs, adelaide.getMarkets());
    }

    private void cleanUp(boolean useNexerelinTariffs, List<MarketAPI> markets) {
        for (MarketAPI market : markets) {
            PersonAPI admin = PSE_CampaignUtils.createAdmin(market);
            market.setAdmin(admin);

            if (useNexerelinTariffs && market.getTariff() != null) {
                market.getTariff().modifyFlat("console_spawned_nexerelin_enabled_cleanup", -0.12f);
            }
        }
    }
}
