package data.scripts.drones.rift;

import cmu.CMUtils;
import cmu.drones.systems.SystemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.corona.CoronaShipsystem;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RiftSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;

    private IntervalUtil tracker = new IntervalUtil(0.2f, 0.4f);

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

        RiftShipsystem system = (RiftShipsystem) SystemData.getDroneSystem(ship, engine);
        if (system == null) return;

        List<ShipAPI> ships = CMUtils.getShipsInRange(
                ship.getLocation(),
                RiftShipsystem.FIELD_EFFECT_RADIUS,
                ship.getOwner(),
                CMUtils.ShipSearchFighters.DONT_CARE,
                CMUtils.ShipSearchOwner.DONT_CARE
        );

        float weight = 0f;
        for (ShipAPI f : ships) {
            if (system.getForgeTracker().getDeployed().contains(f)) continue;

            if (f.isFighter()) {
                if (f.getOwner() == ship.getOwner()) {
                    if (f.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN)) weight += 10f;
                    if (f.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.POST_ATTACK_RUN)) weight += 10f;
                } else {
                    weight -= 15f;
                }
            } else {
                if (f.getOwner() == ship.getOwner() && !ship.isPullBackFighters()) {
                    weight += 15f;
                }
            }
        }

        switch (system.getDroneOrders()) {
            case DEFENCE:
                if (weight > 0f) system.cycleDroneOrders();
                break;
            case ECCM_ARRAY:
                if (weight <= 0f) system.cycleDroneOrders();
                break;
            case RECALL:
                List<ShipAPI> s = CMUtils.getShipsInRange(
                        ship.getLocation(),
                        4000f,
                        ship.getOwner(),
                        CMUtils.ShipSearchFighters.DONT_CARE,
                        CMUtils.ShipSearchOwner.ENEMY
                );
                if (!s.isEmpty()) system.cycleDroneOrders();
                break;
        }
    }
}
