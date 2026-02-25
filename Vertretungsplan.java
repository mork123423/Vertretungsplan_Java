import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vertretungsplan {

    private static final String BASE_URL = "https://bonniweb.de";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final Map<String, String> cookies = new HashMap<>();

    static boolean isLoginPage(Document doc) {
        return doc.selectFirst("input[name=username]") != null
                && doc.selectFirst("input[name=password]") != null;
    }

    static boolean login(String username, String password) throws Exception {
        cookies.putAll(Jsoup.connect(BASE_URL + "/login/index.php?testsession=1")
                .userAgent(USER_AGENT).method(Connection.Method.GET).execute().cookies());
        cookies.putAll(Jsoup.connect(BASE_URL + "/index.php")
                .userAgent(USER_AGENT).method(Connection.Method.GET).execute().cookies());

        Connection.Response loginResp = Jsoup.connect(BASE_URL + "/login/index.php")
                .cookies(cookies).userAgent(USER_AGENT).method(Connection.Method.GET).execute();
        cookies.putAll(loginResp.cookies());
        Document loginPage = loginResp.parse();

        Element loginForm = loginPage.selectFirst("form#login");
        String action = loginForm != null ? loginForm.attr("action") : (BASE_URL + "/login/index.php");

        Map<String, String> data = new HashMap<>();
        if (loginForm != null) {
            for (Element input : loginForm.select("input[name]")) {
                String name = input.attr("name");
                String value = input.hasAttr("value") ? input.attr("value") : "";
                data.put(name, value);
            }
        }
        data.put("username", username);
        data.put("password", password);

        Connection.Response resp = Jsoup.connect(action)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .referrer(BASE_URL + "/login/index.php")
                .data(data)
                .method(Connection.Method.POST)
                .followRedirects(false)
                .execute();

        cookies.putAll(resp.cookies());
        String location = resp.header("Location");
        for (int i = 0; location != null && i < 5; i++) {
            String next = location.startsWith("http") ? location : (BASE_URL + location);
            resp = Jsoup.connect(next)
                    .cookies(cookies)
                    .userAgent(USER_AGENT)
                    .method(Connection.Method.GET)
                    .followRedirects(false)
                    .execute();
            cookies.putAll(resp.cookies());
            location = resp.header("Location");
        }

        Document dash = Jsoup.connect(BASE_URL + "/my/")
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .get();
        return !isLoginPage(dash);
    }

    static String scrapePlan(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .followRedirects(true)
                .execute()
                .parse();
        if (isLoginPage(doc)) return "";
        String text = doc.body().wholeText();
        return text.replace("\r\n", "\n").replace("\r", "\n").replace('\u00A0', ' ');
    }

    static List<String> fetchCourses() throws Exception {
        Document doc = Jsoup.connect(BASE_URL + "/my/courses.php")
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .get();
        if (isLoginPage(doc)) return Collections.emptyList();
        Elements links = doc.select("a[href*='/course/view.php'], .coursebox .coursename a, .course-summaryitem a");
        Set<String> unique = new LinkedHashSet<>();
        for (Element a : links) {
            String t = a.text().trim();
            if (!t.isEmpty()) unique.add(t);
        }
        return new ArrayList<>(unique);
    }

    static Map<String, List<Map<String, String>>> parseUntis(String text) {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\s+\\p{L}+)").matcher(text);

        List<String> parts = new ArrayList<>();
        int last = 0;
        while (m.find()) {
            parts.add(text.substring(last, m.start()));
            parts.add(m.group());
            last = m.end();
        }
        parts.add(text.substring(last));

        Pattern entryPattern = Pattern.compile("^(\\S+)\\s+(\\d{1,2})\\s+(.*?)\\s{2,}(\\S+)\\s+(\\S+)\\s*(.*)$");
        Pattern fallbackPattern = Pattern.compile("^(\\S+)\\s+(\\d{1,2})\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*(.*)$");
        Pattern compactPattern = Pattern.compile("^(Q\\d)(\\d{1,2})(.*?)(---|[A-Z]{2,5}\\d{3,4})(.+)$");

        for (int i = 1; i < parts.size(); i += 2) {
            String day = parts.get(i).trim();
            result.put(day, new ArrayList<>());
            for (String line : parts.get(i + 1).split("\\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Klasse(n)Stunde")) continue;

                Matcher em = entryPattern.matcher(line);
                if (!em.matches()) em = fallbackPattern.matcher(line);
                if (em.matches()) {
                    Map<String, String> e = new HashMap<>();
                    e.put("klasse", em.group(1));
                    e.put("stunde", em.group(2));
                    e.put("fach", em.group(3).trim());
                    e.put("raum", em.group(4));
                    e.put("lehrer", em.group(5));
                    e.put("info", em.group(6).trim());
                    result.get(day).add(e);
                    continue;
                }

                Matcher cm = compactPattern.matcher(line);
                if (cm.matches()) {
                    String tail = cm.group(5).trim();
                    String info = "";
                    if (tail.endsWith("EVA")) { info = "EVA"; tail = tail.substring(0, tail.length() - 3); }
                    else if (tail.endsWith("VA")) { info = "VA"; tail = tail.substring(0, tail.length() - 2); }

                    Map<String, String> e = new HashMap<>();
                    e.put("klasse", cm.group(1));
                    e.put("stunde", cm.group(2));
                    e.put("fach", cm.group(3).trim());
                    e.put("raum", cm.group(4));
                    e.put("lehrer", tail.trim());
                    e.put("info", info);
                    result.get(day).add(e);
                }
            }
        }
        return result;
    }

    static LinkedHashMap<String, String> splitByDay(String text) {
        LinkedHashMap<String, String> blocks = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\s+\\p{L}+)").matcher(text);

        List<String> parts = new ArrayList<>();
        int last = 0;
        while (m.find()) {
            parts.add(text.substring(last, m.start()));
            parts.add(m.group());
            last = m.end();
        }
        parts.add(text.substring(last));

        for (int i = 1; i < parts.size(); i += 2) {
            String day = parts.get(i).trim();
            String block = parts.get(i + 1);
            blocks.put(day, block);
        }
        return blocks;
    }

    static String firstAvailableDay(String text) {
        LinkedHashMap<String, String> blocks = splitByDay(text);
        for (Map.Entry<String, String> e : blocks.entrySet()) {
            String b = e.getValue().toLowerCase();
            if (b.contains("vertretungen sind nicht freigegeben") || b.contains("vertretungen nicht freigegeben")) {
                continue;
            }
            return e.getKey();
        }
        return blocks.isEmpty() ? null : blocks.keySet().iterator().next();
    }

    static boolean matchesCourse(String fach, String lehrer, List<String> courses) {
        if (fach == null || fach.isEmpty()) return false;
        String subject = fach.split("-", 2)[0].trim().toUpperCase();
        if (subject.isEmpty()) return false;

        String teacher = lehrer == null ? "" : lehrer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (teacher.isEmpty()) return false;
        String teacherShort = teacher.length() > 3 ? teacher.substring(0, 3) : teacher;
        String teacherShort2 = teacher.length() > 2 ? teacher.substring(0, 2) : teacher;

        String levelType = "";
        String levelNum = "";
        Matcher lm = Pattern.compile("-(GK|LK)(\\d)").matcher(fach.toUpperCase());
        if (lm.find()) {
            levelType = lm.group(1);
            levelNum = lm.group(2);
        }

        String key1 = "-" + subject + "_" + teacher;
        String key2 = "-" + subject + "_" + teacherShort;
        String key3 = "-" + subject + "_" + teacherShort2;
        String subjectKey = "-" + subject + "_";

        List<String> candidates = new ArrayList<>();
        for (String c : courses) {
            String s = c.replace(' ', '-').toUpperCase();

            if (!levelType.isEmpty()) {
                String pad2 = "0" + levelNum;
                String token1 = "-" + levelType + levelNum; // -GK2
                String token2 = "-" + levelType.charAt(0) + "KB" + pad2; // -GKB02 / -LKB02
                String token3 = "-" + levelType.charAt(0) + "KB" + levelNum;
                if (!(s.contains(token1) || s.contains(token2) || s.contains(token3))) {
                    continue;
                }
            }

            if (s.contains(subjectKey)) {
                candidates.add(s);
                if (s.contains(key1) || s.contains(key2) || s.contains(key3)) return true;
            }
        }

        return candidates.size() == 1;
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try { props.load(new FileInputStream("config.properties")); } catch (Exception ignored) {}

        String user = System.getenv("BONNIWEB_USER");
        String pass = System.getenv("BONNIWEB_PASS");
        if (user == null) user = props.getProperty("username");
        if (pass == null) pass = props.getProperty("password");

        if (user == null || pass == null) { System.out.println("Keine Zugangsdaten gefunden."); return; }
        if (!login(user, pass)) { System.out.println("Login fehlgeschlagen."); return; }

        List<String> courses = fetchCourses();
        String planText = scrapePlan("https://bonniweb.de/pluginfile.php/2992/mod_resource/content/11/w00024.htm");
        if (planText.isEmpty()) { System.out.println("Zugriff auf den Plan fehlgeschlagen (Login-Seite erhalten)."); return; }

        Map<String, List<Map<String, String>>> vertretungen = parseUntis(planText);
        LinkedHashMap<String, List<Map<String, String>>> filteredByDay = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : vertretungen.entrySet()) {
            List<Map<String, String>> filtered = new ArrayList<>();
            for (Map<String, String> e : entry.getValue()) {
                String info = e.getOrDefault("info", "");
                String fach = e.getOrDefault("fach", "");
                String lehrer = e.getOrDefault("lehrer", "");
                if ("EVA".equalsIgnoreCase(info) && matchesCourse(fach, lehrer, courses)) filtered.add(e);
            }
            if (!filtered.isEmpty()) filteredByDay.put(entry.getKey(), filtered);
        }

        System.out.println("EVA-Überschneidungen mit deinen Kursen:");

        List<String> daysAll = new ArrayList<>(splitByDay(planText).keySet());
        String dayToday = firstAvailableDay(planText);
        String dayTomorrow = null;
        if (dayToday != null) {
            int idx = daysAll.indexOf(dayToday);
            if (idx >= 0 && idx + 1 < daysAll.size()) dayTomorrow = daysAll.get(idx + 1);
        }

        int count = 0;
        if (dayToday != null) {
            System.out.println(dayToday);
            List<Map<String, String>> list = filteredByDay.getOrDefault(dayToday, Collections.emptyList());
            if (list.isEmpty()) {
                System.out.println("  (Keine EVA-Überschneidungen)");
            } else {
                count += list.size();
                for (Map<String, String> e : list) System.out.println("  " + e);
            }
        }

        if (dayTomorrow != null) {
            System.out.println(dayTomorrow);
            List<Map<String, String>> list = filteredByDay.getOrDefault(dayTomorrow, Collections.emptyList());
            if (list.isEmpty()) {
                System.out.println("  (Keine EVA-Überschneidungen)");
            } else {
                count += list.size();
                for (Map<String, String> e : list) System.out.println("  " + e);
            }
        }

        if (count == 0 && dayToday == null && dayTomorrow == null) {
            System.out.println("Keine EVA-Überschneidungen gefunden.");
        }
    }
}
