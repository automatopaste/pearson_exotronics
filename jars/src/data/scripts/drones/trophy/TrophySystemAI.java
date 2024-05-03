package data.scripts.drones.trophy;

import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.vector.VectorShipsystem;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrophySystemAI implements ShipSystemAIScript {

    private static final float CHECK_RADIUS = 1600f;

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);

    private float missileDangerPower = 0f;
    private static final float GAIN = 30f;
    private static final float DECAY = -30f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (ship == null || !ship.isAlive() | ship.isHulk()) return;

        //engine.maintainStatusForPlayerShip(this, null, "idk", "level: " + missileDangerPower, false);

        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        TrophyShipsystem system = (TrophyShipsystem) SystemData.getDroneSystem(ship, engine);
        if (system == null) return;

        boolean missileDanger = checkMissileDanger(ship, engine, CHECK_RADIUS);
        if (missileDanger) {
            missileDangerPower += GAIN * tracker.getIntervalDuration();
        } else {
            missileDangerPower += DECAY * tracker.getIntervalDuration();
        }
        missileDangerPower = Math.min(missileDangerPower, 30f);
        missileDangerPower = Math.max(missileDangerPower, 0f);

        boolean cycle = false;
        switch (system.getDroneOrders()) {
            case LANTERN:
                if (system.getForgeTracker().getDeployed().size() < 2) {
                    cycle = true;
                }

                if (missileDangerPower < 15f) {
                    cycle = true;
                }

                break;
            case SHIELD:
                if (missileDangerPower > 15f) {
                    cycle = true;
                }

                break;
            case RECALL:
                if (!AIUtils.getNearbyEnemies(ship, 6000f).isEmpty()) {
                    system.cycleDroneOrders();
                }

                break;
        }

        if (cycle) system.cycleDroneOrders();
    }

    private boolean checkMissileDanger(ShipAPI ship, CombatEngineAPI engine, float radius) {
        Iterator<Object> missileGrid = engine.getMissileGrid().getCheckIterator(ship.getLocation(), radius * 2f, radius * 2f);
        while (missileGrid.hasNext()) {
            Object o = missileGrid.next();
            if (!(o instanceof MissileAPI)) continue;
            MissileAPI missile = (MissileAPI) o;

            if (missile.getOwner() == ship.getOwner()) continue;

            Vector2f d = Vector2f.sub(missile.getLocation(), ship.getLocation(), new Vector2f());
            if (Vector2f.dot(d, d) < radius * radius) {
                return true;
            }
        }

        return false;
    }

}
