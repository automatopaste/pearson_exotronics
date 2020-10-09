package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.PSE_DroneModularVectorAssembly;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class PSE_MVASystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private static List<ShipwideAIFlags.AIFlags> maneuverFlags = new ArrayList<>();
    static {
        //maneuverFlags.add(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.BACKING_OFF);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.PURSUING);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.RUN_QUICKLY);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.TURN_QUICKLY);
    }

    private IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        //unique identifier so that individual system can be gotten from combat engine custom data
        String UNIQUE_SYSTEM_ID = PSE_DroneModularVectorAssembly.UNIQUE_SYSTEM_PREFIX + ship.hashCode();
        PSE_DroneModularVectorAssembly droneSystem = (PSE_DroneModularVectorAssembly) engine.getCustomData().get(UNIQUE_SYSTEM_ID);
        if (droneSystem == null || ship == null || !ship.isAlive()) {
            return;
        }

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) {
            return;
        }

        switch (droneSystem.getDroneOrders()) {
            case TARGETING:
                for (ShipwideAIFlags.AIFlags flag : maneuverFlags) {
                    if (ship.getAIFlags().hasFlag(flag)) {
                        droneSystem.nextDroneOrder();
                        break;
                    }
                }
                break;
            case CLAMPED:
                boolean cycle = true;
                for (ShipwideAIFlags.AIFlags flag : maneuverFlags) {
                    if (ship.getAIFlags().hasFlag(flag)) {
                        cycle = false;
                    }
                }
                if (cycle) {
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
