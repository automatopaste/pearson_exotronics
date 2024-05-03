package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class DIYMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private final MissileAPI missile;
    private final ShipAPI launchingShip;

    private CombatEntityAPI target;

    public DIYMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        this.launchingShip = launchingShip;

        target = null;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    @Override
    public void advance(float amount) {
        if (missile.isFizzling() || missile.isFading()) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        // find a target to aim at
        if (target == null) {
            target = findTarget();
            return;
        }

        // ignore phase ships
        if (target.getCollisionClass() == CollisionClass.NONE) {
            return;
        }

        // follow target
        move(missile, target.getLocation(), amount);
    }

    private CombatEntityAPI findTarget() {
        // if the launching ship has an enemy already targeted, we will prioritise it
        ShipAPI shipTarget = launchingShip.getShipTarget();
        if (shipTarget != null && shipTarget.getOwner() != launchingShip.getOwner()) {
            return shipTarget;
        }

        // otherwise, find a new target
        // AIUtils is a very useful class in LazyLib
        ShipAPI enemy = AIUtils.getNearestEnemy(launchingShip);
        if (enemy != null) {
            return enemy;
        }

        return null;
    }

    private void move(MissileAPI missile, Vector2f target, float amount) {
        // rotate towards target

        Vector2f displacement = Vector2f.sub(target, missile.getLocation(), new Vector2f());
        float absoluteAngle = VectorUtils.getFacing(displacement);
        float relativeAngle = absoluteAngle - missile.getFacing();
        if (relativeAngle > 180f) relativeAngle -= 360f; // relative angle in range +180deg to -180deg

        float turnSpeed = Math.signum(relativeAngle) * missile.getMaxTurnRate(); // make the missile turn at its maximum rate
        turnSpeed *= amount; // turn rate per frame
        turnSpeed = Math.min(relativeAngle, turnSpeed); // do not overshoot

        missile.setFacing(missile.getFacing() + turnSpeed);

        // always accelerate
        missile.giveCommand(ShipCommand.ACCELERATE);
    }
}
