package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.util.PSE_MiscUtils;
import data.scripts.world.PSE.PSE_WorldGen;
import exerelin.campaign.SectorManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.ModUtils;

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
        boolean haveIndEvo = Global.getSettings().getModManager().isModEnabled("deconomics");

        if (haveSSTC) {
            //coming soon(tm)
        }else if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new PSE_WorldGen().generate(Global.getSector());
        }
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
}
