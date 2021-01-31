package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.campaign.PSE_SODCampEventListener;
import data.scripts.campaign.intel.bar.events.PSE_SpecialAgentBarEventCreator;
import data.scripts.world.PSE.PSE_WorldGen;
import exerelin.campaign.SectorManager;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class PSE_AddCampaignContentToExistingSave implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean haveSSTC = Global.getSettings().getModManager().isModEnabled("salvage_and_solder_tc");

        if (haveSSTC) {
            Console.showMessage("Error: This command cannot be run if the mod \"Total Conversion: Salvage and Solder\" is in use!");
            return CommandResult.ERROR;
        }
        if (haveNexerelin && !SectorManager.getManager().isCorvusMode()) {
            Console.showMessage("Error: This command cannot be run if \"Nexerelin\" random core mode is in use!");
            return CommandResult.ERROR;
        }
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.getStar().getName().contains("Adelaide")) {
                Console.showMessage("Error: This command cannot be used more than once!");
                return CommandResult.ERROR;
            }
        }

        new PSE_WorldGen().generateToExistingSave(Global.getSector(), haveNexerelin);
        PSE_SODCampEventListener SODListener = new PSE_SODCampEventListener(false);
        Global.getSector().addTransientListener(SODListener);
        Global.getSector().getListenerManager().addListener(SODListener, true);

        Global.getLogger(PSE_AddCampaignContentToExistingSave.class).info("Added bar event to manager " + PSE_SpecialAgentBarEventCreator.class.toString() + " from command");
        BarEventManager.getInstance().addEventCreator(new PSE_SpecialAgentBarEventCreator());

        Console.showMessage("Successfully ran \"Pearson Exotronics\" campaign generation scripts. Newly added star systems will remain unsurveyed until visited!");
        return CommandResult.SUCCESS;
    }
}