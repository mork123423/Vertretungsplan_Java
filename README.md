# Vertretungsplan_Java

## Überblick
Ein Java‑Programm (GUI + CLI), das sich bei Bonniweb einloggt, den aktuellen Vertretungsplan findet, UNTIS‑Einträge parst und EVA‑Überschneidungen mit deinen Kursen für Heute/Morgen anzeigt.

## Funktionen
- Login via Bonniweb (Session‑Cookies, Redirect‑Handling).
- Automatisches Auflösen der aktuellen Plan‑URL aus einer Ressourcen‑Seite.
- UNTIS‑Parser mit mehreren Formaten (inkl. kompakter Zeilen).
- Kursliste aus Bonniweb abrufen und gegen EVA‑Einträge matchen.
- Optionaler Abgleich mit Stundenplan‑PDF (PDFBox) zur besseren Treffergenauigkeit.
- Ausgabe für Heute und Morgen.
- JavaFX‑GUI mit Profilverwaltung (anlegen, laden, löschen) und Ergebnisanzeige.

## Start
GUI:
- Starte `VertretungsplanApp.java`.

CLI:
- Starte `Vertretungsplan.java`.

## Konfiguration
- `profile.properties`: Profile für die GUI (lokal). Passwörter nicht ins Repo committen.
- `config.properties`: Zugangsdaten für CLI (`username`, `password`) oder per Env‑Vars `BONNIWEB_USER`/`BONNIWEB_PASS`.
- Libraries liegen in `libs/` (Jsoup + PDFBox).

## Ausgabe
- Zeigt EVA‑Überschneidungen pro Tag (Heute/Morgen).
- Markiert mögliche Kursniveau‑Abweichungen (GK/LK/ZK) im CLI.
