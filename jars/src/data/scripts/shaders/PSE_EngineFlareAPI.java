package data.scripts.shaders;

import data.scripts.shaders.util.PSE_ShaderRenderer;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public abstract class PSE_EngineFlareAPI {
    abstract Vector2f getLocation();

    public abstract void setLocation(Vector2f location);

    abstract Vector2f getSize();

    public abstract void setSize(Vector2f size);

    abstract Color getColor();

    public abstract void setColor(Color color);

    abstract PSE_ShaderRenderer getRenderer();

    abstract float getAngle();

    public abstract void setAngle(float angle);

    abstract boolean advance(float amount);
}
