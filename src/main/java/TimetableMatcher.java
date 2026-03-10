import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helfer, der den Text eines Stundenplans (häufig aus einer PDF kopiert)
 * entgegennimmt und ihn in Zeilen unterteilt, die nach der am Anfang
 * stehenden Stundenzahl gruppiert sind. Er bietet auch Methoden an, um
 * einfache Fragen wie „taucht dieses Fach/Lehrer in Stunde 3 auf?“ zu
 * beantworten. Das Parsen ist absichtlich locker, um mit unordentlichen
 * Eingaben klarzukommen.
 */
public class TimetableMatcher {
    // Regex zum Erkennen einer Stundenangabe am Zeilenanfang, z.B. "1.", "2)" usw.
    private static final Pattern HOUR_PATTERN = Pattern.compile("^(\\d{1,2})[\\.)]?\\b");
    private final Map<String, List<String>> hourLines = new LinkedHashMap<>();
    private final List<String> allLines = new ArrayList<>();

    /**
     * Create a matcher by giving it the full text of a timetable.
     *
     * @param text raw text from a PDF or webpage; if null nothing happens
     */
    public TimetableMatcher(String text) {
        if (text == null) return; // nichts zu parsen
        // Zeilenenden und geschützte Leerzeichen normalisieren, damit das Format stabil ist
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").replace('\u00A0', ' ');
        String currentHour = null;
        // jede nicht‑leere Zeile im normalisierten Text durchgehen
        for (String raw : normalized.split("\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;          // leere Zeilen überspringen
            allLines.add(line);                   // Zeile speichern

            Matcher m = HOUR_PATTERN.matcher(line);
            if (m.find()) {
                // Stundenpräfix gefunden, z. B. "1" oder "12"
                currentHour = m.group(1);
            }
            if (currentHour != null) {
                // Zeile der aktuellen Stunde zuordnen
                hourLines.computeIfAbsent(currentHour, k -> new ArrayList<>()).add(line);
            }
        }
    }

    // Gibt die rohe, geparste Map zurück; Schlüssel ist die Stundenzahl,
    // Wert ist die Liste der zugehörigen Zeilen
    public Map<String, List<String>> getParsedTimetable() {
        return hourLines;
    }

    /**
     * Prüft, ob ein gegebenes Fach/Lehrer-Paar im Stundenplan vorkommt.
     *
     * @param fach    Fachbezeichnung (z. B. "Mathe-GK")
     * @param lehrer  Lehrername (kann Leerzeichen enthalten)
     * @param stunde  optionale Stundenzahl zur Einschränkung der Suche
     * @return true, wenn eine passende Zeile gefunden wurde
     */
    public boolean matches(String fach, String lehrer, String stunde) {
        String subject = normalizeSubject(fach);
        if (subject.isEmpty()) return false;

        String teacher = normalizeTeacher(lehrer);
        if (teacher.isEmpty()) return false;
        String teacherShort = teacher.length() > 3 ? teacher.substring(0, 3) : teacher;
        String teacherShort2 = teacher.length() > 2 ? teacher.substring(0, 2) : teacher;

        List<String> lines = stunde != null && hourLines.containsKey(stunde)
                ? hourLines.get(stunde)
                : allLines;

        if (lineMatch(lines, subject, teacher, teacherShort, teacherShort2)) return true;

        // Fallback: Durchsuche alle Zeilen, falls die stundenbezogene Suche fehlschlägt.
        return lineMatch(allLines, subject, teacher, teacherShort, teacherShort2);
    }

    // Hilfsfunktion, die eine Liste von Zeilen durchsucht und sowohl das Fach‑
    // token als auch eines der passenden Lehrer‑Tokens sucht
    private boolean lineMatch(List<String> lines, String subject, String teacher, String t3, String t2) {
        for (String line : lines) {
            String upper = line.toUpperCase();
            if (!containsToken(upper, subject)) continue; // Fach muss als ganzes Wort vorhanden sein
            if (upper.contains(teacher) || upper.contains(t3) || upper.contains(t2)) return true;
        }
        return false;
    }

    // Zerlegt eine Zeile an beliebigen Nicht‑Buchstaben und prüft auf exakten
    // Token‑Match
    private boolean containsToken(String line, String token) {
        String[] parts = line.toUpperCase().split("[^A-Z├ä├û├£]+");
        for (String p : parts) {
            if (p.equals(token)) return true;
        }
        return false;
    }

    private String normalizeSubject(String fach) {
        if (fach == null) return "";
        String up = fach.toUpperCase();
        int idx = up.indexOf('-');
        String subject = idx > 0 ? up.substring(0, idx) : up;
        subject = subject.replaceAll("[^A-Z├ä├û├£]", "");
        return subject.trim();
    }

    private String normalizeTeacher(String lehrer) {
        if (lehrer == null) return "";
        String teacher = lehrer.replaceAll("[^A-Za-z├ä├û├£├ñ├Â├╝]", "").toUpperCase();
        return teacher.trim();
    }
}
