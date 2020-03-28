package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
//import data.scripts.ai.kiprad_droneCoronaDroneAI;
//import data.scripts.weapons.ai.kiprad_eclipseAI;
//import data.scripts.world.kiprad.KIPRADGen;//
import com.fs.starfarer.combat.entities.Ship;
//import data.scripts.ai.SPE_droneCoronaDroneAI;
import data.scripts.ai.SPE_droneCoronaDroneAI;
import exerelin.campaign.SectorManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.io.IOException;
import java.util.List;

public class SPEModPlugin extends BaseModPlugin {

    public static final String DEUCES_DRONE_CORONA_ID = "SPE_deuces";

    public static JSONObject droneCoronaSpecJson;

    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getCorvusMode()) {
            //new KIPRADGen().generate(Global.getSector());
        }
    }

    /*@Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (DEUCES_DRONE_CORONA_ID.contentEquals(ship.getHullSpec().getBaseHullId())) {
            SPEDroneAPI modDrone = new SPEDroneAPI(ship, AIUtils.getNearestAlly(ship));

            return new PluginPick<ShipAIPlugin>(new SPE_droneCoronaDroneAI(member, modDrone), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }*/

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
        droneCoronaSpecJson = loadDroneCoronaSpecJson();

    }

    public JSONObject loadDroneCoronaSpecJson() {
        try {
            return SPE_specJsonLoader.getDroneCoronaSpecJson();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
