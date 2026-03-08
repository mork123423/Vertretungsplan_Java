import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Vertretungsplan {
    private static final String PLAN_RESOURCE_URL = "https://bonniweb.de/mod/resource/view.php?id=1323";
    private static final String TIMETABLE_PDF_URL = "https://bonniweb.de/pluginfile.php/2990/mod_resource/content/4/Stufe_Q2.pdf";

    public static void main(String[] args) throws Exception {
        String user = System.getenv("BONNIWEB_USER");
        String pass = System.getenv("BONNIWEB_PASS");
        
        if (user == null || pass == null) {
            System.out.println("Keine Zugangsdaten gefunden.");
            return;
        }

        BonniwebClient client = new BonniwebClient();
        if (!client.login(user, pass)) {
            System.out.println("Login fehlgeschlagen.");
            return;
        }

        List<String> courses = client.fetchCourses();
        String timetableText = "";
        try {
            timetableText = client.fetchPdfText(TIMETABLE_PDF_URL);
        } catch (Exception ignored) {}
        if (timetableText == null || timetableText.trim().isEmpty()) {
            System.out.println("Hinweis: Stundenplan konnte nicht gelesen werden.");
        }
        String planText = client.fetchPlanTextFromResource(PLAN_RESOURCE_URL);
        if (planText.isEmpty()) {
            System.out.println("Zugriff auf den Plan fehlgeschlagen (Login-Seite erhalten).");
            return;
        }

        UntisParser parser = new UntisParser();
        Map<String, List<PlanEntry>> allByDay = parser.parse(planText);
        EvaOverlapService overlapService = new EvaOverlapService();
        LinkedHashMap<String, List<EvaMatch>> filtered = overlapService.filterEva(allByDay, courses, timetableText);

        System.out.println("EVA-Überschneidungen mit deinen Kursen:");

        List<String> dayOrder = new ArrayList<>(parser.splitByDay(planText).keySet());
        String dayToday = parser.firstAvailableDay(planText);
        String dayTomorrow = null;
        if (dayToday != null) {
            int idx = dayOrder.indexOf(dayToday);
            if (idx >= 0 && idx + 1 < dayOrder.size()) dayTomorrow = dayOrder.get(idx + 1);
        }

        printDay(dayToday, filtered);
        printDay(dayTomorrow, filtered);
    }

    private static void printDay(String label, LinkedHashMap<String, List<EvaMatch>> filtered) {
        if (label == null) return;
        System.out.println(label);
        List<EvaMatch> list = filtered.getOrDefault(label, Collections.emptyList());
        if (list.isEmpty()) {
            System.out.println("  (Keine EVA-Überschneidungen)");
        } else {
            for (EvaMatch m : list) {
                String note = m.levelMismatch ? " (möglicherweise falscher GK/LK/ZK)" : "";
                System.out.println("  " + m.entry.fach + " - Stunde " + m.entry.stunde + " - " + m.entry.lehrer + note);
            }
        }
    }
}






