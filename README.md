# Gem Shop (Fabric 1.21.4)

Mod kliencki Fabric z GUI w stylu "Gem Shop".

## Klawisze
- **N** - otwiera/zamyka GUI (animowane)
- **M** - patrzysz na gracza -> otwiera jego profil

## Funkcje
- Lista graczy z TAB-a serwera (czytana w tle, TAB nie jest otwierany), graczy ze swiata i druzyn scoreboardu
- Filtr (zawęża wyniki), przycisk Search -> profil gracza
- "Brak wyciekow" u wszystkich; tylko `_L3kcj4_` ma wpisy (IP **fikcyjne i stale**, zakresy RFC 5737) - edycja w `Fake.java`
- Przycisk Bomba: 60 s cooldown, wszystkie Bomba szare z timerem, czerwona kreska (tracer) do celu, etykieta, HUD z timerem
- ESP: bialy box, czerwony gdy patrzysz na gracza
- Visuals: kolor akcentu, motyw (Midnight/Glass/Frost), zamykanie (Rozpad/Fade), pozycja HUD (rog / reczne przeciaganie), styl Pelny/Kompakt
- Ustawienia: licencja "Wlasciciel / na zawsze", zmiana klawiszy, animacje, ruch 3D + poswiata, rozmycie tla

Konfiguracja: `config/gemshop.json`.

## Budowanie
Java 21 + Gradle 8.12+:

    gradle build        # jar w build/libs/gemshop-1.0.0.jar
    gradle runClient    # test w grze

Albo po prostu wrzuc repo na GitHuba i uzyj workflow `build.yml` (Actions -> Artifacts).

## Bomba — lokalny snapshot i ograniczenia klienta

- Po kliknięciu Bomba lokalny znacznik aktywuje się po 3 sekundach.
- Timer znacznika nadal trwa 60 sekund, więc w chwili aktywacji HUD pokazuje 57 sekund.
- Po aktywacji mod tworzy lokalny snapshot gracza, jeśli jego encja jest załadowana w świecie klienta.
- Snapshot kopiuje skin, pozycję, rotację, nazwę wyświetlaną, ekwipunek, maksymalne i aktualne HP oraz absorpcję. Jeśli cel był w powietrzu, kopia zostaje w miejscu zapisania.
- Oryginalny model i hitbox oznaczonego gracza są ukrywane tylko na tym kliencie do końca efektu. ESP i tracer kierują się na snapshot.
- Snapshot jest wyłącznie wizualny, nie jest interaktywnym celem i nie przyjmuje ataków. Wyświetlane HP jest migawką z chwili utworzenia kopii.

## Ograniczenia serwera i integracje

- Snapshot istnieje wyłącznie w lokalnym świecie klienta. Serwer go nie widzi, więc nie może przyznać za niego zabójstwa ani punktów rankingu.
- Heart Indicator powinien móc odczytać migawkę podczas patrzenia na nią, ponieważ snapshot jest lokalną encją typu PlayerEntity z własnym ID i HP. Nie będzie jednak aktualizacji HP po atakach; wspólne działanie obu modów wymaga jeszcze testu w grze.
- Klient nie dostaje informacji, który gracz postawił lub zniszczył dany blok. Ghost-blocków przypisanych do działań konkretnego gracza nie da się wiarygodnie zrobić wyłącznie po stronie klienta; wymaga to pluginu/moda serwerowego.
- Komunikaty o zabójstwach i ranking są obliczane przez serwer. Gem Shop nie generuje fałszywych zdarzeń serwerowych ani prawdziwych punktów rankingu.
