#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float a = texture(Sampler0, texCoord0).a * vertexColor.a;
    if (a < 0.1) discard;

    fragColor = vec4(0.0, 0.0, 0.0, 1.0) * ColorModulator;
}
