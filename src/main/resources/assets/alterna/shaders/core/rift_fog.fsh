#version 330

#moj_import <minecraft:globals.glsl>

// Localized height fog filling the rift below a fixed world Y - NOT
// camera-distance fog. Screen-space pass: for every pixel, reconstruct the
// camera-relative world position from the depth buffer, intersect the view
// ray with a horizontal fog slab [FogBottomY, FogTopY], and fade by how
// much of the ray actually crosses the slab. Because everything outside
// the rift below that height is solid rock (rays simply never reach the
// slab there), the fog automatically appears only inside the rift - no
// horizontal mask needed.

layout(std140) uniform RiftFogInfo {
    vec4 FogColor;
    // camY, fogTopY, fogBottomY, density
    vec4 FogParams;
};

uniform sampler2D DepthSampler;

flat in mat4 invViewProj;

out vec4 fragColor;

void main() {
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;

    vec2 ndcXY = (gl_FragCoord.xy / ScreenSize) * 2.0 - 1.0;
    vec4 unproj = invViewProj * vec4(ndcXY, depth * 2.0 - 1.0, 1.0);
    // Camera-relative world position of whatever this pixel shows (terrain
    // surface, or a far-plane point for sky pixels - the slab clamp below
    // handles both identically).
    vec3 world = unproj.xyz / unproj.w;

    float segLen = length(world);
    vec3 dir = world / segLen;

    float camY = FogParams.x;
    float fogTop = FogParams.y;
    float fogBottom = FogParams.z;

    // Ray/slab intersection: the parametric span [tEnter, tExit] where
    // camY + dir.y * t lies inside [fogBottom, fogTop], clamped to the
    // visible segment [0, segLen].
    float tEnter;
    float tExit;
    if (abs(dir.y) < 1.0e-4) {
        bool inside = camY < fogTop && camY > fogBottom;
        tEnter = 0.0;
        tExit = inside ? segLen : 0.0;
    } else {
        float tA = (fogTop - camY) / dir.y;
        float tB = (fogBottom - camY) / dir.y;
        tEnter = clamp(min(tA, tB), 0.0, segLen);
        tExit = clamp(max(tA, tB), 0.0, segLen);
    }
    float travel = max(tExit - tEnter, 0.0);

    // Beer-Lambert over the crossed distance, then weighted by how deep
    // into the slab the crossing sits (midpoint of the span) - haze thin
    // and soft near the top boundary, thick further down, so the fog has
    // no visible hard ceiling at fogTop.
    float fog = 1.0 - exp(-travel * FogParams.w);
    float midY = camY + dir.y * (tEnter + tExit) * 0.5;
    float depthBelow = clamp((fogTop - midY) / (fogTop - fogBottom), 0.0, 1.0);
    fog *= mix(0.25, 1.0, smoothstep(0.0, 0.35, depthBelow));

    fragColor = vec4(FogColor.rgb, fog * FogColor.a);
}
