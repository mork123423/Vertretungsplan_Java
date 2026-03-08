import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimetableMatcher {
    private static final Pattern HOUR_PATTERN = Pattern.compile("^(\\d{1,2})[\\.)]?\\b");
    private final Map<String, List<String>> hourLines = new LinkedHashMap<>();
    private final List<String> allLines = new ArrayList<>();

    public TimetableMatcher(String text) {
        if (text == null) return;
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").replace('\u00A0', ' ');
        String currentHour = null;
        for (String raw : normalized.split("\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            allLines.add(line);

            Matcher m = HOUR_PATTERN.matcher(line);
            if (m.find()) {
                currentHour = m.group(1);
            }
            if (currentHour != null) {
                hourLines.computeIfAbsent(currentHour, k -> new ArrayList<>()).add(line);
            }
        }
    }

    // NEW METHOD: Exposes the parsed data to the GUI
    public Map<String, List<String>> getParsedTimetable() {
        return hourLines;
    }

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

        // Fallback: search all lines if hour-based match failed.
        return lineMatch(allLines, subject, teacher, teacherShort, teacherShort2);
    }

    private boolean lineMatch(List<String> lines, String subject, String teacher, String t3, String t2) {
        for (String line : lines) {
            String upper = line.toUpperCase();
            if (!containsToken(upper, subject)) continue;
            if (upper.contains(teacher) || upper.contains(t3) || upper.contains(t2)) return true;
        }
        return false;
    }

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
