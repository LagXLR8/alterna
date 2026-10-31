package com.huwng.alterna.client.scanner;

import com.huwng.alterna.scanner.ScanEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The holographic "skin" laid over a scanned plant: every block of the plant (a whole tree for
 * the big ones, or the clump/column for small plants) is found with a flood fill, and the
 * outward-facing faces of their shapes become the quads the scanner overlay is drawn on.
 */
final class PlantShell {
    /** Each quad: 4 corners (x,y,z) then the face normal (nx,ny,nz) = 15 floats. */
    final List<float[]> quads = new ArrayList<>();
    final BlockPos origin;
    final ScanEntries.Entry entry;

    private PlantShell(BlockPos origin, ScanEntries.Entry entry) {
        this.origin = origin;
        this.entry = entry;
    }

    /** Big trees are scanned as a whole; small plants just as their clump or column. */
    private static boolean wholeStructure(ScanEntries.Entry e) {
        return switch (e.id()) {
            case "weavecap", "canopra", "goblet_tree", "rootshroom" -> true;
            default -> false;
        };
    }

    static PlantShell build(Level level, BlockPos start, ScanEntries.Entry entry) {
        PlantShell shell = new PlantShell(start.immutable(), entry);
        boolean whole = wholeStructure(entry);
        int maxBlocks = whole ? 4000 : 32;
        int radius = whole ? 40 : 4;
        BlockState startState = level.getBlockState(start);
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        found.add(start.immutable());
        while (!queue.isEmpty() && found.size() < maxBlocks) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos n = p.offset(dx, dy, dz);
                if (found.contains(n) || n.distManhattan(start) > radius * 2 || Math.abs(n.getY() - start.getY()) > radius * 2) continue;
                BlockState s = level.getBlockState(n);
                boolean member = whole
                        ? ScanEntries.forBlock(BuiltInRegistries.BLOCK.getKey(s.getBlock())) == entry
                        : s.getBlock() == startState.getBlock();
                if (!member) continue;
                BlockPos im = n.immutable();
                found.add(im);
                queue.add(im);
            }
        }
        for (BlockPos p : found) {
            BlockState s = level.getBlockState(p);
            VoxelShape shape = s.getShape(level, p);
            AABB b = (shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds()).move(p).inflate(0.012);
            boolean full = shape.isEmpty() || isFullCube(shape);
            for (Direction d : Direction.values()) {
                // inner faces between two blocks of the same plant are skipped (only for full cubes;
                // small plant shapes always show all their sides)
                if (full && found.contains(p.relative(d)) && isFullCube(level.getBlockState(p.relative(d)).getShape(level, p.relative(d)))) continue;
                shell.quads.add(face(b, d));
            }
        }
        return shell;
    }

    private static boolean isFullCube(VoxelShape shape) {
        if (shape.isEmpty()) return false;
        AABB b = shape.bounds();
        return b.minX <= 0.001 && b.minY <= 0.001 && b.minZ <= 0.001 && b.maxX >= 0.999 && b.maxY >= 0.999 && b.maxZ >= 0.999;
    }

    private static float[] face(AABB b, Direction d) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ, x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        return switch (d) {
            case UP -> new float[]{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0};
            case DOWN -> new float[]{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0};
            case NORTH -> new float[]{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1};
            case SOUTH -> new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1};
            case WEST -> new float[]{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0};
            case EAST -> new float[]{x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0};
        };
    }
}
