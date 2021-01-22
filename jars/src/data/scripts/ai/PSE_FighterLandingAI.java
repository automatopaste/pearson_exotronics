package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class PSE_FighterLandingAI implements ShipAIPlugin {
    private Vector2f destination;
    private ShipAPI ship;

    public PSE_FighterLandingAI(Vector2f destination, ShipAPI ship) {
        this.destination = destination;
        this.ship = ship;
    }

    @Override
    public void setDoNotFireDelay(float amount) {

    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        Vector2f fromFighterToBay = Vector2f.sub(destination, ship.getLocation(), new Vector2f());
        Vector2f resultant = Vector2f.add((Vector2f) fromFighterToBay.scale(0.5f * amount), ship.getLocation(), new Vector2f());
        ship.getLocation().set(resultant);
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return null;
    }

    @Override
    public void cancelCurrentManeuver() {

    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }
}
