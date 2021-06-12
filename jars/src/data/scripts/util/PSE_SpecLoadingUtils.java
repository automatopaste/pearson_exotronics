package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import data.scripts.shipsystems.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PSE_SpecLoadingUtils {
    public static Map<String, PSE_DroneSystemSpec> droneSystemSpecHashMap = new HashMap<>();

    public static void loadBaseSystemSpecs() throws IOException, JSONException {
        SettingsAPI settings = Global.getSettings();

        JSONArray droneSystems = settings.loadCSV("data/shipsystems/drone_systems.csv");

        for (int i = 0; i < droneSystems.length(); i++) {
            JSONObject row = droneSystems.getJSONObject(i);
            String id = row.getString("id");
            String filename = row.getString("filename");

            JSONObject droneSystemSpecJson = settings.loadJSON(filename);

            PSE_DroneSystemSpec spec = new PSE_DroneSystemSpec();

            spec.launchDelay = droneSystemSpecJson.getDouble("PSE_launchDelay");
            spec.launchSpeed = droneSystemSpecJson.getDouble("PSE_launchSpeed");
            spec.droneVariant = droneSystemSpecJson.getString("PSE_droneVariant");
            spec.maxDeployedDrones = droneSystemSpecJson.getInt("PSE_maxDrones");
            spec.forgeCooldown = droneSystemSpecJson.getDouble("PSE_forgeCooldown");
            spec.filename = filename;

            droneSystemSpecHashMap.put(id, spec);
        }
    }

    public static String getFilenameForSystemID(String id) {
        return droneSystemSpecHashMap.get(id).filename;
    }

    public static class PSE_DroneSystemSpec {
        public int maxDeployedDrones;
        public double forgeCooldown;
        public double launchDelay;
        public double launchSpeed;
        public String droneVariant;
        public String filename;
    }

    public static JSONArray getDroneBehaviour(String filename) throws JSONException, IOException {
        JSONObject droneSystemSpecJson = Global.getSettings().loadJSON(filename);
        return droneSystemSpecJson.getJSONArray("PSE_droneBehavior");
    }

    public static class PSE_CoronaSpecLoading {
        private static float[] initialOrbitAngleArray;
        private static float[] focusOrbitAngleArray;
        private static float[] orbitRadiusArray;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneCorona.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneCorona.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;
            
            initialOrbitAngleArray = new float[maxDeployedDrones];
            focusOrbitAngleArray = new float[maxDeployedDrones];
            orbitRadiusArray = new float[maxDeployedDrones];
            
            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                initialOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("initialOrbitAngle");
                focusOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("focusModeOrbitAngle");
                orbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("orbitRadius");
            }
        }
        public static float[] getInitialOrbitAngleArray() {
            return initialOrbitAngleArray;
        }
        public static float[] getFocusOrbitAngleArray() {
            return focusOrbitAngleArray;
        }
        public static float[] getOrbitRadiusArray() {
            return orbitRadiusArray;
        }
    }

    public static class PSE_BastionSpecLoading {
        private static float[] cardinalOrbitAngleArray;
        private static float[] frontOrbitAngleArray;
        private static float[] orbitRadiusArray;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneBastion.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneBastion.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;

            cardinalOrbitAngleArray = new float[maxDeployedDrones];
            frontOrbitAngleArray = new float[maxDeployedDrones];
            orbitRadiusArray = new float[maxDeployedDrones];
            
            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                cardinalOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("cardinalOrbitAngle");
                frontOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("frontOrbitAngle");
                orbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("orbitRadius");
            }
        }
        public static float[] getCardinalOrbitAngleArray() {
            return cardinalOrbitAngleArray;
        }
        public static float[] getFrontOrbitAngleArray() {
            return frontOrbitAngleArray;
        }
        public static float[] getOrbitRadiusArray() {
            return orbitRadiusArray;
        }
    }

    public static class PSE_ModularVectorAssemblySpecLoading {
        private static float[] defenceOrbitAngleArray;
        private static float[] clampedOrbitAngleArray;
        private static float[] defenceOrbitRadiusArray;
        private static float[] clampedOrbitRadiusArray;
        private static float[] clampedFacingOffsetArray;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneModularVectorAssembly.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneModularVectorAssembly.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;

            defenceOrbitAngleArray = new float[maxDeployedDrones];
            clampedOrbitAngleArray = new float[maxDeployedDrones];
            defenceOrbitRadiusArray = new float[maxDeployedDrones];
            clampedOrbitRadiusArray = new float[maxDeployedDrones];
            clampedFacingOffsetArray = new float[maxDeployedDrones];

            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                defenceOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("defenceAngle");
                clampedOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("clampedAngle");
                defenceOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("defenceOrbitRadius");
                clampedOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("clampedOrbitRadius");
                clampedFacingOffsetArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("clampedFacingOffset");
            }
        }
        public static float[] getDefenceOrbitAngleArray() {
            return defenceOrbitAngleArray;
        }
        public static float[] getClampedOrbitAngleArray() {
            return clampedOrbitAngleArray;
        }
        public static float[] getDefenceOrbitRadiusArray() {
            return defenceOrbitRadiusArray;
        }
        public static float[] getClampedOrbitRadiusArray() {
            return clampedOrbitRadiusArray;
        }
        public static float[] getClampedFacingOffsetArray() {
            return clampedFacingOffsetArray;
        }
    }

    public static class PSE_CitadelSpecLoading {
        private static float[] antiFighterOrbitAngleArray;
        private static float[] antiFighterFacingOffsetArray;
        private static float[] antiFighterOrbitRadiusArray;
        private static float[] shieldOrbitRadiusArray;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneCitadel.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneCitadel.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;

            antiFighterOrbitAngleArray = new float[maxDeployedDrones];
            antiFighterFacingOffsetArray = new float[maxDeployedDrones];
            antiFighterOrbitRadiusArray = new float[maxDeployedDrones];
            shieldOrbitRadiusArray = new float[maxDeployedDrones];

            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                antiFighterOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("antiFighterOrbitAngle");
                antiFighterFacingOffsetArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("antiFighterFacingOffset");
                antiFighterOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("antiFighterOrbitRadius");
                shieldOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("shieldOrbitRadius");
            }
        }

        public static float[] getAntiFighterOrbitAngleArray() {
            return antiFighterOrbitAngleArray;
        }
        public static float[] getAntiFighterFacingOffsetArray() {
            return antiFighterFacingOffsetArray;
        }
        public static float[] getAntiFighterOrbitRadiusArray() {
            return antiFighterOrbitRadiusArray;
        }
        public static float[] getShieldOrbitRadiusArray() {
            return shieldOrbitRadiusArray;
        }
    }

    public static class PSE_ShroudSpecLoading { //angles and radii are handled in ai
        private static float[] orbitBaseRotationSpeed;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneShroud.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneShroud.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;

            orbitBaseRotationSpeed = new float[maxDeployedDrones];

            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                orbitBaseRotationSpeed[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("orbitBaseRotationSpeed");
            }
        }

        public static float[] getOrbitBaseRotationSpeed() {
            return orbitBaseRotationSpeed;
        }
    }

    public static class PSE_RiftSpecLoading {
        private static float[] fieldOrbitRadiusArray;
        private static float[] fieldOrbitSpeedArray;
        private static float[] defenceOrbitAngleArray;
        private static float[] defenceFacingArray;
        private static float[] defenceOrbitRadiusArray;

        public static void loadJSON() throws JSONException, IOException {
            JSONArray droneBehaviorSpecJson = getDroneBehaviour(getFilenameForSystemID(PSE_DroneRift.UNIQUE_SYSTEM_PREFIX));
            int maxDeployedDrones = droneSystemSpecHashMap.get(PSE_DroneRift.UNIQUE_SYSTEM_PREFIX).maxDeployedDrones;

            fieldOrbitRadiusArray = new float[maxDeployedDrones];
            fieldOrbitSpeedArray = new float[maxDeployedDrones];
            defenceOrbitAngleArray = new float[maxDeployedDrones];
            defenceFacingArray = new float[maxDeployedDrones];
            defenceOrbitRadiusArray = new float[maxDeployedDrones];

            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);

                fieldOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("fieldOrbitRadius");
                fieldOrbitSpeedArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("fieldOrbitSpeed");
                defenceOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("defenceOrbitAngle");
                defenceFacingArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("defenceFacing");
                defenceOrbitRadiusArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("defenceOrbitRadius");
            }
        }

        public static float[] getFieldOrbitRadiusArray() {
            return fieldOrbitRadiusArray;
        }
        public static float[] getFieldOrbitSpeedArray() {
            return fieldOrbitSpeedArray;
        }
        public static float[] getDefenceOrbitAngleArray() {
            return defenceOrbitAngleArray;
        }
        public static float[] getDefenceFacingArray() {
            return defenceFacingArray;
        }
        public static float[] getDefenceOrbitRadiusArray() {
            return defenceOrbitRadiusArray;
        }
    }
}