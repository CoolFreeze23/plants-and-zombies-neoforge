#version 150

// Sampler0: entity texture (mask), Sampler3: paint noise (bound by PazRenderPipelines.PaintNoiseShard)
uniform sampler2D Sampler0;
uniform sampler2D Sampler3;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 base = texture(Sampler0, texCoord0);
    if (base.a < 0.1) discard;

    float n = texture(Sampler3, texCoord0).r;

    float paintAlpha = n * vertexColor.a * base.a;

    fragColor = vec4(vertexColor.rgb, paintAlpha) * ColorModulator;
}
