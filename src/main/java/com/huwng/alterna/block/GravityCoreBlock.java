package com.huwng.alterna.block;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityCoreTracker;
import com.huwng.alterna.gravity.GravityCoreTransitionHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Khối "lõi trọng lực" hình lập phương thay cho GravityInvertBlock cũ.
 * <p>
 * Khác với GravityInvertBlock (chỉ có 1 hướng FACING cố định, và bị lỗi
 * stepOn/useWithoutItem set 2 hướng khác nhau), khối này KHÔNG có state hướng:
 * đứng lên mặt nào thì trọng lực kéo entity về phía mặt đó (mặt entity đang
 * đứng luôn là "dưới chân"), và khi đi tới mép của khối, GravityCoreTransitionHandler
 * sẽ tự xoay entity mượt sang mặt kế tiếp — cho phép đi vòng quanh trọn cả 6 mặt
 * giống một hành tinh nhỏ, thay vì phải đặt nhiều block GravityInvertBlock quay
 * các hướng khác nhau và chấp nhận việc lật đột ngột ở ranh giới.
 */
public class GravityCoreBlock extends Block {

    public static final MapCodec<GravityCoreBlock> CODEC = simpleCodec(GravityCoreBlock::new);

    public GravityCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        // Ghi nhớ "mình vừa đứng lên core này ở tick nào" để
        // tryTransitionOnFall() vẫn còn tác dụng trong vài tick sau khi rời
        // mặt đất (ví dụ nhảy/bước hụt ngay tại mép).
        if (entity instanceof GravityCoreTracker tracker) {
            tracker.alterna$rememberGravityCore(pos, entity.tickCount);
        }

        // DEBUG TẠM THỜI: log 1 lần/giây để soi xem onGround/isSupportedBy có
        // đúng không trên các mặt không phải DOWN. Xoá khối if này sau khi hết
        // debug.
        if (entity instanceof Player && entity.tickCount % 20 == 0) {
            Alterna.LOGGER.info(
                    "[gravitycore-debug] dir={} onGround={} isSupportedBy={} pos={} entityPos={}",
                    GravityApi.getDirection(entity), entity.onGround(), entity.isSupportedBy(pos), pos, entity.position()
            );
        }

        GravityCoreTransitionHandler.tryTransition(level, pos, livingEntity);
    }
}
