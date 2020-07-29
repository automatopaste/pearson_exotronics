package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.PSEDrone;
import data.scripts.shipsystems.PSE_DroneShroud;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public class PSE_DroneShroudSystemAI implements ShipSystemAIScript {
    private IntervalUtil tracker = new IntervalUtil(0.5f, 1.5f);

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private static final float CONCERN_WEIGHT_THRESHOLD = 30f;


    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @SuppressWarnings("unchecked")
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

        String key = "PSE_DroneList_" + ship.hashCode();
        ArrayList<PSEDrone> drones = (ArrayList<PSEDrone>) engine.getCustomData().get(key);
        int count = 0;
        for (PSEDrone drone : drones) {
            if (drone.getFluxTracker().getHardFlux() > 0.5f) {
                count += 1;
            }
        }

        float concernWeightTotal = 0f;
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 4000f)) {
            concernWeightTotal += (enemy.getFleetMember().getDeploymentCostSupplies() * (1f - (MathUtils.getDistance(ship, enemy) / 4000f)));
        }

        //useful debug display
        //engine.maintainStatusForPlayerShip("SHROUD_DEBUG", "graphics/icons/hullsys/drone_pd_high.png", "SYSTEM STATE", concernWeightTotal + ", " + count, true);

        switch (droneSystem.getDroneOrders()) {
            case CIRCLE:
                if (concernWeightTotal <= CONCERN_WEIGHT_THRESHOLD && count <= 1) {
                    droneSystem.nextDroneOrder();
                } else if (AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    droneSystem.nextDroneOrder();
                }
                break;
            case BROADSIDE_MOVEMENT:
                if (concernWeightTotal > CONCERN_WEIGHT_THRESHOLD) {
                    droneSystem.nextDroneOrder();
                } else if (count > 1) {
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
