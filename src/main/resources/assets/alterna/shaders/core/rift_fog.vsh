#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Fullscreen triangle for the rift height-fog pass (see rift_fog.fsh).
// vertex 0: (-1,-1), vertex 1: (3,-1), vertex 2: (-1,3) - one oversized
// triangle covering the whole screen, same trick vanilla post passes use
// with draw(0, 3).

flat out mat4 invViewProj;

void main() {
    vec2 pos = vec2(
        float((gl_VertexID & 1) << 2) - 1.0,
        float((gl_VertexID & 2) << 1) - 1.0);
    gl_Position = vec4(pos, 0.0, 1.0);

    // Inverted once per vertex (3x per frame), not per fragment. ModelView
    // is the level's rotation-only view matrix, so unprojected positions
    // come out camera-relative in world orientation.
    invViewProj = inverse(ProjMat * ModelViewMat);
}
