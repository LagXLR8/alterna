package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.AlternaParticles;
import com.huwng.alterna.client.particle.EnchantParticle;
import com.huwng.alterna.enchantment.ModEnchantments;
import com.huwng.alterna.item.EnchantStoneItem;
import com.huwng.alterna.item.LunarTomeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import org.jspecify.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientEnchantStoneEvents {

    public record EnchantmentTintSource(int defaultColor) implements ItemTintSource {
        public static final MapCodec<EnchantmentTintSource> CODEC =
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default")
                        .xmap(EnchantmentTintSource::new, EnchantmentTintSource::defaultColor);

        public EnchantmentTintSource {
            defaultColor = ARGB.opaque(defaultColor);
        }

        private static final Map<ResourceKey<Enchantment>, Integer> ENCHANT_COLORS = new LinkedHashMap<>();
        static {
            ENCHANT_COLORS.put(ModEnchantments.CHILLING,  0x7EEDDC);
            ENCHANT_COLORS.put(ModEnchantments.ELASTIC,   0xFF69B4);
            ENCHANT_COLORS.put(ModEnchantments.GLUTTONY,  0xF5C953);
            ENCHANT_COLORS.put(ModEnchantments.WILD,      0xF3F3F3);
            ENCHANT_COLORS.put(ModEnchantments.VAMPIRISM, 0xFC90FD);
            ENCHANT_COLORS.put(ModEnchantments.HEROISM,  0xCCFF00);
            ENCHANT_COLORS.put(ModEnchantments.DEFERRED, 0x111111);
            ENCHANT_COLORS.put(ModEnchantments.DETONATION, 0xFF6600);
            ENCHANT_COLORS.put(ModEnchantments.STORMBREAKER, 0x0099FF);
            ENCHANT_COLORS.put(ModEnchantments.CURSE_OF_DROWNED_CAPTAIN, 0xFFD700);
            ENCHANT_COLORS.put(ModEnchantments.CURSE_OF_REJECTION, 0x00FFFF);
            ENCHANT_COLORS.put(ModEnchantments.CURSE_OF_BLOODLUST, 0xFF0000);
            ENCHANT_COLORS.put(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING, 0x111111);
        }

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            if (stack.isEmpty() || !com.huwng.alterna.Config.ENABLE_ENCHANTMENT_TINT.get()) return defaultColor;
            var regAccess = level != null ? level.registryAccess() : (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : null);
            if (regAccess == null) return defaultColor;

            var enchRegistry = regAccess.lookupOrThrow(Registries.ENCHANTMENT);
            for (Map.Entry<ResourceKey<Enchantment>, Integer> entry : ENCHANT_COLORS.entrySet()) {
                Optional<Holder.Reference<Enchantment>> holder = enchRegistry.get(entry.getKey());
                if (holder.isPresent() && stack.getEnchantmentLevel(holder.get()) > 0) {
                    return ARGB.opaque(entry.getValue());
                }
            }
            return defaultColor;
        }

        @Override
        public MapCodec<? extends ItemTintSource> type() {
            return CODEC;
        }
    }

    @EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
    public static class ModBusClientEvents {

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(AlternaParticles.ENCHANT.get(), EnchantParticle.EnchantParticleProvider::new);
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(com.huwng.alterna.entity.ModEntities.THROWN_STORMBREAKER.get(), com.huwng.alterna.client.render.ThrownStormbreakerRenderer::new);
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
            event.register(Alterna.id("enchantment_tint"), EnchantmentTintSource.CODEC);
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void registerRenderBuffers(net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent event) {

            com.huwng.alterna.client.render.ModGlintType.init();
            for (com.huwng.alterna.client.render.ModGlintType type : com.huwng.alterna.client.render.ModGlintType.values()) {
                if (type == com.huwng.alterna.client.render.ModGlintType.NONE) continue;
                if (type.getGlint() != null) event.registerRenderBuffer(type.getGlint());
                if (type.getGlintTranslucent() != null) event.registerRenderBuffer(type.getGlintTranslucent());
                if (type.getEntityGlint() != null) event.registerRenderBuffer(type.getEntityGlint());
            }
        }
    }

    @EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
    public static class GameBusClientEvents {

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            var mc = Minecraft.getInstance();
            if (mc.level == null) return;

            var reg = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            List<Component> tooltip = event.getToolTip();

            ResourceKey<?>[] describableKeys = {
                ModEnchantments.CHILLING, ModEnchantments.ELASTIC,
                ModEnchantments.GLUTTONY, ModEnchantments.VAMPIRISM,
                ModEnchantments.WILD, ModEnchantments.HEROISM,
                ModEnchantments.DEFERRED, ModEnchantments.DETONATION,
                ModEnchantments.STORMBREAKER,
                ModEnchantments.CURSE_OF_DROWNED_CAPTAIN,
                ModEnchantments.CURSE_OF_REJECTION,
                ModEnchantments.CURSE_OF_BLOODLUST,
                ModEnchantments.CURSE_OF_THE_NO_LIFE_KING
            };


            boolean addedSpace = false;

            for (ResourceKey<?> key : describableKeys) {
                @SuppressWarnings("unchecked")
                Optional<Holder.Reference<Enchantment>> holder = reg.get((ResourceKey<Enchantment>) key);
                if (holder.isPresent() && stack.getEnchantmentLevel(holder.get()) > 0) {
                    if (!addedSpace) {
                        tooltip.add(Component.empty());
                        addedSpace = true;
                    }

                    String descKey = "enchantment.alterna." + key.identifier().getPath() + ".desc";
                    Component descComp = Component.translatable(descKey);
                    String fullText = descComp.getString();

                    if (!fullText.equals(descKey)) {
                        String[] lines = fullText.split("\\|");
                        for (String line : lines) {
                            tooltip.add(Component.literal(line.trim())
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                        }
                    }
                }
            }

            // 2. Tooltips for Enchant Stone Items
            if (stack.getItem() instanceof EnchantStoneItem stone) {
                tooltip.add(Component.empty());
                ResourceKey<Enchantment> key = stone.getEnchantmentKey();
                String descKey = "enchantment.alterna." + key.identifier().getPath() + ".desc";
                Component descComp = Component.translatable(descKey);
                String fullText = descComp.getString();

                if (!fullText.equals(descKey)) {
                    String[] lines = fullText.split("\\|");
                    for (String line : lines) {
                        tooltip.add(Component.literal(line.trim())
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                    }
                }

                tooltip.add(Component.translatable("item.alterna.enchant_stone.anvil_tip")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }

            // 3. Tooltips for Lunar Tome Item
            if (stack.getItem() instanceof LunarTomeItem) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("item.alterna.lunar_tome.anvil_tip")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }
}
