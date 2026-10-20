#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Mode;
uniform float Amount;
uniform float Phase;
uniform float FxTime;
uniform float Seed;
uniform vec3 Color;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 tex(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, 0.0, 1.0)).rgb;
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 hueShift(vec3 color, float angle) {
    const vec3 k = vec3(0.57735);
    float c = cos(angle);
    return color * c + cross(k, color) * sin(angle) + k * dot(k, color) * (1.0 - c);
}

void main() {
    int mode = int(Mode + 0.5);
    vec2 uv = texCoord;
    float a = clamp(Amount, 0.0, 1.0);
    float aspect = InSize.x / InSize.y;
    vec2 centered = uv - 0.5;
    vec2 circ = vec2(centered.x * aspect, centered.y);
    float dist = length(circ);
    float ringLight = 0.0;

    if (mode == 5) {
        float size = max(1.0, 1.0 + Amount * 23.0);
        vec2 cell = size / InSize;
        uv = (floor(uv / cell) + 0.5) * cell;
    } else if (mode == 7) {
        uv.x += sin(uv.y * 16.0 + Phase * 2.7) * 0.012 * Amount;
        uv.y += cos(uv.x * 12.0 + Phase * 2.1) * 0.009 * Amount;
    } else if (mode == 9) {
        float radius = FxTime * 0.9;
        float ring = dist - radius;
        float width = 0.12;
        float wave = ring * exp(-ring * ring / (width * width * 0.25)) / width * 2.0 * clamp(1.0 - radius * 0.5, 0.0, 1.0);
        vec2 dir = dist > 0.0001 ? circ / dist : vec2(0.0);
        dir.x /= aspect;
        uv -= dir * wave * 0.06 * Amount;
        ringLight = abs(wave);
    } else if (mode == 10) {
        float k = 0.55 * Amount;
        float r2 = dot(circ, circ);
        uv = 0.5 + centered * (1.0 - k * 0.35 + k * r2) / (1.0 + k * 0.1);
    } else if (mode == 11) {
        float angle = Amount * 3.2 * pow(max(0.0, 1.0 - dist / 0.75), 2.0) + sin(Phase * 0.7) * 0.25 * Amount * step(0.0001, dist);
        float s = sin(angle);
        float c = cos(angle);
        vec2 rotated = vec2(circ.x * c - circ.y * s, circ.x * s + circ.y * c);
        uv = 0.5 + vec2(rotated.x / aspect, rotated.y);
    } else if (mode == 23) {
        float line = floor(uv.y * InSize.y / 3.0);
        float jitter = (hash(line + floor(Phase * 24.0)) - 0.5) * 0.004 * Amount;
        float tear = step(0.985, hash(floor(Phase * 6.0) + floor(uv.y * 20.0))) * 0.02 * Amount;
        uv.x += jitter + tear;
        uv.y += sin(Phase * 0.5) * 0.002 * Amount;
    } else if (mode == 26) {
        uv.x += sin(uv.y * 22.0 + Phase * 1.9) * 0.006 * Amount;
        uv.y += cos(uv.x * 18.0 + Phase * 1.4) * 0.006 * Amount;
    }

    vec3 base = tex(uv);
    vec3 color = base;

    if (mode == 9) {
        color += ringLight * 0.08 * a;
    } else if (mode == 0) {
        color = mix(color, vec3(luma(color)), a);
    } else if (mode == 1) {
        vec3 sepia = vec3(dot(color, vec3(0.393, 0.769, 0.189)), dot(color, vec3(0.349, 0.686, 0.168)), dot(color, vec3(0.272, 0.534, 0.131)));
        color = mix(color, min(sepia, vec3(1.0)), a);
    } else if (mode == 2) {
        color = mix(color, vec3(1.0) - color, a);
    } else if (mode == 4) {
        float l = luma(color);
        vec3 vivid = clamp(mix(vec3(l), color, 1.0 + Amount * 1.1), 0.0, 1.0);
        color = mix(vivid, smoothstep(0.0, 1.0, vivid), a * 0.5);
    } else if (mode == 6) {
        float row = floor(uv.y * 48.0);
        float tick = floor(Phase * 10.0);
        float glitchLine = step(0.93, hash(row * 13.1 + tick * 7.7));
        vec2 shift = vec2((hash(row + tick) - 0.5) * 0.08 * glitchLine * Amount, 0.0);
        float split = Amount * (0.004 + 0.006 * hash(tick));
        color.r = tex(uv + shift + vec2(split, 0.0)).r;
        color.g = tex(uv + shift).g;
        color.b = tex(uv + shift - vec2(split, 0.0)).b;
    } else if (mode == 8 || mode == 23) {
        vec2 offset = centered * 0.012 * Amount * (mode == 23 ? 0.6 : 1.0 + dist * 2.0);
        if (mode == 23) {
            offset = vec2(0.003 * Amount, 0.0);
        }
        color = vec3(tex(uv + offset).r, tex(uv).g, tex(uv - offset).b);
        if (mode == 23) {
            float scan = 0.85 + 0.15 * sin(uv.y * InSize.y * 1.4);
            float noise = hash2(uv * InSize + floor(Phase * 30.0)) - 0.5;
            color = mix(color, vec3(luma(color)), 0.25 * a);
            color = color * mix(1.0, scan, a) + noise * 0.08 * a;
            color *= 1.0 - 0.35 * a * smoothstep(0.35, 0.8, dist);
        }
    } else if (mode == 12) {
        float levels = mix(32.0, 4.0, a);
        color = floor(color * levels + 0.5) / levels;
    } else if (mode == 13) {
        float l = luma(color);
        float boosted = pow(clamp(l * (1.0 + 3.0 * Amount), 0.0, 1.0), 0.8);
        float noise = (hash2(uv * InSize + floor(Phase * 30.0)) - 0.5) * 0.12;
        float vignette = 1.0 - smoothstep(0.3, 0.75, dist);
        vec3 night = (boosted + noise) * Color * vignette;
        color = mix(color, night, a);
    } else if (mode == 14) {
        float l = luma(color);
        vec3 heat = l < 0.25 ? mix(vec3(0.0, 0.0, 0.2), vec3(0.3, 0.0, 0.6), l / 0.25)
                : l < 0.5 ? mix(vec3(0.3, 0.0, 0.6), vec3(0.9, 0.1, 0.1), (l - 0.25) / 0.25)
                : l < 0.75 ? mix(vec3(0.9, 0.1, 0.1), vec3(1.0, 0.8, 0.0), (l - 0.5) / 0.25)
                : mix(vec3(1.0, 0.8, 0.0), vec3(1.0), (l - 0.75) / 0.25);
        color = mix(color, heat, a);
    } else if (mode == 15) {
        vec3 glow = vec3(0.0);
        for (int i = 0; i < 12; i++) {
            float angle = float(i) * 0.5236;
            glow += tex(uv + vec2(cos(angle), sin(angle)) * oneTexel * (6.0 + 6.0 * float(i % 2)));
        }
        glow /= 12.0;
        vec3 dreamy = max(color, glow) * 0.85 + glow * 0.3;
        dreamy = mix(vec3(luma(dreamy)), dreamy, 1.25) * mix(vec3(1.0), Color, 0.5);
        color = mix(color, clamp(dreamy, 0.0, 1.0), a);
    } else if (mode == 16) {
        vec3 left = tex(uv - vec2(oneTexel.x, 0.0));
        vec3 right = tex(uv + vec2(oneTexel.x, 0.0));
        vec3 up = tex(uv - vec2(0.0, oneTexel.y));
        vec3 down = tex(uv + vec2(0.0, oneTexel.y));
        float edge = clamp(length(right - left) + length(down - up), 0.0, 1.0);
        vec3 outlined = mix(color, Color, smoothstep(0.08, 0.3, edge));
        color = mix(color, outlined, a);
    } else if (mode == 17) {
        vec2 offset = vec2(sin(Phase * 1.3) * 0.015 + 0.012, cos(Phase * 0.9) * 0.008) * Amount;
        color = mix(color, tex(uv + offset), 0.5 * a);
    } else if (mode == 18) {
        color = mix(tex(texCoord), tex(vec2(1.0 - texCoord.x, texCoord.y)), smoothstep(0.0, 1.0, a));
    } else if (mode == 19) {
        color = clamp(hueShift(color, Phase * 1.5 * Amount + Amount * 0.5), 0.0, 1.0);
    } else if (mode == 20) {
        color = clamp((color - 0.5) * (1.0 + Amount) + 0.5, 0.0, 1.0);
    } else if (mode == 21) {
        color = mix(color, luma(color) * Color * 1.4, a);
    } else if (mode == 22) {
        vec3 sum = vec3(0.0);
        float strength = 0.08 * Amount;
        for (int i = 0; i < 16; i++) {
            float scale = 1.0 - strength * (float(i) / 15.0);
            sum += tex(0.5 + centered * scale);
        }
        color = sum / 16.0;
    } else if (mode == 24) {
        float l = luma(color);
        vec3 binary = l > 0.45 ? Color : vec3(0.0);
        color = mix(color, binary, a);
    } else if (mode == 25) {
        int x = int(mod(gl_FragCoord.x / 2.0, 4.0));
        int y = int(mod(gl_FragCoord.y / 2.0, 4.0));
        int index = x + y * 4;
        float bayer[16] = float[16](0.0, 8.0, 2.0, 10.0, 12.0, 4.0, 14.0, 6.0, 3.0, 11.0, 1.0, 9.0, 15.0, 7.0, 13.0, 5.0);
        float threshold = (bayer[index] + 0.5) / 16.0 - 0.5;
        float levels = 4.0;
        vec3 dithered = floor(color * levels + threshold + 0.5) / levels;
        color = mix(color, clamp(dithered, 0.0, 1.0), a);
    } else if (mode == 26) {
        float caustic = sin(uv.x * 40.0 + Phase * 2.0) * sin(uv.y * 34.0 - Phase * 1.6);
        vec3 water = color * mix(vec3(1.0), Color * 1.6, 0.6) + max(caustic, 0.0) * 0.06;
        water *= 1.0 - 0.3 * smoothstep(0.3, 0.8, dist);
        color = mix(color, water, a);
    } else if (mode == 27) {
        vec3 blur = (tex(uv + vec2(oneTexel.x, 0.0)) + tex(uv - vec2(oneTexel.x, 0.0)) + tex(uv + vec2(0.0, oneTexel.y)) + tex(uv - vec2(0.0, oneTexel.y))) * 0.25;
        color = clamp(color + (color - blur) * 1.5 * Amount, 0.0, 1.0);
    }
    fragColor = vec4(color, 1.0);
}
