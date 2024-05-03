package data.scripts.drones.trophy;

import cmu.drones.systems.DroneShipsystem;
import cmu.drones.systems.ForgeSpec;
import cmu.drones.systems.ForgeTracker;
import cmu.misc.CombatUI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.drones.vector.VectorDroneAI;
import data.scripts.drones.vector.VectorShipsystem;
import data.scripts.plugins.FieldRenderer;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;

public class TrophyShipsystem extends DroneShipsystem implements ForgeSpec {

    public static final float SLOW_FIELD_RANGE = 800f;
    public static final float SLOW_FIELD_ARC = 60f;
    public static final float SLOW_FIELD_TOP_SPEED = 100f;
    public static final float SLOW_FIELD_FORCE = 0.2f;

    public enum TrophyOrders {
        LANTERN,
        SHIELD,
        RECALL
    }

    private TrophyOrders droneOrders = TrophyOrders.RECALL;

    private final Map<TrophyOrders, SpriteAPI> icons = new HashMap<>();

    private ShipAPI mothership;
    private FieldRenderer renderer;

    private final IntervalUtil particleInterval = new IntervalUtil(0.5f, 0.5f);

    public TrophyShipsystem() {
        icons.put(TrophyOrders.LANTERN, Global.getSettings().getSprite("graphics/icons/hullsys/emp_emitter.png"));
        icons.put(TrophyOrders.SHIELD, Global.getSettings().getSprite("graphics/icons/hullsys/fortress_shield.png"));
        icons.put(TrophyOrders.RECALL, Global.getSettings().getSprite("graphics/icons/hullsys/recall_device.png"));
    }

    @Override
    public ForgeTracker initDroneSystem(ShipAPI mothership) {
        this.mothership = mothership;

        SpriteAPI fieldTex = Global.getSettings().getSprite("fx", "slowfield");

        renderer = new FieldRenderer(fieldTex, mothership, SLOW_FIELD_ARC, SLOW_FIELD_RANGE);
        Global.getCombatEngine().addLayeredRenderingPlugin(renderer);

        return new ForgeTracker(this, mothership, this);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);

        CombatEngineAPI engine = Global.getCombatEngine();

        particleInterval.advance(engine.getElapsedInLastFrame());

        float fieldAngle;
        switch (droneOrders) {
            case LANTERN:
                List<ShipAPI> deployed = getForgeTracker().getDeployed();
                if (deployed.size() >= 2) {
                    Vector2f loc0 = deployed.get(0).getLocation();
                    Vector2f loc1 = deployed.get(1).getLocation();

                    Vector2f sub = Vector2f.sub(loc1, loc0, new Vector2f());
                    sub.scale(0.5f);
                    Vector2f center = Vector2f.add(loc0, sub, new Vector2f());

                    fieldAngle = VectorUtils.getFacing(Vector2f.sub(center, mothership.getLocation(), new Vector2f()));
                } else {
                    break;
                }

                Iterator<Object> missileGrid = engine.getMissileGrid().getCheckIterator(mothership.getLocation(), SLOW_FIELD_RANGE * 2f, SLOW_FIELD_RANGE * 2f);
                while (missileGrid.hasNext()) {
                    Object o = missileGrid.next();
                    if (!(o instanceof MissileAPI)) continue;
                    MissileAPI missile = (MissileAPI) o;

                    Vector2f displacement = Vector2f.sub(missile.getLocation(), mothership.getLocation(), new Vector2f());
                    float d2 = Vector2f.dot(displacement, displacement);

                    final float r2 = SLOW_FIELD_RANGE * SLOW_FIELD_RANGE;
                    if (r2 < d2) continue;

                    float angle = VectorUtils.getFacing(displacement);
                    float angleDisplacement = MathUtils.getShortestRotation(angle, fieldAngle);

                    if (Math.abs(angleDisplacement) > SLOW_FIELD_ARC * 0.5f) continue;

                    Vector2f vel = new Vector2f(missile.getVelocity());
                    float v2 = Vector2f.dot(vel, vel);

                    final float cap2 = SLOW_FIELD_TOP_SPEED * SLOW_FIELD_TOP_SPEED;
                    if (v2 > cap2) {
                        float f = SLOW_FIELD_FORCE * v2; // quadratic friction
                        Vector2f force = new Vector2f(engine.getElapsedInLastFrame() * f, 0f);
                        force = VectorUtils.rotate(force, VectorUtils.getFacing(vel) + 180f);
                        Vector2f.add(missile.getVelocity(), force, missile.getVelocity());

                        missile.setSpriteAlphaOverride(0.5f);

                        if (particleInterval.intervalElapsed()) {
                            engine.addSmoothParticle(missile.getLocation(), missile.getVelocity(), 20f, 10f, 3f, new Color(0, 255, 119, 255));
                        }
                    } else {
                        missile.setSpriteAlphaOverride(1f);
                    }
                }

                renderer.setFieldAngle(fieldAngle);
                renderer.brighter(0.5f);

                break;
            case SHIELD:
            case RECALL:
                renderer.darker(0.5f);
                break;
        }
    }

    @Override
    public void cycleDroneOrders() {
        if (droneOrders.ordinal() == TrophyOrders.values().length - 1) droneOrders = TrophyOrders.values()[0];
        else droneOrders = TrophyOrders.values()[droneOrders.ordinal() + 1];
    }

    @Override
    public int getNumDroneOrders() {
        return TrophyOrders.values().length;
    }

    @Override
    public int getActiveDroneOrder() {
        return droneOrders.ordinal();
    }

    @Override
    public String getActiveDroneOrderTitle() {
        switch (droneOrders) {
            case LANTERN:
                return "LANTERN ARRAY";
            case SHIELD:
                return "SHIELD WALL";
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
        return 2;
    }

    @Override
    public float getForgeCooldown() {
        return 30f;
    }

    @Override
    public float getLaunchDelay() {
        return 0.5f;
    }

    @Override
    public float getLaunchSpeed() {
        return 150f;
    }

    @Override
    public String getDroneVariant() {
        return "PSE_trophy_wing";
    }

    @Override
    public int getMaxReserveCount() {
        return 1;
    }

    @Override
    public boolean canDeploy() {
        return droneOrders != TrophyOrders.RECALL;
    }

    @Override
    public ShipAIPlugin initNewDroneAIPlugin(ShipAPI drone, ShipAPI mothership) {
        return new TrophyDroneAI(drone, mothership, this);
    }

    public TrophyOrders getDroneOrders() {
        return droneOrders;
    }

}
