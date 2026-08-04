package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.neoforged.fml.loading.FMLPaths;

/**
 * JSON-based config for the vine system.
 * Saved to config/alterna-vine.json.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public class VineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("alterna-vine.json").toFile();
    private static VineConfig INSTANCE;
    private static VineConfig BACKUP;

    public transient boolean isServerConfig = false;

    /** Radius (blocks) within which the player snaps to a vine cable. */
    public double snapRadius = 2.0;
    /** Reach (blocks) for right-click to initiate using a vine. */
    public double clickReach = 3.0;
    /** If true, vine can be used even when no cable is nearby (debug). */
    public boolean useAnywhere = false;
    /** Minimum dot-product alignment for cable switching at intersections. */
    public double maxTurnAngle = 0.707;
    /** Vertical offset from player position to cable attachment point. */
    public double hangOffset = 2.3;
    /** Movement speed multiplier along the cable. */
    public double speedMultiplier = 1.0;
    /** If true, gravity affects speed (downhill = faster). */
    public boolean realisticPhysics = false;
    /** Jump boost multiplier when releasing the vine. */
    public double exitJumpMultiplier = 1.4;
    /** Cooldown ticks after releasing a vine. */
    public int releaseCooldown = 10;

    public static VineConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, VineConfig.class);
            } catch (Exception e) {
                Alterna.LOGGER.error("Failed to load alterna-vine.json", e);
                INSTANCE = new VineConfig();
            }
        } else {
            INSTANCE = new VineConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Alterna.LOGGER.error("Failed to save alterna-vine.json", e);
        }
    }

    public static void setServerConfig(VineConfig serverConfig) {
        if (BACKUP == null) {
            BACKUP = INSTANCE;
        }
        serverConfig.isServerConfig = true;
        INSTANCE = serverConfig;
        Alterna.LOGGER.info("Applied server vine configuration.");
    }

    public static void restoreLocalConfig() {
        if (BACKUP != null) {
            INSTANCE = BACKUP;
            BACKUP = null;
            Alterna.LOGGER.info("Restored local vine configuration.");
        }
    }
}
