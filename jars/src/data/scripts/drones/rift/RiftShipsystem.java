package data.scripts.drones.rift;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import cmu.shaders.particles.PolygonParticle;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.PSE_PolygonParticlePlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RiftShipsystem extends DroneShipsystem implements ForgeSpec {

    public static final float FIELD_EFFECT_RADIUS = 1000f;
    private static final float FIELD_EFFECT_BONUS_PERCENT = 35f;
    private static final float MISSILE_FX_RADIUS = 0f;
    private static final float FIGHTER_FX_RADIUS = 10f;
    private static final Color FIELD_EFFECT_COLOUR = new Color(0, 255, 144, 255);
    private static final Color FIELD_ENEMY_EFFECT_COLOUR = new Color(255, 102, 0, 255);
    private final IntervalUtil fieldParticleInterval = new IntervalUtil(3f, 3f);
    private final IntervalUtil missileParticleInterval = new IntervalUtil(0.6f, 0.6f);

    private static final float[] mode2Speed = new float[]{20f, -20f, -20f, 20f};
    private static final float[] mode2Angles = new float[]{-45f, 45f, -135f, 135f};

    public enum RiftOrders {
        DEFENCE,
        ECCM_ARRAY,
        RECALL
    }

    private RiftOrders droneOrders = RiftOrders.RECALL;

    private final Map<RiftOrders, SpriteAPI> icons = new HashMap<>();

    private final float[] angles = new float[getMaxDeployedDrones()];

    private final IntervalUtil droneParticleInterval = new IntervalUtil(1f, 1f);
    public RiftShipsystem() {
        icons.put(RiftOrders.DEFENCE, Global.getSettings().getSprite("graphics/icons/hullsys/drone_pd_high.png"));
        icons.put(RiftOrders.ECCM_ARRAY, Global.getSettings().getSprite("graphics/icons/hullsys/drone_sensor.png"));
        icons.put(RiftOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();

        if (droneOrders == RiftOrders.ECCM_ARRAY) {
            for (int i = 0; i < getMaxDeployedDrones(); i++) {
                angles[i] = MathUtils.clampAngle(amount * mode2Speed[i] + angles[i]);
            }
        } else {
            System.arraycopy(mode2Angles, 0, angles, 0, mode2Angles.length);
        }

        switch (droneOrders) {
            case DEFENCE:
            case RECALL:
                fieldParticleInterval.setElapsed(0f);
                missileParticleInterval.setElapsed(0f);

                break;
            case ECCM_ARRAY:
                droneParticleInterval.advance(amount);
                fieldParticleInterval.advance(amount);
                missileParticleInterval.advance(amount);

                if (droneParticleInterval.intervalElapsed()) {
                    for (ShipAPI drone : getForgeTracker().getDeployed()) {
                        PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(6, CombatEngineLayers.UNDER_SHIPS_LAYER, FIELD_ENEMY_EFFECT_COLOUR, 0.05f);

                        params.computeFunction = new PSE_PolygonParticlePlugin.FollowComputeFunction(drone);

                        params.sizeInit = new Vector2f(0f, 0f);
                        params.sizeFinal = new Vector2f(100f, 50f);

                        params.lifetime = 2f;

                        params.color = FIELD_ENEMY_EFFECT_COLOUR.darker().darker();

                        PSE_PolygonParticlePlugin.add(drone.getLocation(), params);
                    }
                }

                if (fieldParticleInterval.intervalElapsed()) {
                    PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(24, CombatEngineLayers.UNDER_SHIPS_LAYER, FIELD_EFFECT_COLOUR.darker(), 0.05f);
                    params.computeFunction = new PSE_PolygonParticlePlugin.FollowComputeFunction(ship);
                    params.sizeInit = new Vector2f(0f, 0f);
                    params.sizeFinal = new Vector2f(FIELD_EFFECT_RADIUS, 10f);
                    params.color = FIELD_EFFECT_COLOUR.darker().darker().darker();
                    params.lifetime = 6f;

                    PSE_PolygonParticlePlugin.add(ship.getLocation(), params);
                }

                List<ShipAPI> shipsWithBuffedMissiles = new ArrayList<>();
                for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
                    if (MathUtils.getDistanceSquared(ship.getLocation(), missile.getLocation()) < FIELD_EFFECT_RADIUS * FIELD_EFFECT_RADIUS) {
                        if (!shipsWithBuffedMissiles.contains(missile.getSourceAPI())) shipsWithBuffedMissiles.add(missile.getSourceAPI());
                    }
                }

                ListIterator<ShipAPI> iterator = shipsWithBuffedMissiles.listIterator();
                while (iterator.hasNext()) {
                    ShipAPI next = iterator.next();

                    if (next == null || !Global.getCombatEngine().isEntityInPlay(next) || !next.isAlive() || next.isHulk()) iterator.remove();
                }

                for (ShipAPI s : shipsWithBuffedMissiles) {
                    for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
                        if (missile.getSource().equals(s)) {
                            missile.setEccmChanceBonus(100f);
                            missile.setEccmChanceOverride(100f);
                            missile.getSource().getMutableStats().getMissileAccelerationBonus().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                            missile.getSource().getMutableStats().getMissileTurnAccelerationBonus().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                            missile.getSource().getMutableStats().getMissileMaxSpeedBonus().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                            missile.getSource().getMutableStats().getMissileMaxTurnRateBonus().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);

                            Color c = FIELD_ENEMY_EFFECT_COLOUR;

                            if (missileParticleInterval.intervalElapsed()) {
                                PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(6, CombatEngineLayers.BELOW_SHIPS_LAYER, c, 0.5f);

                                params.computeFunction = new PSE_PolygonParticlePlugin.FollowComputeFunction(missile);

                                params.sizeInit = new Vector2f(MISSILE_FX_RADIUS + missile.getCollisionRadius(), 5f);
                                params.sizeFinal = new Vector2f(0f, 1f);

                                params.color = c.darker();

                                params.lifetime = 0.5f;

                                PSE_PolygonParticlePlugin.add(missile.getLocation(), params);
                            }
                        }
                    }
                }

                for (ShipAPI s : Global.getCombatEngine().getShips()) {
                    if (!s.isFighter() || s.isHulk() || !s.isAlive() || s.getHullSpec().getHullId().contains("_drone")) continue;

                    if (MathUtils.getDistanceSquared(ship.getLocation(), s.getLocation()) < FIELD_EFFECT_RADIUS * FIELD_EFFECT_RADIUS) {
                        s.getMutableStats().getTurnAcceleration().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                        s.getMutableStats().getMaxSpeed().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                        s.getMutableStats().getAcceleration().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);
                        s.getMutableStats().getMaxTurnRate().modifyPercent(RiftShipsystem.class.toString(), FIELD_EFFECT_BONUS_PERCENT);

                        Color c = (s.getOwner() == ship.getOwner()) ? FIELD_EFFECT_COLOUR : FIELD_ENEMY_EFFECT_COLOUR;

                        if (missileParticleInterval.intervalElapsed()) {
                            PolygonParticle.PolygonParams params = new PolygonParticle.PolygonParams(3, CombatEngineLayers.BELOW_SHIPS_LAYER, c, 0.2f);

                            params.computeFunction = new PSE_PolygonParticlePlugin.FollowComputeFunction(s);

                            params.sizeInit = new Vector2f(FIGHTER_FX_RADIUS + s.getShieldRadiusEvenIfNoShield(), 5f);
                            params.sizeFinal = new Vector2f(0f, 1f);

                            params.color = c.darker().darker();

                            params.lifetime = 1.8f;

                            PSE_PolygonParticlePlugin.add(s.getLocation(), params);
                        }
                    }
                }

                break;
        }
    }

    public float getAngle(int index) {
        if (index == -1) return 0f;
        return angles[index];
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == RiftOrders.values().length - 1) droneOrders = RiftOrders.values()[0];
        else droneOrders = RiftOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return RiftOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case DEFENCE:
                return "DEFENCE ARRAY";
            case ECCM_ARRAY:
                return "ECCM ARRAY";
            case RECALL:
            default:
                return "RECALL";
        }
    }

    @Override
    public SpriteAPI getIconForActiveState() {
        return icons.get(droneOrders);
    }

    @Override
    public int getMaxDeployedDrones() {
        return 4;
    }

    @Override
    public float getForgeCooldown() {
        return 25f;
    }

    @Override
    public float getLaunchDelay() {
        return 1f;
    }

    @Override
    public float getLaunchSpeed() {
        return 150f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_rift_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 2;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != RiftOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new RiftDroneAI(drone, mothership, this);
    }

    public RiftOrders getDroneOrders() {
        return droneOrders;
    }
}
