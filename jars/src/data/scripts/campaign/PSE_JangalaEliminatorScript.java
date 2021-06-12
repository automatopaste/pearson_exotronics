package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CircularOrbitWithSpinAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.campaign.CircularOrbit;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

public class PSE_JangalaEliminatorScript implements EveryFrameScript {
    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    private float radius = 3250f;

    @Override
    public void advance(float amount) {
        PlanetAPI jangala = null;
        outer:
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.getName().equals("Jangala")) {
                    jangala = planet;
                    break outer;
                }
            }
        }
        if (jangala == null) return;

        float angle = jangala.getCircularOrbitAngle();
        radius -= 50f * amount;
        jangala.setCircularOrbit(jangala.getStarSystem().getStar(), angle, radius, 360f);

        if (radius <= jangala.getStarSystem().getStar().getRadius() * 1.25f) throw new IllegalStateException("Jangala has been eliminated.");
    }
}
