import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service‑Klasse, die die geparsten Vertretungseinträge filtert und nur
 * diejenigen behält, die als "EVA" markiert sind und die mit den vom
 * Schüler belegten Kursen überlappen. Sie verwendet den
 * <code>CourseMatcher</code> für die unscharfe Übereinstimmung.
 */
public class EvaOverlapService {
    private final CourseMatcher matcher = new CourseMatcher();

    public LinkedHashMap<String, List<EvaMatch>> filterEva(Map<String, List<PlanEntry>> byDay, List<String> courses) {
        return filterEva(byDay, courses, null);
    }

    public LinkedHashMap<String, List<EvaMatch>> filterEva(Map<String, List<PlanEntry>> byDay, List<String> courses, String timetableText) {
        TimetableMatcher timetable = (timetableText == null || timetableText.trim().isEmpty())
                ? null
                : new TimetableMatcher(timetableText);
        LinkedHashMap<String, List<EvaMatch>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<PlanEntry>> entry : byDay.entrySet()) {
            List<EvaMatch> filtered = new ArrayList<>();
            for (PlanEntry e : entry.getValue()) {
                if (!"EVA".equalsIgnoreCase(e.info)) continue;
                CourseMatcher.MatchStatus status = matcher.matchStatus(e.fach, e.lehrer, courses);
                boolean ttMatch = timetable != null && timetable.matches(e.fach, e.lehrer, e.stunde);
                if (ttMatch) {
                    filtered.add(new EvaMatch(e, status == CourseMatcher.MatchStatus.LEVEL_MISMATCH));
                } else if (status == CourseMatcher.MatchStatus.OK) {
                    filtered.add(new EvaMatch(e, false));
                } else if (status == CourseMatcher.MatchStatus.LEVEL_MISMATCH) {
                    filtered.add(new EvaMatch(e, true));
                }
            }
            out.put(entry.getKey(), filtered);
        }
        return out;
    }
}
