#version 330

layout (location = 0) in vec4 vertex;

out vec2 uv;

uniform mat4 view;

void main() {
	gl_Position = view * vec4(vertex.xy, 0.0, 1.0);
	uv = vertex.zw;
}