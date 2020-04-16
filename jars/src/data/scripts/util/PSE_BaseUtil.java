package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.PSEModPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.MathUtils;

import java.io.IOException;

import static org.lazywizard.lazylib.combat.AIUtils.getEnemiesOnMap;

public class PSE_BaseUtil {
    public static final String droneCoronaSpecJsonFilename = "data/shipsystems/PSE_corona.system";

    public static final String droneBastionSpecJsonFilename = "data/shipsystems/PSE_bastion.system";

    public PSE_BaseUtil() {
    }

    public static JSONObject getDroneBastionSpecJson() throws IOException, JSONException {
        final SettingsAPI settings = Global.getSettings();
        return settings.loadJSON(droneBastionSpecJsonFilename);
    }

    public static JSONObject getDroneCoronaSpecJson() throws IOException, JSONException {
        final SettingsAPI settings = Global.getSettings();
        return settings.loadJSON(droneCoronaSpecJsonFilename);
    }
}
