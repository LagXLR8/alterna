package com.huwng.alterna.worldgen;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Registers where a huge stone spike may start generating, and the overall
 * bounding box it will need (including its lean and radius), then hands off
 * to HugeStoneSpikePiece to actually place blocks - piece by piece, one
 * chunk at a time - which is what lets a single spike safely span many
 * chunks without triggering "setBlock in a far chunk" warnings.
 */
public class HugeStoneSpikeStructure extends Structure {

    public static final MapCodec<HugeStoneSpikeStructure> CODEC = simpleCodec(HugeStoneSpikeStructure::new);

    public HugeStoneSpikeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        RandomSource random = context.random();

        // Grows straight down starting just below the vanilla bedrock layer
        // (see VoidFallHandler.VOID_START_Y = -64).
        int originY = -62;
        BlockPos origin = new BlockPos(chunkPos.getMiddleBlockX(), originY, chunkPos.getMiddleBlockZ());

        int length = HugeStoneSpikePiece.MIN_LENGTH
                + random.nextInt(HugeStoneSpikePiece.MAX_LENGTH - HugeStoneSpikePiece.MIN_LENGTH + 1);
        int baseRadius = HugeStoneSpikePiece.MIN_RADIUS
                + random.nextInt(HugeStoneSpikePiece.MAX_RADIUS - HugeStoneSpikePiece.MIN_RADIUS + 1);
        double taperPower = 1.1 + random.nextDouble() * 0.7;

        // Extreme lean: the drift distance can be well over the spike's own
        // length, so it reads as dramatically tilted.
        double leanAngle = random.nextDouble() * Math.PI * 2.0;
        double leanDistance = length * (0.5 + random.nextDouble() * 0.9);
        double totalDriftX = Math.cos(leanAngle) * leanDistance;
        double totalDriftZ = Math.sin(leanAngle) * leanDistance;

        // Per-layer radius can exceed baseRadius by up to ~15% due to jitter
        // (see HugeStoneSpikePiece), plus a small wobble offset. Undershooting
        // this margin means chunks holding the widest rings never get
        // postProcess called at all, which looks like the spike is "cut off".
        int margin = (int) Math.ceil(baseRadius * 1.2) + 6;
        int minX = origin.getX() + (int) Math.floor(Math.min(0.0, totalDriftX) - margin);
        int maxX = origin.getX() + (int) Math.ceil(Math.max(0.0, totalDriftX) + margin);
        int minZ = origin.getZ() + (int) Math.floor(Math.min(0.0, totalDriftZ) - margin);
        int maxZ = origin.getZ() + (int) Math.ceil(Math.max(0.0, totalDriftZ) + margin);
        int minY = origin.getY() - length - 2;
        int maxY = origin.getY() + 2;

        BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(
                new HugeStoneSpikePiece(origin, length, baseRadius, taperPower, leanAngle, leanDistance, box))));
    }

    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
    }

    @Override
    public StructureType<?> type() {
        return AlternaStructureTypes.HUGE_STONE_SPIKE.get();
    }
}
