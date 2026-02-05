import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vertretungsplan {

    private static final String BASE_URL = "https://bonniweb.de";
    private static final Map<String, String> cookies = new HashMap<>();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    static boolean isLoginPage(Document doc) {
        return doc.selectFirst("input[name=username]") != null
                && doc.selectFirst("input[name=password]") != null;
    }

    // ================= LOGIN =================
    static boolean login(String username, String password) throws Exception {
        // Ensure cookie support is enabled (Moodle session test)
        Connection.Response testSession = Jsoup.connect(BASE_URL + "/login/index.php?testsession=1")
                .userAgent(USER_AGENT)
                .method(Connection.Method.GET)
                .execute();
        cookies.putAll(testSession.cookies());

        Connection.Response mainPage = Jsoup.connect(BASE_URL + "/index.php")
                .userAgent(USER_AGENT)
                .method(Connection.Method.GET)
                .execute();
        cookies.putAll(mainPage.cookies());

        Connection.Response loginResp = Jsoup.connect(BASE_URL + "/login/index.php")
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .method(Connection.Method.GET)
                .execute();
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

        Connection.Response response = Jsoup.connect(action)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .referrer(BASE_URL + "/login/index.php")
                .data(data)
                .method(Connection.Method.POST)
                .followRedirects(false)
                .execute();

        cookies.putAll(response.cookies());
        // Manually follow redirects to carry updated cookies
        int redirects = 0;
        String location = response.header("Location");
        while (location != null && redirects < 5) {
            String next = location.startsWith("http") ? location : (BASE_URL + location);
            response = Jsoup.connect(next)
                    .cookies(cookies)
                    .userAgent(USER_AGENT)
                    .method(Connection.Method.GET)
                    .followRedirects(false)
                    .execute();
            cookies.putAll(response.cookies());
            location = response.header("Location");
            redirects++;
        }

        Document dash = Jsoup.connect(BASE_URL + "/my/")
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .get();
        return !isLoginPage(dash);
    }

    // ================= SCRAPE =================
    static String scrapePlan(String url) throws Exception {
        Connection.Response resp = Jsoup.connect(url)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .followRedirects(true)
                .execute();
        Document doc = resp.parse();
        if (isLoginPage(doc)) {
            return "";
        }
        // Preserve line breaks so the parser can split entries correctly
        String text = doc.body().wholeText();
        // Normalize line breaks and non-breaking spaces
        text = text.replace("\r\n", "\n")
                   .replace("\r", "\n")
                   .replace('\u00A0', ' ');
        return text;
    }

    // ================= PARSE UNTIS =================
    static Map<String, List<Map<String, String>>> parseUntis(String text) {

        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        Pattern dayPattern = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\s+\\p{L}+)");
        Matcher m = dayPattern.matcher(text);

        List<String> parts = new ArrayList<>();
        int last = 0;
        while (m.find()) {
            parts.add(text.substring(last, m.start()));
            parts.add(m.group());
            last = m.end();
        }
        parts.add(text.substring(last));

        Pattern entryPattern = Pattern.compile(
                "(\\p{L}{1,4}\\d{0,2})(\\d{1,2})(.*?)(---|[\\w\\d]+)(\\p{L}{2,4})(.*)"
        );

        for (int i = 1; i < parts.size(); i += 2) {
            String day = parts.get(i).trim();
            result.put(day, new ArrayList<>());

            for (String line : parts.get(i + 1).split("\\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;

                Matcher em = entryPattern.matcher(line);
                if (em.matches()) {
                    Map<String, String> e = new HashMap<>();
                    e.put("klasse", em.group(1));
                    e.put("stunde", em.group(2));
                    e.put("fach", em.group(3).trim());
                    e.put("raum", em.group(4));
                    e.put("lehrer", em.group(5));
                    e.put("info", em.group(6).trim());
                    result.get(day).add(e);
                }
            }
        }
        return result;
    }

    // ================= MAIN =================
    public static void main(String[] args) throws Exception {

        // ---- Zugangsdaten laden ----
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("config.properties"));
        } catch (Exception ignored) {}

        String user = System.getenv("BONNIWEB_USER");
        String pass = System.getenv("BONNIWEB_PASS");

        if (user == null) user = props.getProperty("username");
        if (pass == null) pass = props.getProperty("password");

        if (user == null || pass == null) {
            System.out.println("Keine Zugangsdaten gefunden.");
            return;
        }

        if (!login(user, pass)) {
            System.out.println("Login fehlgeschlagen.");
            return;
        }

        String planText = scrapePlan(
                "https://bonniweb.de/pluginfile.php/2992/mod_resource/content/11/w00024.htm"
        );
        if (planText.isEmpty()) {
            System.out.println("Zugriff auf den Plan fehlgeschlagen (Login-Seite erhalten).");
            return;
        }

        Map<String, List<Map<String, String>>> vertretungen =
                parseUntis(planText);

        System.out.println("Login erfolgreich\n");
        int[] count = {0};
        vertretungen.forEach((k, v) -> {
            if (!v.isEmpty()) {
                count[0] += v.size();
                System.out.println(k);
                v.forEach(e ->
                        System.out.println("  " + e)
                );
            }
        });

        if (count[0] == 0) {
            System.out.println("Keine Vertretungen gefunden.");
        }
    }
}
