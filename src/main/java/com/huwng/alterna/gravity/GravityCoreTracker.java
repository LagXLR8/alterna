package com.huwng.alterna.gravity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Trạng thái tạm thời (server-side, không cần lưu/sync) mà mỗi Entity cần để
 * GravityCoreTransitionHandler có thể phát hiện lúc nào entity đi qua mép của
 * một GravityCoreBlock (kể cả khi đang rơi tự do gần mép, không chỉ lúc đứng yên).
 * <p>
 * Được implement bởi {@code EntityMixin} (unique fields), không phải attachment,
 * vì dữ liệu này chỉ tồn tại trong vài tick và không cần đồng bộ/persist.
 */
public interface GravityCoreTracker {

    /** Delta di chuyển "dự định" (trước va chạm) của tick gần nhất. */
    Vec3 alterna$getLastMoveDelta();

    /** Ghi nhớ vị trí GravityCoreBlock mà entity vừa đứng lên, cùng tick hiện tại. */
    void alterna$rememberGravityCore(BlockPos pos, int tick);

    /** Có thể trả về null nếu entity không đứng gần GravityCoreBlock nào gần đây. */
    BlockPos alterna$getLastGravityCore();

    int alterna$getLastGravityCoreTick();

    void alterna$clearGravityCore();

    /** Tick gần nhất mà entity vừa hoàn thành 1 lần đổi mặt qua GravityCoreBlock. */
    void alterna$setLastTransitionTick(int tick);

    int alterna$getLastTransitionTick();
}
