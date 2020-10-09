package data.scripts.campaign.PSE_rulecmd.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.DeliveryMissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.PSE_SODCampDeliveryIntel;

import java.util.List;
import java.util.Map;

public class PSE_SODCampDeliveryMission extends BaseCommandPlugin {
    private SectorEntityToken entity;
    private OptionPanelAPI options;
    protected MarketAPI market;
    private InteractionDialogAPI dialog;
    protected FactionAPI faction;

    private void init(SectorEntityToken entity) {
        this.entity = entity;

        faction = entity.getFaction();

        market = entity.getMarket();
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        Global.getLogger(PSE_SODCampDeliveryMission.class).info("Checking command " + command);

        entity = dialog.getInteractionTarget();
        init(entity);

        options = dialog.getOptionPanel();

        if (command.equals("PSE_completeMissions")) {
            completeMissions();
        } else if (command.equals("PSE_checkCompletion")) {
            return checkCompletion();
        }

        return true;
    }

    private boolean checkCompletion() {
        for (IntelInfoPlugin temp : Global.getSector().getIntelManager().getIntel(PSE_SODCampDeliveryIntel.class)) {
            PSE_SODCampDeliveryIntel intel = (PSE_SODCampDeliveryIntel) temp;
            if (!intel.isAccepted()) continue;
            if (intel.isEnding()) continue;

            MarketAPI dest = intel.getDestination();
            if (dest != market) continue;

            if (intel.hasEnough()) return true;
        }
        return false;
    }

    private void completeMissions() {
        dialog.getTextPanel().addPara("You contact the relevant parties and drop off the cargo at the agreed-upon dockside locations.");

        for (IntelInfoPlugin temp : Global.getSector().getIntelManager().getIntel(PSE_SODCampDeliveryIntel.class)) {
            PSE_SODCampDeliveryIntel intel = (PSE_SODCampDeliveryIntel) temp;
            if (!intel.isAccepted()) continue;
            if (intel.isEnding()) continue;

            MarketAPI dest = intel.getDestination();
            if (dest != market) continue;

            if (intel.hasEnough()) {
                intel.performDelivery(dialog);
            }
        }

        options.clearOptions();
        options.addOption("Continue", MarketCMD.DEBT_RESULT_CONTINUE);
    }
}
