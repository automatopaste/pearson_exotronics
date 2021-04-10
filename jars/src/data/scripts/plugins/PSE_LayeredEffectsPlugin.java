package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import java.util.EnumSet;

public class PSE_LayeredEffectsPlugin implements CombatLayeredRenderingPlugin {
    private final PSE_CombatEffectsPlugin parent;

    PSE_LayeredEffectsPlugin(PSE_CombatEffectsPlugin parent) {
        this.parent = parent;
    }

    @Override
    public void init(CombatEntityAPI entity) {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CombatEngineLayers.class);
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine != null) {
            this.parent.render(layer);
        }
    }
}
