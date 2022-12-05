package data.scripts.drones.vector;

import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.shroud.ShroudShipsystem;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class VectorSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private static final List<ShipwideAIFlags.AIFlags> maneuverFlags = new ArrayList<>();
    static {
        //maneuverFlags.add(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.BACKING_OFF);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.PURSUING);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.RUN_QUICKLY);
        maneuverFlags.add(ShipwideAIFlags.AIFlags.TURN_QUICKLY);
    }

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (ship == null || !ship.isAlive() | ship.isHulk()) return;

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        VectorShipsystem system = (VectorShipsystem) SystemData.getDroneSystem(ship, engine);
        if (system == null) return;

        switch (system.getDroneOrders()) {
            case RESONATOR:
                for (ShipwideAIFlags.AIFlags flag : maneuverFlags) {
                    if (ship.getAIFlags().hasFlag(flag)) {
                        system.cycleDroneOrders();
                        break;
                    }
                }
                break;
            case VECTOR_THRUST:
                boolean cycle = true;
                for (ShipwideAIFlags.AIFlags flag : maneuverFlags) {
                    if (ship.getAIFlags().hasFlag(flag)) {
                        cycle = false;
                    }
                }
                if (cycle) {
                    system.cycleDroneOrders();
                }
                break;
            case RECALL:
                if (!AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
                    system.cycleDroneOrders();
                }
                break;
        }
    }
}