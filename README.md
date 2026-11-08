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

- Kliknięcie Bomba uruchamia 3-sekundowe, niewidoczne opóźnienie. Po nim pojawia się HUD z pozostałym czasem z 60-sekundowego okresu (około 0:57).
- Po 3 sekundach mod tworzy nieruchomy lokalny snapshot gracza, jeśli jego encja jest załadowana w świecie klienta.
- Snapshot kopiuje skin, pozycję, rotację, wyświetlaną nazwę, ekwipunek, maksymalne i aktualne HP oraz absorpcję.
- Oryginalny model i hitbox oznaczonego gracza są ukrywane tylko na tym kliencie do końca efektu. ESP i tracer są przekierowane na snapshot.
- Snapshot ma lokalne HP, można go trafiać na tym kliencie, a ataki zmniejszają jego zdrowie bez wysyłania pakietu ataku na prawdziwego gracza. Nad nim są renderowane serca oraz krótkie spadające serca po otrzymaniu obrażeń.
- Po wyzerowaniu lokalnego HP snapshot znika, a przez 2 sekundy wyświetla się lokalny komunikat w stylu „ZABÓJSTWO”. Nie zawiera zmyślonych punktów ani pozycji rankingu.

## Ograniczenia serwera i integracje

- Snapshot istnieje wyłącznie po stronie klienta. Serwer go nie widzi, nie przyznaje za niego prawdziwego zabójstwa, rankingu ani nagrody. Lokalny komunikat nie jest komunikatem serwera.
- Heart Indicator nie jest obecnie zmodyfikowany; Gembi rysuje osobne serca wyłącznie nad snapshotem. Wspólne działanie z modem Heart Indicator nie jest wymagane do tych serc.
- Klient nie dostaje informacji, który gracz postawił lub zniszczył konkretny blok. Ghost-blocków przypisanych do działań konkretnego gracza nie da się wiarygodnie zrobić wyłącznie po stronie klienta; wymaga to pluginu/moda serwerowego.
- Prawdziwe zdarzenia zabójstw i rankingu oraz zmiana widoczności dla innych graczy wymagają obsługi po stronie serwera.
