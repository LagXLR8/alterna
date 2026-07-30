#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Better Clouds-style puff field. Each puff is a real 3D box in world
// space (24 vertices, same vertex table as vanilla's cloud shader) - NOT a
// camera-facing billboard, which is exactly how Better Clouds does it too;
// billboards read as flat cards spinning to face the player the moment the
// camera gets above the layer. The cumulus look comes from thousands of
// small overlapping boxes (see RiftCloudGenerator), not from any one box.

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    // PATCH_SIZE, SIZE_XZ, SIZE_Y, LIFT_RANGE
    vec4 Geometry;
    // xyz = camera-relative world position of the patch's cloud-space
    // origin (recomputed every frame on the CPU in double precision so
    // camera motion stays smooth with no float-precision issues),
    // w = time in ticks for the wave animation.
    vec4 Origin;
    // scaleFalloffMin, patchRadius, edgeFadeStart, puffOpacity
    vec4 Params;
};

// Two RGBA8 texels per puff, packed by RiftCloudGenerator.
uniform samplerBuffer CloudPuffs;

// Same unit-cube face table as vanilla core/rendertype_clouds.vsh - its
// winding is what the default backface culling expects.
const vec3[] vertices = vec3[](
    // Bottom face
    vec3(1, 0, 0), vec3(1, 0, 1), vec3(0, 0, 1), vec3(0, 0, 0),
    // Top face
    vec3(0, 1, 0), vec3(0, 1, 1), vec3(1, 1, 1), vec3(1, 1, 0),
    // North face
    vec3(0, 0, 0), vec3(0, 1, 0), vec3(1, 1, 0), vec3(1, 0, 0),
    // South face
    vec3(1, 0, 1), vec3(1, 1, 1), vec3(0, 1, 1), vec3(0, 0, 1),
    // West face
    vec3(0, 0, 1), vec3(0, 1, 1), vec3(0, 1, 0), vec3(0, 0, 0),
    // East face
    vec3(1, 0, 0), vec3(1, 1, 0), vec3(1, 1, 1), vec3(1, 0, 1)
);

const float[] faceBrightness = float[](
    0.70, // bottom
    1.00, // top
    0.86, 0.86, // north/south
    0.93, 0.93  // west/east
);

out float brightness;
out float fadeOpacity;
out float vertexDistance;

void main() {
    int puffIndex = gl_VertexID / 24;
    int rem = gl_VertexID % 24;
    int face = rem / 4;
    int corner = rem % 4;

    vec4 t0 = texelFetch(CloudPuffs, puffIndex * 2);
    vec4 t1 = texelFetch(CloudPuffs, puffIndex * 2 + 1);

    // Reconstruct u16 cloud-space offsets from the two byte pairs.
    float offX = dot(t0.rg, vec2(255.0, 65280.0)) / 65535.0 * Geometry.x;
    float offZ = dot(t0.ba, vec2(255.0, 65280.0)) / 65535.0 * Geometry.x;
    // Signed lift, byte-encoded by the generator over [-MAX_DIP, 1] with
    // MAX_DIP = 0.3 (see RiftCloudGenerator.MAX_DIP): weak areas sag below
    // the base plane, cores and wall-hugging puffs rise above it.
    float lift = t1.r * 1.3 - 0.3;
    // Power-curve size distribution: most puffs land small, a few blow up
    // to ~2.4x - mixed scales instead of a uniform brick size.
    float sizeScale = mix(0.35, 2.4, pow(t1.g, 2.2));
    // Per-puff density (t1.a): thick opaque cores, wispy thin rims.
    float density = mix(0.4, 1.0, t1.a);

    vec3 base;
    base.x = offX + Origin.x;
    base.z = offZ + Origin.z;

    float distXZ = length(base.xz);

    // BC's scaleFalloff: puffs shrink toward the patch edge so the field
    // thins out instead of ending in a wall of full-size puffs.
    float falloff = mix(1.0, Params.x, clamp(distXZ * distXZ / (Params.y * Params.y), 0.0, 1.0));

    // No wave/dynScale anymore: the cheap GLSL hash it used produced
    // faint diagonal stripes of same-size puffs, and the layer is meant
    // to be fully static anyway - all size variety now comes from the
    // CPU-hashed per-puff scale above.
    float width = Geometry.y * sizeScale * falloff;
    float height = Geometry.z * sizeScale * falloff;

    // Lift raises the box off the base plane, plus a small per-puff
    // vertical jitter (reusing the color-noise byte) so puffs never sit on
    // one perfectly shared plane.
    base.y = Origin.y + lift * Geometry.w + (t1.b - 0.5) * 6.0;

    vec3 v = vertices[face * 4 + corner] - vec3(0.5, 0.0, 0.5);
    vec3 pos = base + v * vec3(width, height, width);

    // Near fade (BC's NEAR_VISIBILITY): puffs at the camera dissolve
    // instead of clipping through it. Tight range (4..12 blocks) so
    // clouds stay solid until the player is nearly inside them.
    float dist = length(base);
    float nearFade = smoothstep(4.0, 12.0, dist);

    // Edge fade, BC-style quadratic (f*f), from edgeFadeStart out to the
    // patch radius.
    float edge = 1.0 - clamp((distXZ - Params.z) / (Params.y - Params.z), 0.0, 1.0);
    float edgeFade = edge * edge;

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    // Per-face light plus BC's per-puff color variance, so a bank reads as
    // lumpy masses instead of one flat white sheet.
    brightness = faceBrightness[face] * mix(0.88, 1.12, t1.b);
    fadeOpacity = nearFade * edgeFade * Params.w * density;
    vertexDistance = fog_spherical_distance(pos);
}
