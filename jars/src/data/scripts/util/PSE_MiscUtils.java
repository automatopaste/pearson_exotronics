package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.Objects;

public final class PSE_MiscUtils {
    private static final String droneCoronaSpecJsonFilename = "data/shipsystems/PSE_droneCorona.system";
    private static final String droneBastionSpecJsonFilename = "data/shipsystems/PSE_droneBastion.system";
    private static final String droneMVASpecJsonFilename = "data/shipsystems/PSE_droneMVA.system";
    private static final String droneCitadelSpecJsonFilename = "data/shipsystems/PSE_droneCitadel.system";

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
            JSONObject droneSystemSpecJson = settings.loadJSON(droneMVASpecJsonFilename);
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

    public static class PSE_CitadelSpecLoading {
        private static float[] antiFighterOrbitAngleArray;
        private static float[] antiFighterFacingOffsetArray;
        private static float[] antiFighterOrbitRadiusArray;
        private static float[] shieldOrbitRadiusArray;

        private static int maxDeployedDrones;
        private static double launchDelay;
        private static double launchSpeed;
        private static String droneVariant;

        public static void loadJSON() throws JSONException, IOException {
            SettingsAPI settings = Global.getSettings();
            JSONObject droneSystemSpecJson = settings.loadJSON(droneCitadelSpecJsonFilename);
            JSONArray droneBehaviorSpecJson = droneSystemSpecJson.getJSONArray("PSE_droneBehavior");

            launchDelay = droneSystemSpecJson.getDouble("PSE_launchDelay");
            launchSpeed = droneSystemSpecJson.getDouble("PSE_launchSpeed");
            droneVariant = droneSystemSpecJson.getString("PSE_droneVariant");
            maxDeployedDrones = droneSystemSpecJson.getInt("PSE_maxDrones");

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

    public static boolean isEntityInArc(CombatEntityAPI entity, Vector2f center, float centerAngle, float arcDeviation) {
        //Vector2f entityRelativeLocation = Vector2f.sub(entity.getLocation(), center, new Vector2f());
        //float entityAngle = VectorUtils.getFacing(entityRelativeLocation);
        //float rel = MathUtils.getShortestRotation(entityAngle, centerAngle);
        if (entity instanceof ShipAPI) {
            Vector2f point = getNearestPointOnShipBounds((ShipAPI) entity, center);
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center, point);
        } else {
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center, getNearestPointOnCollisionRadius(entity,  center));
        }
        //return rel < arcDeviation && rel > -arcDeviation;
    }

    public static Vector2f getVectorFromAToB(CombatEntityAPI a, CombatEntityAPI b) {
        return Vector2f.sub(b.getLocation(), a.getLocation(), new Vector2f());
    }

    public static Vector2f getRandomVectorInCircleRange(float maxRange, float minRange, Vector2f center) {
        float dist = (minRange + ((float) Math.random() * (maxRange - minRange)));
        Vector2f loc = new Vector2f(0f, dist);
        VectorUtils.rotate(loc, (float) Math.random() * 360f);
        Vector2f.add(loc, center, loc);
        return loc;
    }

    public static Vector2f getRandomVectorInCircleRangeWithDistanceMult(float maxRange, float minRange, Vector2f center, float mult) {
        float dist = (minRange + (mult * (maxRange - minRange)));
        Vector2f loc = new Vector2f(0f, dist);
        VectorUtils.rotate(loc, (float) Math.random() * 360f);
        Vector2f.add(loc, center, loc);
        return loc;
    }

    public static Vector2f getNearestPointOnCollisionRadius(CombatEntityAPI entity, Vector2f point) {
        return MathUtils.getPointOnCircumference(
                entity.getLocation(),
                entity.getCollisionRadius(),
                VectorUtils.getAngle(entity.getLocation(), point)
        );
    }

    public static Vector2f getNearestPointOnRadius(Vector2f center, float radius, Vector2f point) {
        return MathUtils.getPointOnCircumference(
                center,
                radius,
                VectorUtils.getAngle(center, point)
        );
    }

    public static Vector2f getNearestPointOnShipBounds(ShipAPI ship, Vector2f point) {
        BoundsAPI bounds = ship.getExactBounds();
        if (bounds == null) {
            return getNearestPointOnCollisionRadius(ship, point);
        } else {
            Vector2f closest = ship.getLocation();
            float distSquared = 0f;
            for (BoundsAPI.SegmentAPI segment : bounds.getSegments()) {
                Vector2f tmpcp = MathUtils.getNearestPointOnLine(point, segment.getP1(), segment.getP2());
                float distSquaredTemp = MathUtils.getDistanceSquared(tmpcp, point);
                if (distSquaredTemp < distSquared) {
                    distSquared = distSquaredTemp;
                    closest = tmpcp;
                }
            }
            return closest;
        }
    }
}