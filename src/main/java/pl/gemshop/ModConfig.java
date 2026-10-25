package pl.gemshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("gemshop.json"); }

    public int accent = 3;          // 0 niebieski, 1 zolty, 2 zielony, 3 rozowy, 4 fiolet
    public int theme = 0;           // 0 Midnight, 1 Glass, 2 Frost
    public boolean closeFade = true; // false = Rozpad
    public boolean esp = true;
    public boolean tracer = true;
    public boolean tracerAll = false; // false: tylko oznaczony Bomba, true: wszyscy gracze
    public int hudCorner = 0;       // 0 LG, 1 PG, 2 LD, 3 PD
    public boolean hudManual = false;
    public boolean hudCompact = false;
    public int hudX = 16, hudY = 16;
    public boolean animations = true;
    public boolean parallax = true;
    public boolean blur = true;
    public boolean noPush = false; // ogranicza odpychanie przez inne encje; nie wyłącza kolizji ze ścianami
    public boolean hitboxEnabled = false;
    public float hitboxWidth = 1.5f;
    public float hitboxHeight = 1.5f;
    // AutoTrap/AutoWeb w GemShop to tryby podgladu treningowego; nie stawiaja blokow.
    public boolean autoTrapPreview = false;
    public boolean autoTrapObsidian = false;
    public float autoTrapSpeed = 5.0f;
    public boolean autoWebPreview = false;
    public float autoWebSpeed = 5.0f;
    public boolean assistPreview = false;
    // Przelaczniki modulow GUI; wymagaja osobnej implementacji, by wykonywac dzialania w grze.
    public boolean errorKillerEnabled = false;
    public boolean autoTrapEnabled = false;
    public boolean aimAssistEnabled = false;
    public boolean autoWebEnabled = false;
    public boolean fireworkFixedEnabled = false;
    public boolean freeCamEnabled = false;
    public float freeCamSpeed = 5.0f;

    public static ModConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ModConfig c = GSON.fromJson(Files.readString(p), ModConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) { }
        ModConfig c = new ModConfig();
        c.save();
        return c;
    }

    public void save() {
        try { Files.writeString(path(), GSON.toJson(this)); } catch (Exception ignored) { }
    }
}
