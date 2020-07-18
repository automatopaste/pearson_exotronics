package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.PSE_DroneCorona;
import data.scripts.shipsystems.PSE_DroneShroud;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class PSE_DroneShroudSystemAI implements ShipSystemAIScript {
    private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipSystemAPI system;

    private static final Map<ShipAPI.HullSize, Float> concernWeight = new HashMap<>(5);
    static {
        concernWeight.put(ShipAPI.HullSize.FIGHTER, 0f);
        concernWeight.put(ShipAPI.HullSize.FRIGATE, 2f);
        concernWeight.put(ShipAPI.HullSize.DESTROYER, 5f);
        concernWeight.put(ShipAPI.HullSize.CRUISER, 15f);
        concernWeight.put(ShipAPI.HullSize.CAPITAL_SHIP, 35f);
    }

    private static final float CONCERN_WEIGHT_THRESHOLD = 30f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        if (!tracker.intervalElapsed() || ship == null) {
            return;
        }

        //unique identifier so that individual system can be gotten from combat engine custom data
        String UNIQUE_SYSTEM_ID = "PSE_DroneShroud_" + ship.hashCode();
        PSE_DroneShroud droneSystem = (PSE_DroneShroud) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (droneSystem == null) {
            return;
        }

        float concernWeightTotal = 0f;
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 10000f)) {
            float mult = 1f - (MathUtils.getDistance(ship, enemy) / 10000f);
            concernWeightTotal += concernWeight.get(enemy.getHullSize()) * mult;
        }

        engine.maintainStatusForPlayerShip("SHROUD_STASDT_KEY", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", concernWeightTotal + "", true);

        int charges = system.getAmmo();
        switch (droneSystem.getDroneOrders()) {
            case CIRCLE:
                if (concernWeightTotal <= CONCERN_WEIGHT_THRESHOLD) {
                    droneSystem.nextDroneOrder();
                } else if (AIUtils.getNearbyEnemies(ship, 4000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
            case BROADSIDE_MOVEMENT:
                if (concernWeightTotal > CONCERN_WEIGHT_THRESHOLD) {
                    droneSystem.nextDroneOrder();
                } else if (AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
            case RECALL:
                if (!AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
        }
    }
}
