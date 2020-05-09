package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
//import data.scripts.ai.kiprad_droneCoronaDroneAI;
//import data.scripts.weapons.ai.kiprad_eclipseAI;
//import data.scripts.world.kiprad.KIPRADGen;//
//import data.scripts.ai.PSE_droneCoronaDroneAI;
import data.scripts.util.PSE_BaseUtil;
import exerelin.campaign.SectorManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class PSEModPlugin extends BaseModPlugin {
    public static final String MOD_ID = "pearson_exotronics";
    public static final String MOD_AUTHOR = "tomatopaste";
    public static final String MOD_ERROR_PREFIX =
            System.lineSeparator()
                    + System.lineSeparator() + MOD_ID + " by " + MOD_AUTHOR
                    + System.lineSeparator() + System.lineSeparator()
                    + "This wasn't supposed to happen..."
                    + System.lineSeparator();

    public static final String DEUCES_DRONE_CORONA_ID = "PSE_deuces";

    public static JSONObject droneCoronaSpecJson;
    public static JSONObject droneBastionSpecJson;

    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean haveSSTC = Global.getSettings().getModManager().isModEnabled("salvage_and_solder_tc");
        if (!haveSSTC) {

        }else if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            //do generation
        }
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
        droneBastionSpecJson = loadDroneBastionSpecJson();
    }

    public JSONObject loadDroneBastionSpecJson() {
        try {
            return PSE_BaseUtil.getDroneBastionSpecJson();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        throw new NullPointerException(MOD_ERROR_PREFIX + "Incorrectly loaded JSON file in: " + new Throwable().getStackTrace()[0].getMethodName());
    }

    public JSONObject loadDroneCoronaSpecJson() {
        try {
            return PSE_BaseUtil.getDroneCoronaSpecJson();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        throw new NullPointerException(MOD_ERROR_PREFIX + "Incorrectly loaded JSON file in: " + new Throwable().getStackTrace()[0].getMethodName());
    }
}
