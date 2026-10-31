package com.huwng.alterna.scanner;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The scanner's databank: which creatures and plants can be scanned, what they map to,
 * and how long each narrated entry lasts. Titles/descriptions live in the lang files
 * ("scanner.alterna.<id>.title" / ".text"); narration in assets/alterna/sounds/scanner/<id>.ogg.
 * (Generated from the databank sheet together with the voice lines.)
 */
public final class ScanEntries {
    public enum Category { FAUNA, FLORA }

    /** @param voiceSeconds length of the narration, used to pace the on-screen text */
    public record Entry(String id, Category category, Identifier icon, float voiceSeconds) {
        public String titleKey() { return "scanner.alterna." + this.id + ".title"; }
        public String textKey() { return "scanner.alterna." + this.id + ".text"; }
    }

    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static final Map<Identifier, Entry> BY_ENTITY = new HashMap<>();
    private static final Map<Identifier, Entry> BY_BLOCK = new HashMap<>();

    static {
        add("cragback", Category.FAUNA, "alterna:cragback_spawn_egg", 19.21F, new String[]{"alterna:cragback"}, new String[]{});
        add("tetra", Category.FAUNA, "alterna:tetra_spawn_egg", 18.75F, new String[]{"alterna:tetra"}, new String[]{});
        add("tetra_nest", Category.FAUNA, "alterna:tetra_nest", 14.46F, new String[]{}, new String[]{"alterna:tetra_nest"});
        add("climbing_zombie", Category.FAUNA, "alterna:climbing_zombie_spawn_egg", 12.24F, new String[]{"alterna:climbing_zombie"}, new String[]{});
        add("armor_overlord", Category.FAUNA, "alterna:armor_overlord_spawn_egg", 31.77F, new String[]{"alterna:armor_overlord"}, new String[]{});
        add("weavecap", Category.FLORA, "alterna:weavecap_cap", 15.03F, new String[]{}, new String[]{"alterna:weavecap_log", "alterna:weavecap_wood", "alterna:weavecap_cap", "alterna:weavecap_gills", "alterna:weavecap_sprout"});
        add("lichen_grass", Category.FLORA, "alterna:lichen_grass", 14.35F, new String[]{}, new String[]{"alterna:lichen_grass", "alterna:tall_lichen_grass", "alterna:lichen_turf"});
        add("ember_cap", Category.FLORA, "alterna:ember_cap", 12.52F, new String[]{}, new String[]{"alterna:ember_cap"});
        add("violet_bush", Category.FLORA, "alterna:violet_bush", 10.70F, new String[]{}, new String[]{"alterna:violet_bush", "alterna:violet_leaves"});
        add("white_currant", Category.FLORA, "alterna:white_currant_berries", 10.70F, new String[]{}, new String[]{"alterna:white_currant_berry_bush"});
        add("star_lily", Category.FLORA, "alterna:star_lily", 14.67F, new String[]{}, new String[]{"alterna:star_lily", "alterna:star_lily_vine", "alterna:hanging_star_lily", "alterna:hanging_star_lily_plant"});
        add("canopra", Category.FLORA, "alterna:canopra_sapling", 12.20F, new String[]{}, new String[]{"alterna:canopra_log", "alterna:canopra_leaves", "alterna:canopra_sapling"});
        add("goblet_tree", Category.FLORA, "alterna:goblet_log", 13.56F, new String[]{}, new String[]{"alterna:goblet_log", "alterna:goblet_wood", "alterna:goblet_membrane"});
        add("rootshroom", Category.FLORA, "alterna:rootshroom_fungus", 13.22F, new String[]{}, new String[]{"alterna:rootshroom_stem", "alterna:rootshroom_leaves", "alterna:rootshroom_light", "alterna:rootshroom_fungus"});
        add("enoki", Category.FLORA, "alterna:enoki_mushroom", 11.66F, new String[]{}, new String[]{"alterna:enoki_mushroom", "alterna:enoki_mushroom_wall"});
        add("cloudberry", Category.FLORA, "alterna:cloud_berries", 10.34F, new String[]{}, new String[]{"alterna:cloudberry_vines", "alterna:cloudberry_vines_plant"});
        add("daze", Category.FLORA, "alterna:short_daze", 10.36F, new String[]{}, new String[]{"alterna:short_daze", "alterna:tall_daze"});
        add("wild_moss", Category.FLORA, "alterna:wild_moss_block", 10.57F, new String[]{}, new String[]{"alterna:wild_moss_block", "alterna:wild_moss_carpet", "alterna:wild_hanging_moss"});
        add("purple_sugar_cane", Category.FLORA, "alterna:purple_sugar_cane", 8.97F, new String[]{}, new String[]{"alterna:purple_sugar_cane"});
        add("vitalroot", Category.FLORA, "alterna:vitalroot", 9.21F, new String[]{}, new String[]{"alterna:vitalroot", "alterna:vitalroot_block"});
    }

    private static void add(String id, Category category, String icon, float seconds, String[] entities, String[] blocks) {
        Entry e = new Entry(id, category, Identifier.parse(icon), seconds);
        BY_ID.put(id, e);
        for (String s : entities) BY_ENTITY.put(Identifier.parse(s), e);
        for (String s : blocks) BY_BLOCK.put(Identifier.parse(s), e);
    }

    private ScanEntries() {}

    public static @Nullable Entry forEntity(Identifier type) {
        return BY_ENTITY.get(type);
    }

    public static @Nullable Entry forBlock(Identifier block) {
        return BY_BLOCK.get(block);
    }

    public static Iterable<Entry> all() {
        return BY_ID.values();
    }
}
