#version 330

#moj_import <minecraft:fog.glsl>

// See rift_clouds.vsh. One fragment only shades one small box face; the
// soft volumetric look comes from thousands of translucent boxes blending
// on top of each other (Better Clouds' coverage-accumulation idea,
// approximated by plain translucent stacking with a moderate per-puff
// opacity).

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec4 Geometry;
    vec4 Origin;
    vec4 Params;
};

in float brightness;
in float fadeOpacity;
in float vertexDistance;

out vec4 fragColor;

void main() {
    vec4 color = CloudColor;
    color.rgb *= brightness;
    color.a *= fadeOpacity;

    // Kept for parity with the vanilla cloud shader, but the renderer
    // binds its own Fog UBO with FogCloudsEnd pushed out to ~infinity -
    // the player's render-distance fog must not eat the far half of the
    // patch (that was the earlier "can't render far" bug).
    color.a *= 1.0 - linear_fog_value(vertexDistance, 0, FogCloudsEnd);

    fragColor = color;
}
