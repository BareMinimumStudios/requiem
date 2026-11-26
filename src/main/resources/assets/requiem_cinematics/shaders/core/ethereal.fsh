#version 150

in vec2 uv;
uniform vec2 Canvas;
uniform vec2 Pointer;
uniform float Clock;
uniform float Opacity;
uniform vec3 Accent;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f *= f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + 1.0), f.x), f.y);
}

void main() {
    vec2 aspect = vec2(Canvas.x / Canvas.y, 1.0);
    vec2 p = (uv - 0.5) * aspect;
    vec2 pointer = (Pointer - 0.5) * aspect;
    vec2 drift = vec2(Clock * 0.018, -Clock * 0.011);
    vec2 q = (p - pointer * 0.035) * vec2(3.2, 5.0) + drift;
    float cloud = 0.57 * noise(q) + 0.28 * noise(q * 2.17 + 5.3)
                + 0.15 * noise(q * 4.38 - drift * 0.8);
    float arc = p.y + 0.28 * p.x + 0.14 * sin(2.9 * p.x + 0.4);
    float ribbon = exp(-14.0 * pow(abs(arc + 0.25 * (cloud - 0.5)), 2.0));
    float filament = pow(max(0.0, 1.0 - 2.1 * abs(arc - 0.16)), 5.0);
    float shimmer = 0.88 + 0.12 * sin(1.55 * Clock + 12.0 * cloud + p.x * 5.0);
    float strength = (0.27 * ribbon * smoothstep(0.27, 0.83, cloud) + 0.07 * filament) * shimmer;
    vec3 color = mix(vec3(0.16, 0.36, 0.52), Accent, smoothstep(0.16, 0.86, cloud));
    fragColor = vec4(color, strength * Opacity);
}
