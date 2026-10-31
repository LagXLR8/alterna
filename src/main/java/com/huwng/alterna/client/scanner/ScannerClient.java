package com.huwng.alterna.client.scanner;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.item.ModItems;
import com.huwng.alterna.scanner.ScanEntries;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Client side of the Bio-Scanner.
 *
 * Hold right-click with the scanner on a creature or plant: a beam of sparks runs to the
 * target and a progress bar fills under the crosshair. When it completes, a databank panel
 * slides in from the right edge of the screen, its text types itself out, and the
 * databank voice reads the entry aloud. Looking away resets the scan.
 */
public final class ScannerClient {
    private static final int SCAN_TICKS = 45;
    private static final double RANGE = 16.0;
    private static final int PANEL_W = 184;
    private static final int CYAN = 0xFF4FE0FF, CYAN_DIM = 0xFF2A8FA8, WHITE = 0xFFFFFFFF, GREY = 0xFFA9C4CC;

    // ---- current scan
    private static ScanEntries.@Nullable Entry target;
    private static @Nullable Object targetKey;
    private static boolean hasTarget;
    private static int progress;
    private static boolean scanning;
    // what the world-space VFX (ScannerFx) draws around
    static @Nullable Entity fxEntity;
    static @Nullable PlantShell plantShell;
    static @Nullable Vec3 fxPoint;
    // completion flash (the skin lights up and fades)
    static @Nullable Entity flashEntity;
    static @Nullable PlantShell flashShell;
    static long flashStart;
    static final float FLASH_TICKS = 16.0F;

    // ---- panel
    private static ScanEntries.@Nullable Entry shown;
    private static long shownAt;
    private static @Nullable SoundInstance voice;

    private ScannerClient() {}

