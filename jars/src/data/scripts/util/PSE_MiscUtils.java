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
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("launchSpeed");
            droneVariant = droneSystemSpecJson.getString("droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("maxDrones");
            
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
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("launchSpeed");
            droneVariant = droneSystemSpecJson.getString("droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("maxDrones");

            cardinalOrbitAngleArray = new float[maxDeployedDrones];
            frontOrbitAngleArray = new float[maxDeployedDrones];
            orbitRadiusArray = new float[maxDeployedDrones];
            
            for (int i = 0; i < maxDeployedDrones; i++) {
                JSONObject droneConfigPerIndexJsonObject = droneBehaviorSpecJson.getJSONObject(i);
                
                cardinalOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("initialOrbitAngle");
                frontOrbitAngleArray[i] = Objects.requireNonNull(droneConfigPerIndexJsonObject).getInt("focusModeOrbitAngle");
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

    public static void addJitterAfterimage(ShipAPI ship, Color color, float range, float velocityMult, float maxJitter, float in, float dur, float out, boolean additive, boolean combineWithSpriteColour, boolean aboveShip) {
        ship.addAfterimage(color, MathUtils.getRandomNumberInRange(-1f * range, range), MathUtils.getRandomNumberInRange(-1f * range, range), ship.getVelocity().getX() * velocityMult, ship.getVelocity().getY() * velocityMult, maxJitter, in, dur, out, additive, combineWithSpriteColour, aboveShip);
    }

    public static void applyFluxPerSecondPerFrame(ShipAPI ship, float fluxPerSecond, float amount) {
        ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() + (fluxPerSecond * amount));
    }
}
