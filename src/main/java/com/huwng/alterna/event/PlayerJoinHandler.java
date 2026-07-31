package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.nio.file.Path;

@EventBusSubscriber(modid = Alterna.MODID)
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (Config.SHOW_DEV_WARNING.get()) {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve("alterna-common.toml");

            MutableComponent alternaComponent = Component.literal("Alterna")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            MutableComponent configComponent = Component.literal("config")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenFile(configPath))
                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("alterna.welcome_warning.config_hover")))
                    );

            MutableComponent welcomeMsg = Component.translatable("alterna.welcome_warning", alternaComponent, configComponent)
                    .withStyle(ChatFormatting.BOLD);

            player.sendSystemMessage(welcomeMsg);
        }
    }
}
