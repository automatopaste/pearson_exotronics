package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.ai.PSE_BaseCompetentMissileAI;
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

    private static final String[] incompatibleMods = {
         "superdegenerateportraitpack"
    };

    public static final String MOD_ID = "pearson_exotronics";
    public static final String MOD_AUTHOR = "tomatopaste";
    public static final String MOD_ERROR_PREFIX =
            System.lineSeparator()
                    + System.lineSeparator() + MOD_ID + " by " + MOD_AUTHOR
                    + System.lineSeparator() + System.lineSeparator()
                    + "This wasn't supposed to happen..."
                    + System.lineSeparator();

    private static final String THRALL_ID = "PSE_thrall_missile";

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
        try {
            Global.getSettings().getScriptClassLoader().loadClass("data.scripts.util.MagicAnim");
        } catch (ClassNotFoundException ex) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "MagicLib is required to run at least one of the mods you have installed."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download MagicLib at http://fractalsoftworks.com/forum/index.php?topic=13718.0"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        for (String id : incompatibleMods) {
            if (Global.getSettings().getModManager().isModEnabled(id)) {
                String message = System.lineSeparator()
                        + System.lineSeparator() + "One or more of the mods you have installed has been made incompatible for technical or moral reasons."
                        + System.lineSeparator() + System.lineSeparator()
                        + "Name of incompatible mod: " + Global.getSettings().getModManager().getModSpec(id).getName()
                        + System.lineSeparator();
                throw new IllegalStateException(message);
            }
        }

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
        try {
            PSE_MiscUtils.PSE_CitadelSpecLoading.loadJSON();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        log.info("Added bar event to manager " + PSE_SpecialAgentBarEventCreator.class.toString());
        BarEventManager.getInstance().addEventCreator(new PSE_SpecialAgentBarEventCreator());
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String specId = missile.getProjectileSpecId();

        if (specId.equals(THRALL_ID)) {
            return new PluginPick<MissileAIPlugin>(new PSE_BaseCompetentMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SET);
        }

        return null;
    }
}