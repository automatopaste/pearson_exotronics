package data.scripts.plugins;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PSE_TitleScreenPlugin implements EveryFrameCombatPlugin{
    private static final int width = 3;
    private static final int height = 3;
    private static final float rowWidth = 300f;
    private static final float rowHeight = 500f;

    private final ShipAPI[][] shipGrid = new ShipAPI[height][width];
    private Vector2f gridStart = new Vector2f();
    private final Vector2f gridVelocity = new Vector2f(7f, 3f);

    @Override
    public void init(CombatEngineAPI engine) {
        for (int i = 0; i < height; i++) shipGrid[i] = null;
        gridStart = Vector2f.add(engine.getViewport().getCenter(), new Vector2f(-engine.getViewport().getVisibleWidth() * 0.5f, -engine.getViewport().getVisibleHeight() * 0.5f), null);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!Global.getCurrentState().equals(GameState.TITLE)) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        gridStart = Vector2f.add(gridStart, (Vector2f) new Vector2f(gridVelocity).scale(amount), null);

        for (ShipAPI ship : engine.getShips()) if (!ship.getHullSpec().getHullId().equals("PSE_penrith")) engine.removeEntity(ship);

        for (int i = 0; i < height; i++) {
            ShipAPI[] row = shipGrid[i];
            if (row == null) {
                row = getNewRow();
                shipGrid[i] = row;
            }

            for (int j = 0; j < width; j++) {
                ShipAPI ship = row[j];
                Vector2f loc = new Vector2f(j * rowHeight, i * rowWidth);
                Vector2f.add(loc, gridStart, loc);

                ship.getLocation().set(loc);
                ship.setFacing(VectorUtils.getFacing(gridVelocity));
            }
        }
    }

    private ShipAPI[] getNewRow() {
        ShipAPI[] ships = new ShipAPI[width];
        for (int i = 0; i < width; i++) {
            ShipAPI ship = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).spawnShipOrWing("PSE_penrith_Balanced", new Vector2f(), 0f);
            if (ship == null) throw new NullPointerException("lol");
            ships[i] = ship;
        }
        return ships;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

    }


    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }
}
