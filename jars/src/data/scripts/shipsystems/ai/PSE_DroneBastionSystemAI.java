package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.PSE_DroneBastion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PSE_DroneBastionSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipSystemAPI system;
    private PSE_DroneBastion droneSystem;

    private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    private boolean isShipInFocusModeEngagementRange = false;
    private boolean isTargetVulnerable = false;

    private float longestWeaponRange = 0f;
    private float droneLongestWeaponRange = 0;

    String UNIQUE_SYSTEM_ID;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            //gets the range of the longest range ballistic or energy weapon on init
            if (weapon.getRange() > longestWeaponRange) {
                longestWeaponRange = weapon.getRange();
            }
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        //unique identifier so that individual system can be gotten from combat engine custom data
        UNIQUE_SYSTEM_ID = "PSE_droneBastion_" + ship.hashCode();

        this.droneSystem = (PSE_DroneBastion) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (droneSystem == null) {
            return;
        }

        if (tracker.intervalElapsed()  && ship != null) {
            List<MissileAPI> missilesInRange = AIUtils.getNearbyEnemyMissiles(ship, longestWeaponRange);
            boolean isMissileThreatPresent = !missilesInRange.isEmpty();
            float missileThreatAngle = 0;
            if (isMissileThreatPresent) {
                for (MissileAPI missile : missilesInRange) {
                    float a = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), missile.getLocation())));
                    a = Math.abs(a);
                    if (a > missileThreatAngle) {
                        missileThreatAngle = a;
                    }
                }
            }

            List<ShipAPI> shipsInRange = AIUtils.getNearbyEnemies(ship, longestWeaponRange);
            boolean isShipThreatPresent = !shipsInRange.isEmpty();
            float shipThreatAngle = 0;
            if (isShipThreatPresent) {
                for (ShipAPI eship : shipsInRange) {
                    float a = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), eship.getLocation())));
                    a = Math.abs(a);
                    if (a > shipThreatAngle) {
                        shipThreatAngle = a;
                    }
                }
            }

            switch (droneSystem.getDroneOrders()) {
                case CARDINAL:
                    if ((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle < 60 || missileThreatAngle < 90)) {
                        ship.useSystem();
                    }
                    break;
                case FRONT:
                    //switch to defensive mode if pilot is not a gigachad HIL user
                    if ((isShipThreatPresent || isMissileThreatPresent) && (shipThreatAngle >= 60 || missileThreatAngle >= 90)) {
                        ship.useSystem();
                    } else if (AIUtils.getNearbyEnemies(ship, longestWeaponRange * 2f).isEmpty()) {
                        ship.useSystem();
                    }
                    break;
                case RECALL:
                    if (!AIUtils.getNearbyEnemies(ship, longestWeaponRange * 2f).isEmpty()) {
                        ship.useSystem();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}