import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvaOverlapService {
    private final CourseMatcher matcher = new CourseMatcher();

    public LinkedHashMap<String, List<PlanEntry>> filterEva(Map<String, List<PlanEntry>> byDay, List<String> courses) {
        LinkedHashMap<String, List<PlanEntry>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<PlanEntry>> entry : byDay.entrySet()) {
            List<PlanEntry> filtered = new ArrayList<>();
            for (PlanEntry e : entry.getValue()) {
                if ("EVA".equalsIgnoreCase(e.info) && matcher.matchesCourse(e.fach, e.lehrer, courses)) {
                    filtered.add(e);
                }
            }
            out.put(entry.getKey(), filtered);
        }
        return out;
    }
}
