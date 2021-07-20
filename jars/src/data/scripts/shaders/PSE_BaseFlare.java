package data.scripts.shaders;

import data.scripts.shaders.util.PSE_ShaderRenderer;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PSE_BaseFlare extends PSE_EngineFlareAPI {
    private Vector2f loc;
    private Vector2f size;
    private Color color;
    private final PSE_ShaderRenderer renderer;
    private float angle;

    public PSE_BaseFlare(Vector2f loc, Vector2f size, Color color, float angle, PSE_ShaderRenderer renderer) {
        this.loc = loc;
        this.size = size;
        this.color = color;
        this.angle = angle;
        this.renderer = renderer;
    }
    @Override
    public Vector2f getLocation() {
        return loc;
    }

    @Override
    public Vector2f getSize() {
        return size;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public PSE_ShaderRenderer getRenderer() {
        return renderer;
    }

    @Override
    public float getAngle() {
        return angle;
    }

    @Override
    public void setSize(Vector2f size) {
        this.size = size;
    }

    @Override
    public void setAngle(float angle) {
        this.angle = angle;
    }

    @Override
    public void setLocation(Vector2f loc) {
        this.loc = loc;
    }

    @Override
    public boolean advance(float amount) {
        return false;
    }
}
