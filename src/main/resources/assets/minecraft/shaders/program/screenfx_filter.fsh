#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Mode;
uniform float Amount;
uniform float Phase;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

void main() {
    int mode = int(Mode + 0.5);
    vec2 uv = texCoord;
    if (mode == 5) {
        float size = max(1.0, 1.0 + Amount * 23.0);
        vec2 cell = size / InSize;
        uv = (floor(uv / cell) + 0.5) * cell;
    } else if (mode == 7) {
        uv.x += sin(uv.y * 16.0 + Phase * 2.7) * 0.012 * Amount;
        uv.y += cos(uv.x * 12.0 + Phase * 2.1) * 0.009 * Amount;
    }
    vec3 color = texture(DiffuseSampler, clamp(uv, 0.0, 1.0)).rgb;
    float a = clamp(Amount, 0.0, 1.0);
    if (mode == 0) {
        float luma = dot(color, vec3(0.299, 0.587, 0.114));
        color = mix(color, vec3(luma), a);
    } else if (mode == 1) {
        vec3 sepia = vec3(dot(color, vec3(0.393, 0.769, 0.189)), dot(color, vec3(0.349, 0.686, 0.168)), dot(color, vec3(0.272, 0.534, 0.131)));
        color = mix(color, min(sepia, vec3(1.0)), a);
    } else if (mode == 2) {
        color = mix(color, vec3(1.0) - color, a);
    } else if (mode == 4) {
        float luma = dot(color, vec3(0.299, 0.587, 0.114));
        vec3 vivid = clamp(mix(vec3(luma), color, 1.0 + Amount * 1.1), 0.0, 1.0);
        color = mix(vivid, smoothstep(0.0, 1.0, vivid), a * 0.5);
    } else if (mode == 6) {
        float row = floor(uv.y * 48.0);
        float tick = floor(Phase * 10.0);
        float glitchLine = step(0.93, hash(row * 13.1 + tick * 7.7));
        vec2 shift = vec2((hash(row + tick) - 0.5) * 0.08 * glitchLine * Amount, 0.0);
        float split = Amount * (0.004 + 0.006 * hash(tick));
        color.r = texture(DiffuseSampler, clamp(uv + shift + vec2(split, 0.0), 0.0, 1.0)).r;
        color.g = texture(DiffuseSampler, clamp(uv + shift, 0.0, 1.0)).g;
        color.b = texture(DiffuseSampler, clamp(uv + shift - vec2(split, 0.0), 0.0, 1.0)).b;
    }
    fragColor = vec4(color, 1.0);
}
