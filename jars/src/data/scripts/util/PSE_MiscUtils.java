package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public final class PSE_MiscUtils {
    private static final String droneCoronaSpecJsonFilename = "data/shipsystems/PSE_corona.system";
    private static final String droneBastionSpecJsonFilename = "data/shipsystems/PSE_bastion.system";
    private static final String MVASpecJsonFilename = "data/shipsystems/PSE_MVA.system";

    public static class PSE_CoronaSpecLoading {
        private static float[] initialOrbitAngleArray;
        private static float[] focusOrbitAngleArray;
        private static float[] orbitRadiusArray;

        private static int maxDeployedDrones;
        private static double launchDelay;
        private static double launchSpeed;
        private static String droneVariant;

        public static void loadJSON() throws JSONException, IOException {
            SettingsAPI settings = Global.getSettings();
            JSONObject droneSystemSpecJson = settings.loadJSON(droneCoronaSpecJsonFilename);
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("PSE_droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("PSE_launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("PSE_launchSpeed");
            droneVariant = droneSystemSpecJson.getString("PSE_droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("PSE_maxDrones");
            
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

        public static int getMaxDeployedDrones() {
            return maxDeployedDrones;
        }
        public static double getLaunchDelay() {
            return launchDelay;
        }
        public static double getLaunchSpeed() {
            return launchSpeed;
        }
        public static String getDroneVariant() {
            return droneVariant;
        }
    }

    public static class PSE_BastionSpecLoading {
        private static float[] cardinalOrbitAngleArray;
        private static float[] frontOrbitAngleArray;
        private static float[] orbitRadiusArray;

        private static int maxDeployedDrones;
        private static double launchDelay;
        private static double launchSpeed;
        private static String droneVariant;

        public static void loadJSON() throws JSONException, IOException {
            SettingsAPI settings = Global.getSettings();
            JSONObject droneSystemSpecJson = settings.loadJSON(droneBastionSpecJsonFilename);
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("PSE_droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("PSE_launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("PSE_launchSpeed");
            droneVariant = droneSystemSpecJson.getString("PSE_droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("PSE_maxDrones");

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

        public static int getMaxDeployedDrones() {
            return maxDeployedDrones;
        }
        public static double getLaunchDelay() {
            return launchDelay;
        }
        public static double getLaunchSpeed() {
            return launchSpeed;
        }
        public static String getDroneVariant() {
            return droneVariant;
        }
    }

    public static class PSE_ModularVectorAssemblySpecLoading {
        private static float[] defenceOrbitAngleArray;
        private static float[] clampedOrbitAngleArray;
        private static float[] defenceOrbitRadiusArray;
        private static float[] clampedOrbitRadiusArray;
        private static float[] clampedFacingOffsetArray;

        private static int maxDeployedDrones;
        private static double launchDelay;
        private static double launchSpeed;
        private static String droneVariant;

        public static void loadJSON() throws JSONException, IOException {
            SettingsAPI settings = Global.getSettings();
            JSONObject droneSystemSpecJson = settings.loadJSON(MVASpecJsonFilename);
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("PSE_droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("PSE_launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("PSE_launchSpeed");
            droneVariant = droneSystemSpecJson.getString("PSE_droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("PSE_maxDrones");

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

        public static int getMaxDeployedDrones() {
            return maxDeployedDrones;
        }
        public static double getLaunchDelay() {
            return launchDelay;
        }
        public static double getLaunchSpeed() {
            return launchSpeed;
        }
        public static String getDroneVariant() {
            return droneVariant;
        }
    }

    public static void addJitterAfterimage(ShipAPI ship, Color color, float range, float velocityMult, float maxJitter, float in, float dur, float out, boolean additive, boolean combineWithSpriteColour, boolean aboveShip) {
        ship.addAfterimage(color, MathUtils.getRandomNumberInRange(-1f * range, range), MathUtils.getRandomNumberInRange(-1f * range, range), ship.getVelocity().getX() * velocityMult, ship.getVelocity().getY() * velocityMult, maxJitter, in, dur, out, additive, combineWithSpriteColour, aboveShip);
    }

    public static void applyFluxPerSecondPerFrame(ShipAPI ship, float fluxPerSecond, float amount) {
        ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() + (fluxPerSecond * amount));
        if (ship.getCurrFlux() >= ship.getMaxFlux()) {
            ship.getFluxTracker().forceOverload(0f);
        }
    }
}
