package com.huwng.alterna.gravity;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.AlternaAttachments;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/**
 * API công khai để đọc/ghi trọng lực của entity.
 */
public class GravityApi {

    private static final GravityData DEFAULT_DATA = new GravityData(Direction.DOWN, 1.0);

    public static GravityData getData(Entity entity) {
        if (entity == null || !entity.hasData(AlternaAttachments.GRAVITY_DATA)) {
            return DEFAULT_DATA;
        }
        return entity.getData(AlternaAttachments.GRAVITY_DATA);
    }

    public static Direction getDirection(Entity entity) {
        if (entity == null) return Direction.DOWN;
        return getData(entity).getDirection();
    }

    public static void setDirection(Entity entity, Direction direction) {
        if (entity == null) return;
        Direction oldDir = getDirection(entity);
        double strength = getData(entity).getStrength();

        // Tạo instance mới để NeoForge Attachment System nhận biết thay đổi và tự động sync sang Client!
        GravityData newData = new GravityData(direction, strength);
        entity.setData(AlternaAttachments.GRAVITY_DATA, newData);

        Alterna.LOGGER.info("[GRAVITY-DEBUG] setDirection for {} (isClient={}): {} -> {}",
                entity.getName().getString(),
                entity.level() != null && entity.level().isClientSide(),
                oldDir,
                direction);
    }

    public static boolean hasCustomGravity(Entity entity) {
        return !getData(entity).isNormal();
    }
}
