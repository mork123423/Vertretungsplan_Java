# Vertretungsplan App

Eine Java-Anwendung, die Klassenvertretungen abruft und mit dem
Stundenplan eines Schülers von BonniWeb abgleicht.

## Funktionen

- Anmeldung bei BonniWeb und Abruf der Vertretungsdaten
- Parsen von Stundenplan‑PDFs
- Abgleich der Vertretungen mit deinen Kursen
- JavaFX‑GUI zur Anzeige der Ergebnisse

## Verwendung

> Einfach von der [Release](https://github.com/sys-skye/Vertretungsplan_Java/releases/tag/1Release) Seite herunterladen.


## Oder selbst bauen

### Voraussetzungen

- Java 17+
- Maven 3.6+
- Zugangsdaten für ein BonniWeb‑Konto

### Einrichtung

1. Projekt klonen oder herunterladen.

### Build
```bash
mvn clean package
``` 

### Ausführen
```bash
mvn javafx:run
``` 

---

## Project Structure

| Datei | Beschreibung |
|------|-------------|
| `Launcher.java` | Einstiegspunkt mit JavaFX‑GUI |
| `Vertretungsplan.java` | Hauptlogik zum Abrufen und Verarbeiten der Daten |
| `BonniwebClient.java` | Handhabt BonniWeb‑Authentifizierung und Datenerfassung |
| `TimetableMatcher.java` | Vergleicht Vertretungen mit dem Stundenplan |
| `CourseMatcher.java` | Ordnet Kurse den Vertretungseinträgen zu |

## Lizenz

Nicht lizenziert
