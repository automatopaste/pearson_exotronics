package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.campaign.PSE_SODCampEventListener;
import data.scripts.campaign.intel.bar.events.PSE_SpecialAgentBarEventCreator;
import data.scripts.util.PSE_MiscUtils;
import data.scripts.world.PSE.PSE_WorldGen;
import exerelin.campaign.SectorManager;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class PSEModPlugin extends BaseModPlugin {
    Logger log = Global.getLogger(PSEModPlugin.class);

    public static final String MOD_ID = "pearson_exotronics";
    public static final String MOD_AUTHOR = "tomatopaste";
    public static final String MOD_ERROR_PREFIX =
            System.lineSeparator()
                    + System.lineSeparator() + MOD_ID + " by " + MOD_AUTHOR
                    + System.lineSeparator() + System.lineSeparator()
                    + "This wasn't supposed to happen..."
                    + System.lineSeparator();

    public static boolean haveNexerelin = false;
    public static boolean isNexerelinRandomSector = false;

    public static boolean memes = Global.getSettings().getBoolean("PSE_memes");

    @Override
    public void onNewGame() {
        haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean haveSSTC = Global.getSettings().getModManager().isModEnabled("salvage_and_solder_tc");
        boolean haveIndEvo = Global.getSettings().getModManager().isModEnabled("deconomics");

        if (haveSSTC) {
            //coming soon(tm)
        } else if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new PSE_WorldGen().generate(Global.getSector());
        }

        isNexerelinRandomSector = haveNexerelin && SectorManager.getManager().isCorvusMode();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        PSE_SODCampEventListener SODListener = new PSE_SODCampEventListener(newGame);
        Global.getSector().addTransientListener(SODListener);
        Global.getSector().getListenerManager().addListener(SODListener, true);
    }

    @Override
    public void onApplicationLoad() throws ClassNotFoundException {

        //ty to certain mods for this excellent error message formatting
        try {
            Global.getSettings().getScriptClassLoader().loadClass("org.lazywizard.lazylib.ModUtils");
        } catch (ClassNotFoundException ex) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "LazyLib is required to run at least one of the mods you have installed."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download LazyLib at http://fractalsoftworks.com/forum/index.php?topic=5444"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }
        /*try {
            Global.getSettings().getScriptClassLoader().loadClass("data.scripts.util.MagicAnim");
        } catch (ClassNotFoundException ex) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "MagicLib is required to run at least one of the mods you have installed."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download MagicLib at http://fractalsoftworks.com/forum/index.php?topic=13718.0"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }*/

        //load some custom jsons
        try {
            PSE_MiscUtils.PSE_CoronaSpecLoading.loadJSON();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        try {
            PSE_MiscUtils.PSE_BastionSpecLoading.loadJSON();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        try {
            PSE_MiscUtils.PSE_ModularVectorAssemblySpecLoading.loadJSON();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        log.info("Added bar event to manager " + PSE_SpecialAgentBarEventCreator.class.toString());
        BarEventManager.getInstance().addEventCreator(new PSE_SpecialAgentBarEventCreator());
    }

    public static PersonAPI createAdmin(MarketAPI market)
    {
        FactionAPI faction = market.getFaction();
        PersonAPI admin = faction.createRandomPerson();
        int size = market.getSize();

        switch (size)
        {
            case 3:
            case 4:
                admin.setRankId(Ranks.GROUND_CAPTAIN);
                break;
            case 5:
                admin.setRankId(Ranks.GROUND_MAJOR);
                break;
            case 6:
                admin.setRankId(Ranks.GROUND_COLONEL);
                break;
            case 7:
            case 8:
            case 9:
            case 10:
                admin.setRankId(Ranks.GROUND_GENERAL);
                break;
            default:
                admin.setRankId(Ranks.GROUND_LIEUTENANT);
                break;
        }

        List<String> skills = Global.getSettings().getSortedSkillIds();

        int industries = 0;
        int defenses = 0;
        boolean military = market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY);

        for (Industry curr : market.getIndustries())
        {
            if (curr.isIndustry())
            {
                industries++;
            }
            if (curr.getSpec().hasTag(Industries.TAG_GROUNDDEFENSES))
            {
                defenses++;
            }
        }

        admin.getStats().setSkipRefresh(true);

        int num = 0;
        if (industries >= 2 || (industries == 1 && defenses == 1))
        {
            if (skills.contains(Skills.INDUSTRIAL_PLANNING))
            {
                admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
            }
            num++;
        }

        if (num == 0 || size >= 7)
        {
            if (military)
            {
                if (skills.contains(Skills.FLEET_LOGISTICS))
                {
                    admin.getStats().setSkillLevel(Skills.FLEET_LOGISTICS, 3);
                }
            }
            else if (defenses > 0)
            {
                if (skills.contains(Skills.PLANETARY_OPERATIONS))
                {
                    admin.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 3);
                }
            }
            else
            {
                // nothing else suitable, so just make sure there's at least one skill, if this wasn't already set
                if (skills.contains(Skills.INDUSTRIAL_PLANNING))
                {
                    admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
                }
            }
        }

        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        admin.getStats().setSkipRefresh(false);
        admin.getStats().refreshCharacterStatsEffects();
        admin.setPostId(Ranks.POST_ADMINISTRATOR);
        market.addPerson(admin);
        market.setAdmin(admin);
        market.getCommDirectory().addPerson(admin);
        ip.addPerson(admin);
        ip.getData(admin).getLocation().setMarket(market);
        ip.checkOutPerson(admin, "permanent_staff");

        return admin;
    }
}
