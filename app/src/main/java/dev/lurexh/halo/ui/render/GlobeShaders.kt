package dev.lurexh.halo.ui.render

object GlobeShaders {
    const val SURFACE_VERTEX = """#version 300 es
precision highp float;
in vec3 aPosition;
in vec2 aUv;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec3 vNormal;
out vec2 vUv;
out vec3 vWorld;
void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorld = world.xyz;
    vNormal = normalize(mat3(uModel) * aPosition);
    vUv = aUv;
    gl_Position = uProjection * uView * world;
}
"""

    const val SURFACE_FRAGMENT = """#version 300 es
precision highp float;
in vec3 vNormal;
in vec2 vUv;
in vec3 vWorld;
uniform vec3 uAccent;
uniform vec3 uPulse;
uniform float uTime;
uniform float uScan;
uniform float uAlert;
out vec4 fragColor;
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void main() {
    vec3 n = normalize(vNormal);
    vec3 viewDir = normalize(vec3(0.0, 0.0, 3.2) - vWorld);
    float fresnel = pow(1.0 - max(dot(n, viewDir), 0.0), 2.4);
    float latitude = smoothstep(0.965, 1.0, abs(sin(vUv.y * 3.14159265 * 14.0)));
    float longitude = smoothstep(0.975, 1.0, abs(sin(vUv.x * 6.2831853 * 28.0)));
    float grid = max(latitude, longitude);
    vec2 cell = floor(vUv * vec2(96.0, 48.0));
    float dotMask = step(0.82, hash(cell));
    vec2 local = fract(vUv * vec2(96.0, 48.0)) - 0.5;
    float dotShape = 1.0 - smoothstep(0.12, 0.30, length(local));
    float dots = dotMask * dotShape;
    float sweep = exp(-pow((vUv.y - fract(uTime * 0.12)) * 10.0, 2.0)) * uScan;
    vec3 base = vec3(0.035, 0.055, 0.11);
    vec3 color = base;
    color += uAccent * (grid * 0.24 + dots * 0.11 + fresnel * 0.42);
    color += uPulse * sweep * 0.58;
    color = mix(color, vec3(1.0, 0.18, 0.28), uAlert * (0.18 + 0.12 * sin(uTime * 9.0)));
    fragColor = vec4(color, 0.96);
}
"""

    const val ATMOSPHERE_FRAGMENT = """#version 300 es
precision highp float;
in vec3 vNormal;
in vec2 vUv;
in vec3 vWorld;
uniform vec3 uAccent;
uniform float uAlert;
out vec4 fragColor;
void main() {
    vec3 n = normalize(vNormal);
    float rim = pow(1.0 - abs(n.z), 3.2);
    vec3 color = mix(uAccent, vec3(1.0, 0.22, 0.30), uAlert);
    fragColor = vec4(color, rim * 0.34);
}
"""

    const val POINT_VERTEX = """#version 300 es
precision highp float;
in vec3 aPosition;
in vec4 aColor;
in float aSize;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec4 vColor;
void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    gl_PointSize = aSize;
    vColor = aColor;
}
"""

    const val POINT_FRAGMENT = """#version 300 es
precision highp float;
in vec4 vColor;
out vec4 fragColor;
void main() {
    vec2 uv = gl_PointCoord - 0.5;
    float d = length(uv);
    float alpha = 1.0 - smoothstep(0.32, 0.5, d);
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
"""

    const val LINE_VERTEX = """#version 300 es
precision highp float;
in vec3 aPosition;
in vec4 aColor;
uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
out vec4 vColor;
void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    vColor = aColor;
}
"""

    const val LINE_FRAGMENT = """#version 300 es
precision highp float;
in vec4 vColor;
out vec4 fragColor;
void main() {
    fragColor = vColor;
}
"""
}
