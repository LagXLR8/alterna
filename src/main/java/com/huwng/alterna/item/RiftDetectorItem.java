package com.huwng.alterna.item;

import com.huwng.alterna.worldgen.GiantCrackParams;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class RiftDetectorItem extends Item {
    public RiftDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            BlockPos playerPos = player.blockPosition();
            BlockPos nearest = GiantCrackParams.findNearestCrack(level, playerPos, 4);

            if (nearest != null) {
                int dx = nearest.getX() - playerPos.getX();
                int dz = nearest.getZ() - playerPos.getZ();
                int dist = (int) Math.sqrt(dx * dx + dz * dz);

                double angle = Math.toDegrees(Math.atan2(-dx, dz));
                if (angle < 0) angle += 360;

                String dir;
                if (angle >= 337.5 || angle < 22.5) dir = "Nam";
                else if (angle >= 22.5 && angle < 67.5) dir = "Tây Nam";
                else if (angle >= 67.5 && angle < 112.5) dir = "Tây";
                else if (angle >= 112.5 && angle < 157.5) dir = "Tây Bắc";
                else if (angle >= 157.5 && angle < 202.5) dir = "Bắc";
                else if (angle >= 202.5 && angle < 247.5) dir = "Đông Bắc";
                else if (angle >= 247.5 && angle < 292.5) dir = "Đông";
                else dir = "Đông Nam";

                player.sendSystemMessage(Component.literal("§a[Vết Nứt] §fVết nứt gần nhất tại §eX: " + nearest.getX() + ", Z: " + nearest.getZ() + " §f(Hướng §b" + dir + "§f, cách §c" + dist + "m§f)"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            } else {
                player.sendSystemMessage(Component.literal("§cKhông tìm thấy vết nứt nào gần đây!"));
            }
        }

        return InteractionResult.SUCCESS;
    }
}
