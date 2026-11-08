package pl.adixfx;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista utworów. Żeby dodać nowy wbudowany: wrzuć plik .ogg do assets/adixfx/sounds/,
 * dopisz go w sounds.json i dodaj wpis poniżej. Własne utwory (przeciągnięte w grze)
 * dochodzą do tego dynamicznie - patrz CustomTracks.
 */
public final class Playlist {
    public record Track(String id, String name, Identifier sound, String customFile) {
        public Track(String id, String name, Identifier sound) {
            this(id, name, sound, null);
        }

        /** true = własny, przeciągnięty plik (gra go CustomPlayer, nie silnik dźwięku gry). */
        public boolean isCustom() {
            return customFile != null;
        }
    }

    public static final List<Track> TRACKS = List.of(
            new Track("musor_drop", "МУСОР DROP", Identifier.of("adixfx", "musor_drop")),
            new Track("zani_preparaty", "Зани препараты", Identifier.of("adixfx", "zani_preparaty")),
            new Track("dark_triad", "Заны Заны Препараты (krak3 | The Dark Triad)", Identifier.of("adixfx", "dark_triad"))
    );

    private Playlist() {}

    /** nazwa wyświetlana (własna, jeśli ją zmieniłeś) */
    public static String displayName(Track t) {
        String n = Config.d.names.get(t.id());
        return n == null || n.isBlank() ? t.name() : n;
    }

    public static void setName(Track t, String name) {
        if (name == null || name.isBlank() || name.equals(t.name())) Config.d.names.remove(t.id());
        else Config.d.names.put(t.id(), name.trim());
        Config.save();
    }

    /** głośność utworu 0..2 (domyślnie 1 = 100%) */
    public static float volume(Track t) {
        return Config.d.trackVolume.getOrDefault(t.id(), 1.0f);
    }

    public static void setVolume(Track t, float v) {
        Config.d.trackVolume.put(t.id(), Math.max(0f, Math.min(2f, v)));
    }

    public static boolean enabled(Track t) {
        return Config.d.tracks.getOrDefault(t.id(), true);
    }

    public static void setEnabled(Track t, boolean v) {
        Config.d.tracks.put(t.id(), v);
        Config.save();
    }

    /** Wszystkie gotowe do grania utwory: wbudowane (bez usuniętych) + własne, już skonwertowane. */
    public static List<Track> allTracks() {
        List<Track> out = new ArrayList<>();
        for (Track t : TRACKS) if (!Config.d.removedBuiltins.contains(t.id())) out.add(t);
        for (CustomTracks.Entry e : Config.d.customTracks) {
            if (!CustomTracks.READY.equals(e.status)) continue;
            out.add(new Track(e.id, e.name, null, e.file));
        }
        return out;
    }

    public static List<Track> enabledTracks() {
        List<Track> out = new ArrayList<>();
        for (Track t : allTracks()) if (enabled(t)) out.add(t);
        return out;
    }

    /**
     * Usuwa utwór naprawdę z moda, a nie tylko ukrywa. Własny (przeciągnięty) plik jest od razu
     * kasowany z dysku. Wbudowany znika na stałe z listy, ustawień i grania - ale jego plik .ogg
     * zostaje w środku moda (to .jar, nie da się z niego nic skasować bez nowego builda), więc
     * trafia do "kosza" (Config.d.removedBuiltins); AdixFxScreen pokazuje wtedy ostrzeżenie,
     * że trzeba poprosić Claude o opróżnienie kosza przy następnej wersji moda.
     */
    public static void remove(Track t) {
        if (t.isCustom()) {
            CustomTracks.remove(t.id());
        } else {
            Config.d.removedBuiltins.add(t.id());
        }
        Config.d.names.remove(t.id());
        Config.d.trackVolume.remove(t.id());
        Config.d.tracks.remove(t.id());
        Config.save();
    }
}
