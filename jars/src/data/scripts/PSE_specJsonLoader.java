package data.scripts;

import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;

import java.io.IOException;

public class PSE_specJsonLoader {
    public static final String droneCoronaSpecJsonFilepath = "data/shipsystems/PSE_droneCorona.system";
    public static final String droneCoronaSpecJsonFilename = "PSE_droneCorona";

    public PSE_specJsonLoader() {
    }

    public static JSONObject getDroneCoronaSpecJson() throws IOException, JSONException {
        return JSONUtils.loadCommonJSON(droneCoronaSpecJsonFilename, droneCoronaSpecJsonFilepath);
    }
}
