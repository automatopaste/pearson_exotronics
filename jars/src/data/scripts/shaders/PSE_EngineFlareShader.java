package data.scripts.shaders;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.shaders.util.PSE_ShaderRenderer;
import org.dark.shaders.util.ShaderAPI;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class PSE_EngineFlareShader implements ShaderAPI {
    private static final String DATA_KEY = "we_do_a_little_trolling";

    private final boolean enabled;

    public PSE_EngineFlareShader() {
        if (!ShaderLib.areShadersAllowed() || !ShaderLib.areBuffersAllowed()) {
            enabled = false;
            return;
        }

        enabled = true;
    }

    public static void addFlare(ShipEngineControllerAPI.ShipEngineAPI engine, PSE_EngineFlareAPI flare) {
        final ShaderAPI flareShader = ShaderLib.getShaderAPI(PSE_EngineFlareShader.class);

        if (flareShader instanceof PSE_EngineFlareShader && flareShader.isEnabled()) {
            if (flare != null) {
                PSE_EngineFlareShader.FlareData localData = (PSE_EngineFlareShader.FlareData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
                if (localData == null) {
                    return;
                }
                final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = localData.flares;
                if (flares != null) {
                    flares.put(engine, flare);
                }
            }
        }
    }

    public static void removeFlare(ShipEngineControllerAPI.ShipEngineAPI engine) {
        final ShaderAPI flareShader = ShaderLib.getShaderAPI(PSE_EngineFlareShader.class);

        if (flareShader instanceof PSE_EngineFlareShader && flareShader.isEnabled()) {
            if (engine != null) {
                PSE_EngineFlareShader.FlareData localData = (PSE_EngineFlareShader.FlareData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
                if (localData == null) {
                    return;
                }
                final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = localData.flares;
                if (flares != null) {
                    flares.remove(engine);
                }
            }
        }
    }

    public static PSE_EngineFlareAPI getFlare(ShipEngineControllerAPI.ShipEngineAPI engine) {
        final ShaderAPI flareShader = ShaderLib.getShaderAPI(PSE_EngineFlareShader.class);

        if (flareShader instanceof PSE_EngineFlareShader && flareShader.isEnabled()) {
            PSE_EngineFlareShader.FlareData localData = (PSE_EngineFlareShader.FlareData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
            if (localData == null) {
                return null;
            }
            final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = localData.flares;
            if (flares != null) return flares.get(engine);
        }
        return null;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewportAPI) {
        if (!enabled) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        FlareData data = (FlareData) engine.getCustomData().get(DATA_KEY);
        if (data == null) return;

        final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = data.flares;
        if (flares != null && !flares.isEmpty()) drawFlares();
    }

    private void drawFlares() {
        FlareData data = (FlareData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data == null) return;
        final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = data.flares;
        if (flares == null) return;

        if (ShaderLib.useBufferCore()) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, ShaderLib.getAuxiliaryBufferId());
        } else if (ShaderLib.useBufferARB()) {
            ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, ShaderLib.getAuxiliaryBufferId());
        } else {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, ShaderLib.getAuxiliaryBufferId());
        }

        for (ShipEngineControllerAPI.ShipEngineAPI engine : new ArrayList<>(flares.keySet())) {
            PSE_EngineFlareAPI flare = flares.get(engine);

            PSE_ShaderRenderer s = new PSE_ShaderRenderer(
                    "data/shaders/engineflare.vert",
                    "data/shaders/pass.frag"
            );
            flare.getRenderer().render(
                    Global.getCombatEngine().getViewport(),
                    new Vector2f(flare.getLocation().x, flare.getLocation().y),
                    new Vector2f(flare.getSize().x, flare.getSize().y),
                    flare.getAngle()
            );
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> list) {
        if (!enabled) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        FlareData data = (FlareData) engine.getCustomData().get(DATA_KEY);
        if (data == null) return;
        final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = data.flares;
        if (flares == null) return;

        if (!engine.isPaused()) {
            ListIterator<ShipEngineControllerAPI.ShipEngineAPI> iter = new ArrayList<>(flares.keySet()).listIterator();
            while (iter.hasNext()) if (flares.get(iter.next()).advance(amount)) iter.remove();
        }
    }

    @Override
    public void initCombat() {
        Global.getCombatEngine().getCustomData().put(DATA_KEY, new FlareData());
    }

    @Override
    public void destroy() {
        CombatEngineAPI engine = Global.getCombatEngine();
        FlareData data = (FlareData) engine.getCustomData().get(DATA_KEY);
        if (data == null) return;
        final Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = data.flares;
        if (flares == null) return;

        for (ShipEngineControllerAPI.ShipEngineAPI e : flares.keySet()) flares.get(e).getRenderer().dispose();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public CombatEngineLayers getCombatLayer() {
        return CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER;
    }

    @Override
    public boolean isCombat() {
        return true;
    }

    @Override
    public RenderOrder getRenderOrder() {
        return RenderOrder.WORLD_SPACE;
    }

    public static class FlareData {
        public Map<ShipEngineControllerAPI.ShipEngineAPI, PSE_EngineFlareAPI> flares = new HashMap<>();
    }

    @Override
    public void renderInScreenCoords(ViewportAPI viewportAPI) {
    }
}