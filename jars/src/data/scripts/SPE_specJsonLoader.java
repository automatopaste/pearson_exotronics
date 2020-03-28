package data.scripts;

import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;

import java.io.IOException;

public class SPE_specJsonLoader {
    public static final String droneCoronaSpecJsonFilepath = "data/shipsystems/SPE_droneCorona.system";
    public static final String droneCoronaSpecJsonFilename = "SPE_droneCorona";

    public SPE_specJsonLoader () {
    }

    public static JSONObject getDroneCoronaSpecJson() throws IOException, JSONException {
        return JSONUtils.loadCommonJSON(droneCoronaSpecJsonFilename, droneCoronaSpecJsonFilepath);
    }
}
