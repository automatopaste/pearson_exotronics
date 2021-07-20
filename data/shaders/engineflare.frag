#version 330

uniform vec3 color;
uniform float time;

in vec2 uv;

void main() {
    float a = time * 0.5; //speed
    float b = 8.0; //lobe depth
    float n = 2.5; //number of lobes
    float x = uv.x;
    float y = uv.y;
    float pi = 3.14159;
    float h = (1.0 - (x * x * x)) * ((cos(pi * 2.0 * n * (x - a)) / b) + (1.0 - (1.0 / b)));
   
    //abs distance from func   
    float e = abs(y - 0.5);
    float d = abs(e - h);
   
    float t = clamp(((d / e) - 1.0), 0.0, 1.8);
   
    t *= clamp((0.75 * h + 0.75) * 3.0 * abs((0.45 * -(1.0 - x)) + e), 0.15, 3.0);


    // Output to screen
    gl_FragColor = vec4(color.xyz * t, 1.0);
}