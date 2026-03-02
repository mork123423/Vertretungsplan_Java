import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BonniwebClient {
    private static final String BASE_URL = "https://bonniweb.de";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final Map<String, String> cookies = new HashMap<>();

    public boolean login(String username, String password) throws Exception {
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

    public String fetchPlanText(String url) throws Exception {
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

    public List<String> fetchCourses() throws Exception {
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

    private boolean isLoginPage(Document doc) {
        return doc.selectFirst("input[name=username]") != null
                && doc.selectFirst("input[name=password]") != null;
    }
}
