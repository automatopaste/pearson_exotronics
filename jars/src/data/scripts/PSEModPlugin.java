package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ai.DIYMissileAI;
import data.scripts.world.PSE.PSE_WorldGen;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

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

    private static final String THRALL_ID = "PSE_thrall_missile";

    private static final String CORONA_DRONE_ID = "PSE_drone1";

    public static boolean haveNexerelin = false;

    public static final Set<String> DRONE_SYSTEM_IDS = new HashSet<>();
    static {
        DRONE_SYSTEM_IDS.add("PSE_Corona");
        DRONE_SYSTEM_IDS.add("PSE_Bastion");
        DRONE_SYSTEM_IDS.add("PSE_Citadel");
        DRONE_SYSTEM_IDS.add("PSE_Vector");
        DRONE_SYSTEM_IDS.add("PSE_Rift");
        DRONE_SYSTEM_IDS.add("PSE_Shroud");
    }

    @Override
    public void onNewGame() {
//        haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
//        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
//            new PSE_WorldGen().generate(Global.getSector());
//        }

        new PSE_WorldGen().generate(Global.getSector());
    }

    @Override
    public void onGameLoad(boolean newGame) {
//        PSE_SODCampEventListener SODListener = new PSE_SODCampEventListener(newGame);
//        Global.getSector().getListenerManager().addListener(SODListener, true);
    }

    @Override
    public void onNewGameAfterTimePass() {
//        if (Global.getSettings().getBoolean("PSE_eliminateJangala")) Global.getSector().addTransientScript(new PSE_JangalaEliminatorScript());
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
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        // On the todo list
//        log.info("Added bar event to manager " + PSE_SpecialAgentBarEventCreator.class);
//        BarEventManager.getInstance().addEventCreator(new PSE_SpecialAgentBarEventCreator());
    }

    public static final String DIY_MISSILE_ID = "atropos_torp";

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String specID = missile.getProjectileSpecId();

        if (specID.equals(DIY_MISSILE_ID)) {
            MissileAIPlugin newAIPlugin = new DIYMissileAI(missile, launchingShip);
            return new PluginPick<>(newAIPlugin, CampaignPlugin.PickPriority.MOD_SET);
        }

        return null;
    }
}