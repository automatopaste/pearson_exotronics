package data.scripts.shaders.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class PSE_ShaderRenderer {
    private int vao;
    private final PSE_ShaderProgram program;

    public PSE_ShaderRenderer(String vert, String frag) {
        this.program = new PSE_ShaderProgram();
        try {
            vert = Global.getSettings().loadText(vert);
            frag = Global.getSettings().loadText(frag);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        program.createVertexShader(vert);
        program.createFragmentShader(frag);
        program.link();

        //configure vao and vbos
        float[] vertices = new float[] {
                0f, 1f, 0f, 1f,
                1f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f,

                0f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
                1f, 0f, 1f, 0f
        };

        FloatBuffer verticesBuffer = createFloatBuffer(vertices.length);
        verticesBuffer.put(vertices).flip();

        // Create the VAO and bind to it
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Create the VBO and bind to it
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(0, 4, GL_FLOAT, false, 4 * Float.SIZE / Byte.SIZE, 0);

        glBindVertexArray(0);
    }

    public void render(ViewportAPI viewport, Vector2f position, Vector2f size, float angle) {
        if (position == null) return;

        glBindVertexArray(vao);

        program.bind();

        FloatBuffer viewBuffer = createFloatBuffer(16);
        matrixToBuffer(getViewMatrixBuffer(viewport, position, size, angle), viewBuffer);
        glUniformMatrix4(glGetUniformLocation(program.getProgramID(), "view"), false, viewBuffer);

        //color vector
        //FloatBuffer colorBuffer = new Vector3f(1.0f, 0.5f, 0.0f).get(BufferUtils.createFloatBuffer(3));
        //glUniform3(glGetUniformLocation(program.getProgramID(), "color"), colorBuffer);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        program.unbind();
        glBindVertexArray(0);
    }

    private Matrix4f getViewMatrixBuffer(ViewportAPI viewport, Vector2f position, Vector2f size, float angle) {
        return new Matrix4f()
                .translate(new Vector3f(position.x - 0.5f * size.x, position.y - 0.5f * size.y, 0f))
                .translate(0.5f * size.x, 0.5f * size.y, 0f)

                .rotate((float) Math.toRadians(angle), new Vector3f(0f, 0f, 1f))
                .translate(new Vector3f(-0.5f * size.x, -0.5f * size.y, 0f))

                .scale(new Vector3f(size.x, size.y, 1f))
                .translate(new Vector3f(-viewport.getCenter().x, -viewport.getCenter().y, 0f))
                .translate(new Vector3f(viewport.getVisibleWidth() / 2f, viewport.getVisibleHeight() / 2f, 0f))

                .translate(viewport.getCenter().x, viewport.getCenter().y, 0f)
                .scale(viewport.getViewMult(), viewport.getViewMult(), 1f)
                .translate(-viewport.getCenter().x, -viewport.getCenter().y, 0f)

                .ortho(0f, viewport.getVisibleWidth(), viewport.getVisibleHeight(), 0f, -1f, 1f);
    }

    public void dispose() {
        program.dispose();
    }

    private void matrixToBuffer(float[] m, int offset, FloatBuffer dest) {
        for (int i = 0; i < 16; i++) dest.put(i + offset, m[i]);
    }

    private static FloatBuffer createFloatBuffer(int size) {
        return ByteBuffer.allocateDirect(size << 2)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }
}