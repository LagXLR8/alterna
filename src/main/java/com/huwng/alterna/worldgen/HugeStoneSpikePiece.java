package com.huwng.alterna.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.StructureManager;

/**
 * Places one giant, tilted, one-pointed stone spike that hangs from just
 * below the vanilla bedrock layer and tapers to a hot glowing point.
 *
 * IMPORTANT: postProcess() is called once per chunk that this piece's
 * bounding box overlaps (that's how vanilla structures safely span more
 * than one chunk). Every call must reproduce the exact same shape - so all
 * per-layer randomness is derived from a deterministic seed (origin + layer
 * index), never from the RandomSource Minecraft happens to pass in, which
 * differs between calls. Every setBlock is also checked against chunkBB so
 * we only ever touch the chunk currently being processed.
 */
public class HugeStoneSpikePiece extends StructurePiece {

    static final int MIN_LENGTH = 120;
    static final int MAX_LENGTH = 300;
    static final int MIN_RADIUS = 20;
    static final int MAX_RADIUS = 40;

    // How far along the spike (0 = top, 1 = tip) the hot gradient begins.
    private static final double HOT_GRADIENT_START = 0.2;

    private static final BlockState[] ROCK_PALETTE = new BlockState[] {
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.TUFF.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(),
    };

    private static final BlockState[] HOT_PALETTE = new BlockState[] {
            Blocks.MAGMA_BLOCK.defaultBlockState(),
            Blocks.MAGMA_BLOCK.defaultBlockState(),
            Blocks.MAGMA_BLOCK.defaultBlockState(),
            Blocks.SHROOMLIGHT.defaultBlockState(),
            Blocks.SHROOMLIGHT.defaultBlockState(),
            Blocks.RESIN_BLOCK.defaultBlockState(),
            Blocks.RESIN_BLOCK.defaultBlockState(),
    };

    private final BlockPos origin;
    private final int length;
    private final int baseRadius;
    private final double taperPower;
    private final double leanAngle;
    private final double leanDistance;

    public HugeStoneSpikePiece(BlockPos origin, int length, int baseRadius, double taperPower,
            double leanAngle, double leanDistance, BoundingBox box) {
        super(AlternaStructurePieceTypes.HUGE_STONE_SPIKE.get(), 0, box);
        this.origin = origin;
        this.length = length;
        this.baseRadius = baseRadius;
        this.taperPower = taperPower;
        this.leanAngle = leanAngle;
        this.leanDistance = leanDistance;
    }

    public HugeStoneSpikePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(AlternaStructurePieceTypes.HUGE_STONE_SPIKE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", -63), tag.getIntOr("OZ", 0));
        this.length = tag.getIntOr("Length", HugeStoneSpikePiece.MIN_LENGTH);
        this.baseRadius = tag.getIntOr("BaseRadius", HugeStoneSpikePiece.MIN_RADIUS);
        this.taperPower = tag.getDoubleOr("TaperPower", 1.3);
        this.leanAngle = tag.getDoubleOr("LeanAngle", 0.0);
        this.leanDistance = tag.getDoubleOr("LeanDistance", 0.0);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putInt("Length", length);
        tag.putInt("BaseRadius", baseRadius);
        tag.putDouble("TaperPower", taperPower);
        tag.putDouble("LeanAngle", leanAngle);
        tag.putDouble("LeanDistance", leanDistance);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos pos) {

        double totalDriftX = Math.cos(leanAngle) * leanDistance;
        double totalDriftZ = Math.sin(leanAngle) * leanDistance;
        LevelHeightAccessor heightAccessor = level;

        for (int i = 0; i < length; i++) {
            int y = origin.getY() - i;
            if (y < heightAccessor.getMinY() + 4 || y > heightAccessor.getMaxY() - 4) {
                break;
            }
            // Cheap vertical reject before doing any per-layer work.
            if (y < chunkBB.minY() - 1 || y > chunkBB.maxY() + 1) {
                continue;
            }

            double t = length <= 1 ? 1.0 : (double) i / (double) (length - 1);
            double profile = Math.pow(1.0 - t, taperPower);

            // Deterministic per-layer randomness so every chunk this piece
            // touches reproduces the exact same shape at the seams.
            RandomSource layerRandom = RandomSource.create(mixSeed(origin, i));
            double jitter = 0.85 + layerRandom.nextDouble() * 0.3;
            int radius = (int) Math.round(baseRadius * profile * jitter);

            double wobbleX = (layerRandom.nextDouble() - 0.5) * 0.4;
            double wobbleZ = (layerRandom.nextDouble() - 0.5) * 0.4;
            double layerCenterX = origin.getX() + totalDriftX * t + wobbleX;
            double layerCenterZ = origin.getZ() + totalDriftZ * t + wobbleZ;
            int cx = (int) Math.round(layerCenterX);
            int cz = (int) Math.round(layerCenterZ);

            // Cheap horizontal reject: skip the whole layer if its bounding
            // circle can't possibly touch the chunk we're processing.
            if (cx + radius < chunkBB.minX() - 1 || cx - radius > chunkBB.maxX() + 1
                    || cz + radius < chunkBB.minZ() - 1 || cz - radius > chunkBB.maxZ() + 1) {
                continue;
            }

            double hotProportion = 0.0;
            if (t > HOT_GRADIENT_START) {
                hotProportion = Math.min(1.0, Math.max(0.0, (t - HOT_GRADIENT_START) / (1.0 - HOT_GRADIENT_START)));
            }

            if (radius > 0) {
                placeLayer(level, layerRandom, cx, y, cz, radius, hotProportion, chunkBB);
            } else if (t >= 0.999) {
                BlockPos tipPos = new BlockPos(cx, y, cz);
                if (chunkBB.isInside(tipPos)) {
                    BlockState existing = level.getBlockState(tipPos);
                    if (existing.isAir() || existing.getFluidState().isSource()) {
                        level.setBlock(tipPos, HOT_PALETTE[layerRandom.nextInt(HOT_PALETTE.length)], 2);
                    }
                }
            }
        }
    }

    private void placeLayer(WorldGenLevel level, RandomSource random, int cx, int y, int cz, int radius,
            double hotProportion, BoundingBox chunkBB) {
        int radiusSq = radius * radius;
        double raggedThreshold = radiusSq * 0.75D;

        for (int dx = -radius; dx <= radius; dx++) {
            int x = cx + dx;
            if (x < chunkBB.minX() || x > chunkBB.maxX()) {
                continue;
            }
            for (int dz = -radius; dz <= radius; dz++) {
                int z = cz + dz;
                if (z < chunkBB.minZ() || z > chunkBB.maxZ()) {
                    continue;
                }
                int distSq = dx * dx + dz * dz;
                if (distSq > radiusSq) {
                    continue;
                }
                if (distSq > raggedThreshold && random.nextFloat() < 0.35F) {
                    continue;
                }

                BlockPos blockPos = new BlockPos(x, y, z);
                BlockState existing = level.getBlockState(blockPos);
                if (!existing.isAir() && !existing.getFluidState().isSource()) {
                    // Don't bulldoze existing terrain/structures.
                    continue;
                }

                BlockState chosen = (hotProportion > 0.0 && random.nextDouble() < hotProportion)
                        ? HOT_PALETTE[random.nextInt(HOT_PALETTE.length)]
                        : ROCK_PALETTE[random.nextInt(ROCK_PALETTE.length)];
                level.setBlock(blockPos, chosen, 2);
            }
        }
    }

    private static long mixSeed(BlockPos origin, int layer) {
        return origin.asLong() * 31L + layer;
    }
}
