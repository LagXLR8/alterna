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
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return;
        Direction oldDir = getDirection(entity);
        if (oldDir == direction) return;
        double strength = getData(entity).getStrength();

        // Tạo instance mới để NeoForge Attachment System nhận biết thay đổi và tự động sync sang Client.
        // Yaw/pitch adjustment xảy ra client-side trong LocalPlayerMixin khi client detect direction change.
        GravityData newData = new GravityData(direction, oldDir, strength);
        entity.setData(AlternaAttachments.GRAVITY_DATA, newData);

        Alterna.LOGGER.debug("[GRAVITY] setDirection {} -> {}", oldDir, direction);
    }

    public static boolean hasCustomGravity(Entity entity) {
        return !getData(entity).isNormal();
    }
}
