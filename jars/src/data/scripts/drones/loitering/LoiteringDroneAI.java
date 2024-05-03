package data.scripts.drones.loitering;

import cmu.drones.ai.DroneAIUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;
import java.util.List;

public class LoiteringDroneAI implements ShipAIPlugin {

    private final ShipAPI drone;
    private final ShipAPI mothership;

    private float weaponRange;

    private final DroneAIUtils.PDControl control = new DroneAIUtils.PDControl() {
        @Override
        public float getKp() {
            return 10f;
        }
        @Override
        public float getKd() {
            return 3f;
        }

        @Override
        public float getRp() {
            return 3f;
        }

        @Override
        public float getRd() {
            return 1f;
        }
    };

    public LoiteringDroneAI(ShipAPI drone, ShipAPI mothership) {
        this.drone = drone;
        this.mothership = mothership;

        List<WeaponAPI> droneWeapons = drone.getAllWeapons();
        weaponRange = 0f;
        for (WeaponAPI weapon : droneWeapons) {
            if (weapon.getRange() > weaponRange) {
                weaponRange = weapon.getRange();
            }
        }
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        ShipAPI target = mothership.getShipTarget();
        if (target == null || target.getOwner() == drone.getOwner()) {
            target = (ShipAPI) DroneAIUtils.getEnemyTarget(mothership, drone, weaponRange, true, true, false, 120f);
        }

        Vector2f movePos = new Vector2f(mothership.getLocation());
        float targetFacing = mothership.getFacing();
        if (target != null && target.isAlive()) {
            Vector2f targetOffset = VectorUtils.getDirectionalVector(target.getLocation(), drone.getLocation());
            targetOffset.scale(weaponRange);
            Vector2f.add(targetOffset, drone.getLocation(), targetOffset);

            Vector2f mothershipOffset = VectorUtils.getDirectionalVector(mothership.getLocation(), drone.getLocation());
            mothershipOffset.scale(mothership.getShieldRadiusEvenIfNoShield() * 1.5f);
            Vector2f.add(mothershipOffset, drone.getLocation(), mothershipOffset);

            Vector2f v = Vector2f.sub(targetOffset, mothershipOffset, new Vector2f());
            v.scale(0.5f);
            Vector2f.add(v, targetOffset, v);

            targetFacing = VectorUtils.getFacing(v);

            movePos.set(v);
        }

        DroneAIUtils.move(movePos, drone, control);
        DroneAIUtils.rotate(targetFacing, drone, control);
    }

    @Override
    public void setDoNotFireDelay(float amount) {

    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        ShipwideAIFlags aiFlags = new ShipwideAIFlags();
        aiFlags.setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP);
        return aiFlags;
    }

    @Override
    public void cancelCurrentManeuver() {

    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }
}
