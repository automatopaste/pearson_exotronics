package data.scripts.drones.subsystems;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

import java.util.EnumSet;

public class LoiteringDroneAI implements ShipAIPlugin {

    private final ShipAPI drone;

    public LoiteringDroneAI(ShipAPI drone) {
        this.drone = drone;
    }

    @Override
    public void setDoNotFireDelay(float amount) {

    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public void advance(float amount) {

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