    // ================================================================== registration
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Alterna.id("scanner_progress"), ScannerClient::renderProgress);
        event.registerAboveAll(Alterna.id("scanner_panel"), ScannerClient::renderPanel);
    }

    /** Gives every scannable creature's renderer the holographic scan-skin layer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.EntityType<?> type : event.getEntityTypes()) {
            if (ScanEntries.forEntity(BuiltInRegistries.ENTITY_TYPE.getKey(type)) == null) continue;
            var renderer = event.getRenderer(type);
            if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living) {
                living.addLayer(new ScanOverlayLayer(living));
            }
        }
    }

    // ================================================================== ticking
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        scanning = player != null && player.isUsingItem() && player.getUseItem().is(ModItems.SCANNER.get());
        if (!scanning || mc.level == null) {
            fxEntity = null;
            plantShell = null;
            fxPoint = null;
            progress = 0;
            target = null;
            targetKey = null;
            hasTarget = false;
            return;
        }

        // ---- what are we pointing at?
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult blockHit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double maxSq = blockHit.getType() == HitResult.Type.MISS ? RANGE * RANGE : eye.distanceToSqr(blockHit.getLocation());
        AABB sweep = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, sweep, e -> !e.isSpectator() && e.isPickable(), maxSq);

        ScanEntries.Entry entry = null;
        Object key = null;
        Vec3 point = null;
        fxEntity = null;
        if (entityHit != null) {
            plantShell = null;
            Entity e = entityHit.getEntity();
            if (e instanceof PartEntity<?> part) e = part.getParent();
            entry = ScanEntries.forEntity(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
            key = e.getId();
            point = entityHit.getLocation();
            fxEntity = e;
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            var state = mc.level.getBlockState(pos);
            entry = ScanEntries.forBlock(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            key = pos.immutable();
            point = blockHit.getLocation();
            PlantShell shell = plantShell;
            boolean stale = shell == null || shell.entry != entry || player.tickCount % 40 == 0
                    || !shell.origin.equals(pos) && !shellContains(shell, pos);
            plantShell = entry == null ? null : stale ? PlantShell.build(mc.level, pos, entry) : shell;
        } else {
            plantShell = null;
        }
        fxPoint = point;
        hasTarget = key != null;
        if (!Objects.equals(key, targetKey) || entry != target) {
            if (hasTarget && entry == null && !Objects.equals(key, targetKey)) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCAN_FAIL.get(), 1.0F, 0.5F));
            }
            progress = 0;
            targetKey = key;
            target = entry;
        }
        if (entry == null) return;

        // ---- scanning
        progress++;
        if (progress % 20 == 1) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCAN_LOOP.get(), 0.9F + progress / (float) SCAN_TICKS * 0.3F, 0.5F));
        }
        if (progress >= SCAN_TICKS) {
            flashEntity = fxEntity;
            flashShell = plantShell;
            flashStart = mc.level.getGameTime();
            complete(mc, entry);
            progress = 0;
            if (mc.gameMode != null) mc.gameMode.releaseUsingItem(player);
        }
    }

    private static boolean shellContains(PlantShell shell, BlockPos pos) {
        // the shell was built from a different block of the same plant: keep it
        return shell.origin.distManhattan(pos) < 64 && shell.entry == target;
    }

    /** How strongly to draw the scan skin over this rendered creature (0 = not at all). */
    static float overlayStrengthFor(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0.0F;
        float partial = ScanOverlayLayer.partial();
        float best = 0.0F;
        if (scanning && fxEntity != null && target != null && ScanOverlayLayer.matches(state, fxEntity, partial)) {
            best = skinStrength(progressFraction(partial), mc.level.getGameTime() + partial);
        }
        if (flashEntity != null) {
            float f = flashFade(mc.level.getGameTime() + partial);
            if (f > 0.0F && ScanOverlayLayer.matches(state, flashEntity, partial)) best = Math.max(best, f);
        }
        return best;
    }

    /** Skin brightness while scanning: faint at first, brighter as the scan fills, with a soft pulse. */
    static float skinStrength(float progress, float time) {
        return (0.35F + 0.55F * progress) * (0.85F + 0.15F * Mth.sin(time * 0.6F));
    }

    /** 1 right after a scan completes, fading to 0. */
    static float flashFade(float time) {
        float t = (time - flashStart) / FLASH_TICKS;
        return t < 0.0F || t > 1.0F ? 0.0F : 1.0F - t * t;
    }

    static float progressFraction(float partial) {
        return target == null ? 0.0F : Math.min(1.0F, (progress + partial) / SCAN_TICKS);
    }

    static boolean isScanning() {
        return scanning;
    }

    static boolean hasEntry() {
        return target != null;
    }

    private static void complete(Minecraft mc, ScanEntries.Entry entry) {
        if (voice != null) mc.getSoundManager().stop(voice);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCAN_COMPLETE.get(), 1.0F, 0.7F));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCAN_PANEL.get(), 1.0F, 0.6F));
        var voiceEvent = ModSounds.SCAN_VOICES.get(entry.id());
        if (voiceEvent != null) {
            voice = SimpleSoundInstance.forUI(voiceEvent.get(), 1.0F, 1.0F);
            mc.getSoundManager().play(voice);
        }
        shown = entry;
        shownAt = Util.getMillis();
    }

    // ================================================================== HUD: progress
    private static void renderProgress(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!scanning) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        float f = target == null ? 0.0F : Math.min(1.0F, (progress + delta.getGameTimeDeltaPartialTick(false)) / SCAN_TICKS);

        // corner brackets around the crosshair that close in as the scan progresses
        int r = 14 - (int) (f * 5);
        int col = target != null ? CYAN : 0xFF6A7A80;
        for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) {
            int x = cx + sx * r, y = cy + sy * r;
            g.fill(Math.min(x, x - sx * 4), y, Math.max(x, x - sx * 4) + 1, y + 1, col);
            g.fill(x, Math.min(y, y - sy * 4), x + 1, Math.max(y, y - sy * 4) + 1, col);
        }

        Component label;
        if (target != null) {
            label = Component.translatable("scanner.alterna.scanning").append(": ").append(Component.translatable(target.titleKey()));
        } else if (hasTarget) {
            label = Component.translatable("scanner.alterna.no_data");
        } else {
            label = Component.translatable("scanner.alterna.no_target");
        }
        int y = cy + 22;
        g.centeredText(font, label, cx, y, target != null ? CYAN : GREY);
        if (target != null) {
            int w = 90, x0 = cx - w / 2, y0 = y + 12;
            g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 4, 0xAA000000);
            g.fill(x0, y0, x0 + (int) (w * f), y0 + 3, CYAN);
            g.centeredText(font, (int) (f * 100) + "%", cx, y0 + 7, GREY);
        }
    }

    // ================================================================== HUD: databank panel
    private static void renderPanel(GuiGraphicsExtractor g, DeltaTracker delta) {
        ScanEntries.Entry entry = shown;
        if (entry == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float elapsed = (Util.getMillis() - shownAt) / 1000.0F;
        float total = entry.voiceSeconds() + 5.0F;
        if (elapsed > total) {
            shown = null;
            return;
        }
        // slide in from the right edge, slide back out at the end
        float in = Mth.clamp(elapsed / 0.35F, 0.0F, 1.0F);
        float out = Mth.clamp((total - elapsed) / 0.45F, 0.0F, 1.0F);
        float slide = easeOut(Math.min(in, out));

        int textW = PANEL_W - 16;
        String full = Component.translatable(entry.textKey()).getString();
        List<FormattedCharSequence> allLines = font.split(FormattedText.of(full), textW);
        int panelH = Math.min(g.guiHeight() - 24, 44 + allLines.size() * 10 + 10);
        int x = g.guiWidth() - (int) ((PANEL_W + 8) * slide);
        int y = 12;

        // frame
        g.fill(x, y, x + PANEL_W, y + panelH, 0xD8081218);
        g.fill(x, y, x + PANEL_W, y + 2, CYAN);
        g.outline(x, y, PANEL_W, panelH, CYAN_DIM);
        g.fill(x + 3, y + panelH - 3, x + PANEL_W - 3, y + panelH - 2, 0x552A8FA8);

        // header row
        g.pose().pushMatrix();
        g.pose().translate(x + 6, y + 6);
        g.pose().scale(0.75F, 0.75F);
        g.text(font, Component.translatable("scanner.alterna.header"), 0, 0, CYAN_DIM, false);
        Component cat = Component.translatable("scanner.alterna.category." + entry.category().name().toLowerCase());
        g.text(font, cat, (int) ((PANEL_W - 12) / 0.75F) - font.width(cat), 0, CYAN_DIM, false);
        g.pose().popMatrix();

        // icon + title
        ItemStack icon = BuiltInRegistries.ITEM.getOptional(entry.icon()).map(ItemStack::new).orElse(ItemStack.EMPTY);
        int tx = x + 8;
        if (!icon.isEmpty()) {
            g.item(icon, x + 6, y + 16);
            tx = x + 26;
        }
        g.text(font, Component.translatable(entry.titleKey()).withStyle(ChatFormatting.BOLD), tx, y + 20, WHITE, true);

        // voice activity indicator
        boolean speaking = voice != null && mc.getSoundManager().isActive(voice);
        for (int i = 0; i < 5; i++) {
            float a = speaking ? Math.abs(Mth.sin(elapsed * 9.0F + i * 1.3F)) : 0.1F;
            int h = 1 + (int) (a * 6);
            int bx = x + PANEL_W - 30 + i * 4;
            g.fill(bx, y + 27 - h, bx + 2, y + 27, speaking ? CYAN : CYAN_DIM);
        }
        g.fill(x + 6, y + 34, x + PANEL_W - 6, y + 35, CYAN_DIM);

        // text types itself out in step with the narration
        float typed = Mth.clamp(elapsed / Math.max(1.0F, entry.voiceSeconds() * 0.9F), 0.0F, 1.0F);
        String partial = full.substring(0, Math.round(full.length() * typed));
        List<FormattedCharSequence> lines = font.split(FormattedText.of(partial), textW);
        int ly = y + 40;
        for (FormattedCharSequence line : lines) {
            if (ly > y + panelH - 12) break;
            g.text(font, line, x + 8, ly, 0xFFDDEFF3, false);
            ly += 10;
        }
        // blinking caret while typing
        if (typed < 1.0F && ((int) (elapsed * 3)) % 2 == 0 && !lines.isEmpty()) {
            int cx = x + 8 + font.width(lines.get(lines.size() - 1)) + 1;
            int cy = y + 40 + (lines.size() - 1) * 10;
            g.fill(cx, cy, cx + 4, cy + 8, CYAN);
        }
    }

    private static float easeOut(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }
}
