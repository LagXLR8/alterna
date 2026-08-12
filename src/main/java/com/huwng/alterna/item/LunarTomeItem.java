package com.huwng.alterna.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class LunarTomeItem extends Item {
    private static final String[] WHISPER_MESSAGES = {
        "It longs to return to the deep ocean… back to where it was born.",
        "It's restless… it needs to return to the deep ocean before it wakes.",
        "The book whispers to go home… to the cold, deep ocean.",
        "Its path ends where the moonlight cannot reach… in the deep ocean.",
        "This tome yearns for the depths. It was never meant to remain here."
    };

    public LunarTomeItem(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            String message = WHISPER_MESSAGES[level.getRandom().nextInt(WHISPER_MESSAGES.length)];
            player.sendSystemMessage(Component.literal(message));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f + level.getRandom().nextFloat() * 0.4f);

            player.getCooldowns().addCooldown(stack, 40);
        }

        return InteractionResult.SUCCESS;
    }
}
