import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Vertretungsplan {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try { props.load(new FileInputStream("config.properties")); } catch (Exception ignored) {}

        String user = System.getenv("BONNIWEB_USER");
        String pass = System.getenv("BONNIWEB_PASS");
        if (user == null) user = props.getProperty("username");
        if (pass == null) pass = props.getProperty("password");

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
        String planText = client.fetchPlanText("https://bonniweb.de/pluginfile.php/2992/mod_resource/content/11/w00024.htm");
        if (planText.isEmpty()) {
            System.out.println("Zugriff auf den Plan fehlgeschlagen (Login-Seite erhalten).");
            return;
        }

        UntisParser parser = new UntisParser();
        Map<String, List<PlanEntry>> allByDay = parser.parse(planText);
        EvaOverlapService overlapService = new EvaOverlapService();
        LinkedHashMap<String, List<PlanEntry>> filtered = overlapService.filterEva(allByDay, courses);

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

    private static void printDay(String label, LinkedHashMap<String, List<PlanEntry>> filtered) {
        if (label == null) return;
        System.out.println(label);
        List<PlanEntry> list = filtered.getOrDefault(label, Collections.emptyList());
        if (list.isEmpty()) {
            System.out.println("  (Keine EVA-Überschneidungen)");
        } else {
            for (PlanEntry e : list) {
                System.out.println("  " + e.fach + " - Stunde " + e.stunde + " - " + e.lehrer);
            }
        }
    }   d
}
