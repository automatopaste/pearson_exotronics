package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PSE_BaseCompetentMissileAI implements MissileAIPlugin, GuidedMissileAI {
    protected ShipAPI launchingShip;
    protected MissileAPI missile;
    protected CombatEntityAPI target;

    public PSE_BaseCompetentMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        this.launchingShip = launchingShip;

        ShipAPI target = launchingShip.getShipTarget();
        if (target != null) this.target = target;
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) return;

        if (target == null) {
            if (!missile.isFading()) {
                target = findTarget();

                missile.giveCommand(ShipCommand.ACCELERATE);
            }
        } else {
            ShipAPI targetShip = null;
            if (target instanceof ShipAPI) targetShip = (ShipAPI) target;

            if (targetShip != null && !targetShip.isAlive()) {
                target = findTarget();

                if (target == null) return;
            }

            if (target.getCollisionClass() != CollisionClass.NONE && (targetShip == null || (!targetShip.isPhased() && targetShip.isAlive()))) {
                Vector2f relativeVelocity = Vector2f.sub(target.getVelocity(), missile.getVelocity(), new Vector2f());
                //Vector2f relativeVelocity = new Vector2f(target.getVelocity());
                Vector2f relativePosition = Vector2f.sub(target.getLocation(), missile.getLocation(), new Vector2f());
                float distance = relativePosition.length();
                float relativeSpeed = relativeVelocity.length();
                float acceleration = missile.getAcceleration();

                //relativeSpeed is always absolute so is an approx value probably worth tidying up sometime
                //t = -u - sqrt(u^2 - 4*0.5*a*-s) / 2*0.5*a
                double discriminant = Math.sqrt(relativeSpeed + (2f * acceleration * distance));
                float timeToReachCurrentEnemyLocation = (float) ((-relativeSpeed - discriminant) / acceleration);
                if (timeToReachCurrentEnemyLocation < 0f) {
                    timeToReachCurrentEnemyLocation = (float) ((-relativeSpeed + discriminant) / acceleration);
                }

                Vector2f projectedPosition = Vector2f.add((Vector2f) relativeVelocity.scale(timeToReachCurrentEnemyLocation), target.getLocation(), new Vector2f());
                Vector2f toProjected = Vector2f.sub(projectedPosition, missile.getLocation(), new Vector2f());

                float angularDistance = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getFacing(toProjected));

                if (Math.abs(angularDistance) < 30f) missile.giveCommand(ShipCommand.ACCELERATE);
                else if (Math.abs(angularDistance) > 45f) missile.giveCommand(ShipCommand.DECELERATE);

                float angvel = missile.getAngularVelocity();
                float decelerationAngleAbs = (angvel * angvel) / (2 *  missile.getTurnAcceleration());

                if (angularDistance < 0f) {
                    if (angularDistance < -decelerationAngleAbs) {
                        missile.giveCommand(ShipCommand.TURN_RIGHT);
                    } else {
                        missile.giveCommand(ShipCommand.STRAFE_RIGHT);
                        //missile.giveCommand(ShipCommand.TURN_LEFT);
                        dampRotation(amount);
                    }
                }
                else if (angularDistance > 0f) {
                    if (angularDistance > decelerationAngleAbs) {
                        missile.giveCommand(ShipCommand.TURN_LEFT);
                    } else {
                        missile.giveCommand(ShipCommand.STRAFE_LEFT);
                        //missile.giveCommand(ShipCommand.TURN_RIGHT);
                        dampRotation(amount);
                    }
                }
            }
        }
    }

    private void dampRotation(float amount) {
        float angvel = missile.getAngularVelocity();
        float turnaccPerAdvance = missile.getTurnAcceleration() * amount;
        if (angvel > 0f) {
            if (angvel > turnaccPerAdvance) {
                missile.setAngularVelocity(angvel - turnaccPerAdvance);
            } else {
                missile.setAngularVelocity(angvel * 0.5f);
            }
        } else {
            if (-angvel > turnaccPerAdvance) {
                missile.setAngularVelocity(angvel + turnaccPerAdvance);
            } else {
                missile.setAngularVelocity(angvel * 0.5f);
            }
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    protected CombatEntityAPI findTarget() {
        ShipAPI closest = null;
        float distanceTracker = Float.MAX_VALUE;
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isPhased() || !ship.isAlive() || (launchingShip.getOwner() == ship.getOwner()) || ship.getCollisionClass() == CollisionClass.NONE) continue;

            float distanceSquared = MathUtils.getDistanceSquared(missile, ship);
            if (distanceSquared < distanceTracker) {
                closest = ship;
                distanceTracker = distanceSquared;
            }
        }

        return closest;
    }

    protected float getRemainingRange() {
        return missile.getMaxSpeed() * (missile.getMaxFlightTime() - missile.getFlightTime());
    }
}