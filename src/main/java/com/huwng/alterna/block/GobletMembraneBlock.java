package com.huwng.alterna.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Goblet Membrane — giống HoneyBlock (sticky/slow, giảm fall damage)
 *
 * CƠ CHẾ SÓNG CẢM ỨNG (Stable Ripple Mechanics):
 *  - Mặc định: STAGE 0 (goblet_membrane)
 *  - Người chơi bước vào vùng 3.5 block -> Các khối xung quanh DUY TRÌ STAGE 1 (goblet_membrane_0)
 *  - Người chơi giẫm trực tiếp (≤1.5 block) -> Khối dưới chân DUY TRÌ STAGE 2 (goblet_membrane_1)
 *  - Giữ vững trạng thái KHÔNG NHÁY VỀ 0 chừng nào người chơi còn trong bán kính.
 *  - Chỉ chuyển mượt về STAGE 0 khi người chơi đã rời khỏi bán kính 3.5 block.
 */
public class GobletMembraneBlock extends HalfTransparentBlock {

    public static final MapCodec<GobletMembraneBlock> CODEC = simpleCodec(GobletMembraneBlock::new);

    /** 0 = mặc định, 1 = bán kính xung quanh, 2 = vị trí giẫm trực tiếp */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);

    /** Chu kỳ kiểm tra duy trì hiệu ứng (20 tick = 1.0s) */
    private static final int CHECK_DELAY_TICKS = 20;

    // --- HoneyBlock slide constants ---
    private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
    private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

    public GobletMembraneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    public MapCodec<GobletMembraneBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    // ── Collision: dùng VoxelShape của HoneyBlock ──

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ── Fall damage ──

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            level.broadcastEntityEvent(entity, (byte) 54);
        }
        if (entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall())) {
            entity.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
        }
    }

    // ── Interaction: Trực tiếp giẫm (stepOn) hoặc chạm vào (entityInside) ──

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof Player && !level.isClientSide()) {
            triggerRippleEffect(level, pos);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof Player && !level.isClientSide()) {
            triggerRippleEffect(level, pos);
        }
        if (isSlidingDown(pos, entity)) {
            maybeDoSlideAchievement(entity, pos);
            doSlideMovement(entity);
            if (doesEntityDoHoneyBlockSlideEffects(entity)) {
                RandomSource random = level.getRandom();
                if (random.nextInt(5) == 0) {
                    entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
                }
                if (!level.isClientSide() && random.nextInt(5) == 0) {
                    level.broadcastEntityEvent(entity, (byte) 53);
                }
            }
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    private static boolean doesEntityDoHoneyBlockSlideEffects(Entity entity) {
        return entity instanceof LivingEntity
                || entity instanceof AbstractMinecart
                || entity instanceof PrimedTnt
                || entity instanceof AbstractBoat;
    }

    private boolean isSlidingDown(BlockPos pos, Entity entity) {
        if (entity.onGround()) return false;
        if (entity.getY() > pos.getY() + 0.9375 - 1.0E-7) return false;
        double oldDeltaY = entity.getDeltaMovement().y / 0.98F + 0.08;
        if (oldDeltaY >= -0.08) return false;
        double dx = Math.abs(pos.getX() + 0.5 - entity.getX());
        double dz = Math.abs(pos.getZ() + 0.5 - entity.getZ());
        double overlap = 0.4375 + entity.getBbWidth() / 2.0F;
        return dx + 1.0E-7 > overlap || dz + 1.0E-7 > overlap;
    }

    private void maybeDoSlideAchievement(Entity entity, BlockPos pos) {
        if (entity instanceof ServerPlayer sp && entity.level().getGameTime() % 20L == 0L) {
            CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger(sp, entity.level().getBlockState(pos));
        }
    }

    private void doSlideMovement(Entity entity) {
        Vec3 dm = entity.getDeltaMovement();
        double oldDeltaY = dm.y / 0.98F + 0.08;
        double newDeltaY = (-0.05 - 0.08) * 0.98F;
        if (oldDeltaY < -SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST) {
            double factor = -THROTTLE_SLIDE_SPEED_TO / oldDeltaY;
            entity.setDeltaMovement(new Vec3(dm.x * factor, newDeltaY, dm.z * factor));
        } else {
            entity.setDeltaMovement(new Vec3(dm.x, newDeltaY, dm.z));
        }
        entity.resetFallDistance();
    }

    // ── Kích hoạt hiệu ứng gợn sóng & Kiểm tra ổn định trạng thái ──

    /**
     * Kích hoạt gợn sóng khi người chơi tương tác:
     *  - Khối tại vị trí giẫm trực tiếp (≤1.5b) -> STAGE 2
     *  - Các khối xung quanh bán kính hình tròn (≤3.5b) -> STAGE 1
     */
    private void triggerRippleEffect(Level level, BlockPos center) {
        int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);

                    if (st.getBlock() == this) {
                        int desired = getDesiredStage(level, p);
                        if (desired > 0) {
                            if (st.getValue(STAGE) != desired) {
                                level.setBlock(p, st.setValue(STAGE, desired), 3);
                            }
                            level.scheduleTick(p, this, CHECK_DELAY_TICKS);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentStage = state.getValue(STAGE);
        if (currentStage > 0) {
            int desired = getDesiredStage(level, pos);

            // Cập nhật state chính xác theo vị trí hiện tại của người chơi
            if (desired != currentStage) {
                level.setBlock(pos, state.setValue(STAGE, desired), 3);
            }

            // Nếu người chơi vẫn ở gần (desired = 1 hoặc 2), tiếp tục duy trì KHÔNG RESET VỀ 0
            if (desired > 0) {
                level.scheduleTick(pos, this, CHECK_DELAY_TICKS);
            }
        }
    }

    /**
     * Tính toán chính xác STAGE mong muốn cho khối tại pos dựa trên vị trí các người chơi gần nhất:
     *  - Có người chơi trực tiếp giẫm/chạm (bán kính ≤1.5 block) -> STAGE 2
     *  - Có người chơi ở khu vực xung quanh (bán kính ≤3.5 block) -> STAGE 1
     *  - Không có người chơi nào trong bán kính 3.5 block -> STAGE 0 (mặc định)
     */
    private int getDesiredStage(Level level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // Kiểm tra người chơi ở khoảng cách giẫm trực tiếp (STAGE 2)
        List<Player> directPlayers = level.getEntitiesOfClass(Player.class,
                new AABB(cx - 1.5, cy - 1.5, cz - 1.5, cx + 1.5, cy + 1.5, cz + 1.5));
        if (!directPlayers.isEmpty()) {
            return 2;
        }

        // Kiểm tra người chơi ở bán kính xung quanh (STAGE 1)
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class,
                new AABB(cx - 3.5, cy - 2.0, cz - 3.5, cx + 3.5, cy + 2.0, cz + 3.5));
        if (!nearbyPlayers.isEmpty()) {
            return 1;
        }

        return 0;
    }
}
