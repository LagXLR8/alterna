package com.huwng.alterna.item;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alterna.MODID);

    public static final DeferredItem<RiftDetectorItem> RIFT_DETECTOR = registerItem("rift_detector",
            RiftDetectorItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlankRiftMapItem> BLANK_RIFT_MAP = registerItem("blank_rift_map",
            BlankRiftMapItem::new, new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> VINE_ROPE = registerItem("vine_rope",
            Item::new, new Item.Properties().stacksTo(16));

    public static final DeferredItem<BlockItem> PURPLE_SUGAR_CANE = registerItem("purple_sugar_cane",
            p -> new BlockItem(ModBlocks.PURPLE_SUGAR_CANE.get(), p),
            new Item.Properties());

    public static final DeferredItem<BlockItem> CLOUD_BERRIES = registerItem("cloud_berries",
            p -> new BlockItem(ModBlocks.CLOUDBERRY_VINES.get(), p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    public static final DeferredItem<BlockItem> WHITE_CURRANT_BERRIES = registerItem("white_currant_berries",
            p -> new BlockItem(ModBlocks.WHITE_CURRANT_BERRY_BUSH.get(), p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    public static final DeferredItem<StandingAndWallBlockItem> ENOKI_MUSHROOM = registerItem("enoki_mushroom",
            p -> new StandingAndWallBlockItem(ModBlocks.ENOKI_MUSHROOM.get(), ModBlocks.ENOKI_MUSHROOM_WALL.get(), Direction.DOWN, p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));

    public static final DeferredItem<BlockItem> VITALROOT = registerItem("vitalroot",
            p -> new BlockItem(ModBlocks.VITALROOT.get(), p),
            new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(-3).saturationModifier(0.0F).alwaysEdible().build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON, 200, 0)))
                            .build()
            ));

    public static final DeferredItem<Item> COOKED_VITALROOT = registerItem("cooked_vitalroot",
            Item::new,
            new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(10).saturationModifier(0.4F).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0)))
                            .build()
            ));

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CLIMBING_ZOMBIE_SPAWN_EGG = registerItem("climbing_zombie_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(com.huwng.alterna.entity.ModEntities.CLIMBING_ZOMBIE.get(), new net.minecraft.nbt.CompoundTag()))),
            new Item.Properties());

    public static final DeferredItem<ChillingEnchantStoneItem> CHILLING_ENCHANT_STONE = registerItem("chilling_enchant_stone",
            ChillingEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<ElasticEnchantStoneItem> ELASTIC_ENCHANT_STONE = registerItem("elastic_enchant_stone",
            ElasticEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<GluttonyEnchantStoneItem> GLUTTONY_ENCHANT_STONE = registerItem("gluttony_enchant_stone",
            GluttonyEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<VampirismEnchantStoneItem> VAMPIRISM_ENCHANT_STONE = registerItem("vampirism_enchant_stone",
            VampirismEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<WildEnchantStoneItem> WILD_ENCHANT_STONE = registerItem("wild_enchant_stone",
            WildEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<HeroismEnchantStoneItem> HEROISM_ENCHANT_STONE = registerItem("heroism_enchant_stone",
            HeroismEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<DeferredEnchantStoneItem> DEFERRED_ENCHANT_STONE = registerItem("deferred_enchant_stone",
            DeferredEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<DetonationEnchantStoneItem> DETONATION_ENCHANT_STONE = registerItem("detonation_enchant_stone",
            DetonationEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<StormbreakerEnchantStoneItem> STORMBREAKER_ENCHANT_STONE = registerItem("stormbreaker_enchant_stone",
            StormbreakerEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<CurseOfDrownedCaptainEnchantStoneItem> CURSE_OF_DROWNED_CAPTAIN_ENCHANT_STONE = registerItem("curse_of_drowned_captain_enchant_stone",
            CurseOfDrownedCaptainEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<CurseOfRejectionEnchantStoneItem> CURSE_OF_REJECTION_ENCHANT_STONE = registerItem("curse_of_rejection_enchant_stone",
            CurseOfRejectionEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<CurseOfBloodlustEnchantStoneItem> CURSE_OF_BLOODLUST_ENCHANT_STONE = registerItem("curse_of_bloodlust_enchant_stone",
            CurseOfBloodlustEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<CurseOfTheNoLifeKingEnchantStoneItem> CURSE_OF_THE_NO_LIFE_KING_ENCHANT_STONE = registerItem("curse_of_the_no_life_king_enchant_stone",
            CurseOfTheNoLifeKingEnchantStoneItem::new, new Item.Properties());

    public static final DeferredItem<LunarTomeItem> LUNAR_TOME = registerItem("lunar_tome",
            LunarTomeItem::new, new Item.Properties());




    // =====================================================================
    // ---- LICHEN SAVANNA ----
    // =====================================================================
    /** Tiny glowing mushrooms: a snack that lets you see in the dark for a short while. */
    public static final DeferredItem<BlockItem> EMBER_CAP = registerItem("ember_cap",
            p -> new BlockItem(ModBlocks.EMBER_CAP.get(), p),
            new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0)))
                            .build()));

    /** Shed by Cragbacks from time to time - their thick back armour. */
    public static final DeferredItem<Item> CRAG_PLATE = registerItem("crag_plate",
            Item::new, new Item.Properties());

    public static final DeferredItem<Item> RAW_CRAG_SHANK = registerItem("raw_crag_shank",
            Item::new, new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()));

    public static final DeferredItem<Item> COOKED_CRAG_SHANK = registerItem("cooked_crag_shank",
            Item::new, new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(9).saturationModifier(0.9F).build()));

    public static final DeferredItem<Item> TETRA_EGG = registerItem("tetra_egg",
            Item::new, new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> CRAGPLATE_HELMET = registerItem("cragplate_helmet",
            Item::new, () -> new Item.Properties().humanoidArmor(ModArmorMaterials.CRAGPLATE, net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final DeferredItem<Item> CRAGPLATE_CHESTPLATE = registerItem("cragplate_chestplate",
            Item::new, () -> new Item.Properties().humanoidArmor(ModArmorMaterials.CRAGPLATE, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> CRAGPLATE_LEGGINGS = registerItem("cragplate_leggings",
            Item::new, () -> new Item.Properties().humanoidArmor(ModArmorMaterials.CRAGPLATE, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final DeferredItem<Item> CRAGPLATE_BOOTS = registerItem("cragplate_boots",
            Item::new, () -> new Item.Properties().humanoidArmor(ModArmorMaterials.CRAGPLATE, net.minecraft.world.item.equipment.ArmorType.BOOTS));

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CRAGBACK_SPAWN_EGG = registerItem("cragback_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(com.huwng.alterna.entity.ModEntities.CRAGBACK.get(), new net.minecraft.nbt.CompoundTag()))),
            new Item.Properties());

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> TETRA_SPAWN_EGG = registerItem("tetra_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(com.huwng.alterna.entity.ModEntities.TETRA.get(), new net.minecraft.nbt.CompoundTag()))),
            new Item.Properties());

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ARMOR_OVERLORD_SPAWN_EGG = registerItem("armor_overlord_spawn_egg",
            p -> new net.minecraft.world.item.SpawnEggItem(p.component(net.minecraft.core.component.DataComponents.ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(com.huwng.alterna.entity.ModEntities.ARMOR_OVERLORD.get(), new net.minecraft.nbt.CompoundTag()))),
            new Item.Properties());

    // ---- Armor Overlord trophies
    /** One of the Overlord's hooked blades. */
    public static final DeferredItem<Item> OVERLORD_HOOK = registerItem("overlord_hook", Item::new, new Item.Properties());
    /** A piece of the Overlord's mushroom-cap carapace. */
    public static final DeferredItem<Item> CAPSHELL_FRAGMENT = registerItem("capshell_fragment", Item::new, new Item.Properties());

    public static final DeferredItem<GrappleItem> OVERLORD_GRAPPLE = registerItem("overlord_grapple", GrappleItem::new,
            () -> new Item.Properties().durability(256).repairable(OVERLORD_HOOK.get()));

    /**
     * Capshell Shield: a shield cut from the Overlord's cap. Blocks like a shield but over a
     * wider arc against projectiles, wears more slowly and lasts much longer.
     */
    public static final DeferredItem<net.minecraft.world.item.ShieldItem> CAPSHELL_SHIELD = registerItem("capshell_shield",
            net.minecraft.world.item.ShieldItem::new,
            () -> new Item.Properties()
                    .durability(560)
                    .repairable(CAPSHELL_FRAGMENT.get())
                    .equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                    .delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,
                            context -> new net.minecraft.world.item.component.BlocksAttacks(
                                    0.2F,
                                    0.6F,
                                    java.util.List.of(
                                            new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(135.0F,
                                                    java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)), 0.0F, 1.0F),
                                            new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
                                    new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 0.6F),
                                    java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                                    java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                                    java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))
                    .component(net.minecraft.core.component.DataComponents.BREAK_SOUND, net.minecraft.sounds.SoundEvents.SHIELD_BREAK));

    public static final DeferredItem<ScannerItem> SCANNER = registerItem("scanner", ScannerItem::new,
            new Item.Properties().stacksTo(1));

    private static <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        return ITEMS.registerItem(name, factory, () -> properties);
    }

    /**
     * Same as registerItem, but the properties are only built when the item is actually
     * registered. Needed for anything that looks up tags/registries while building its
     * properties (e.g. Item.Properties#humanoidArmor -> repairable(TagKey)), because the
     * item registry is frozen while mod classes are being initialised.
     */
    private static <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, I> factory,
            java.util.function.Supplier<Item.Properties> properties) {
        return ITEMS.registerItem(name, factory, properties);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
