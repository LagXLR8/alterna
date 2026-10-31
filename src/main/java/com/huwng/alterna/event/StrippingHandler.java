package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Lets axes strip all of Alterna's logs and wood (Goblet, Canopra, Weavecap).
 * Previously none of the mod's logs could be stripped in survival.
 */
@EventBusSubscriber(modid = Alterna.MODID)
public final class StrippingHandler {
    private static Map<Block, Supplier<? extends Block>> map;

    private static Map<Block, Supplier<? extends Block>> map() {
        if (map == null) {
            map = Map.of(
                    ModBlocks.GOBLET_LOG.get(), ModBlocks.GOBLET_STRIPPED_LOG,
                    ModBlocks.GOBLET_WOOD.get(), ModBlocks.GOBLET_STRIPPED_WOOD,
                    ModBlocks.CANOPRA_LOG.get(), ModBlocks.STRIPPED_CANOPRA_LOG,
                    ModBlocks.CANOPRA_WOOD.get(), ModBlocks.STRIPPED_CANOPRA_WOOD,
                    ModBlocks.WEAVECAP_LOG.get(), ModBlocks.STRIPPED_WEAVECAP_LOG,
                    ModBlocks.WEAVECAP_WOOD.get(), ModBlocks.STRIPPED_WEAVECAP_WOOD);
        }
        return map;
    }

    @SubscribeEvent
    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        BlockState state = event.getState();
        Supplier<? extends Block> stripped = map().get(state.getBlock());
        if (stripped == null) return;
        BlockState result = stripped.get().defaultBlockState();
        if (state.hasProperty(RotatedPillarBlock.AXIS) && result.hasProperty(RotatedPillarBlock.AXIS)) {
            result = result.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
        }
        event.setFinalState(result);
    }

    private StrippingHandler() {}
}
