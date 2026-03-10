import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

/**
 * Kleiner HTTP‑Client, der mit der Website bonniweb.de kommuniziert. Er
 * führt einen Login durch, verwaltet Cookies für die Sitzung und bietet
 * Methoden zum Abrufen des Vertretungsplantexts, des Stundenplan‑PDF‑Texts
 * und der Kursliste.
 *
 * Die Implementierung nutzt Jsoup für HTML‑Anfragen und -Parsing und PDFBox
 * zur Textextraktion aus PDF‑Dokumenten. Der Code ist bewusst in einem
 * einfachen Stil gehalten, damit Anfänger der Ablauffolge folgen können.
 */
public class BonniwebClient {
    private static final String BASE_URL = "https://bonniweb.de";
    // pretend to be a modern browser, some sites reject unknown agents
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    // store cookies between requests so we stay logged in
    private final Map<String, String> cookies = new HashMap<>();

    /**
     * Meldet sich mit den übergebenen Zugangsdaten bei bonniweb an. Diese
     * Methode führt die übliche Reihe von GET/POST‑Anfragen aus, um Cookies
     * zu erhalten, und folgt Weiterleitungen, bis das Dashboard erreicht ist.
     * Ist auf der Zielseite kein Login‑Formular mehr vorhanden, gehen wir von
     * einem erfolgreichen Login aus.
     */
    public boolean login(String username, String password) throws Exception {
        // erste Anfragen, um vor dem Login vorhandene Cookies zu erhalten
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

        // ein paar Weiterleitungen folgen, um das endgültige Ziel zu erreichen
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

    /**
     * Lädt eine Klartextversion des Vertretungsplans von der angegebenen URL
     * herunter. Es wird einfach die Seite abgeholt, geprüft, dass wir nicht
     * zurück zur Login‑Seite geleitet wurden, und der gesamte Textkörper mit
     * normalisierten Zeilenenden zurückgegeben.
     */
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

    // Helfer, der der Moodle‑Ressourcenseite folgt und vor dem Herunterladen
    // die tatsächliche Plan‑URL ermittelt (die sich wöchentlich ändern kann).
    public String fetchPlanTextFromResource(String resourceUrl) throws Exception {
        String planUrl = resolveLatestPlanUrl(resourceUrl);
        if (planUrl == null || planUrl.isEmpty()) return "";
        return fetchPlanText(planUrl);
    }

    /**
     * Lädt ein PDF von der angegebenen URL herunter und verwendet dann
     * PDFBox, um lesbaren Text daraus zu extrahieren. Sieht die Antwort wie
     * HTML aus (was passiert, wenn wir zurück zur Loginseite geleitet
     * wurden), geben wir einen leeren String zurück.
     */
    public String fetchPdfText(String pdfUrl) throws Exception {
        Connection.Response resp = Jsoup.connect(pdfUrl)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .ignoreContentType(true)
                .followRedirects(true)
                .execute();
        String contentType = resp.contentType();
        if (contentType != null && contentType.toLowerCase().contains("text/html")) {
            return "";
        }
        byte[] bytes = resp.bodyAsBytes();
        try (PDDocument doc = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textPdf = stripper.getText(doc);
            return textPdf.replace("\r\n", "\n").replace("\r", "\n").replace('\u00A0', ' ');
        }
    }

    /**
     * Gegeben die URL einer Moodle‑Ressourcenseite: folge ihr und versuche, die
     * eigentliche URL des Vertretungsplans zu ermitteln. Moodle liefert oft
     * eine Wrapper‑Seite um ein <iframe> oder <embed>; diese Elemente suchen
     * wir daher zuerst. Als letzte Fallback‑Option durchsuchen wir alle
     * Links und wählen denjenigen mit der höchsten Wochenzahl im Namen aus.
     */
    public String resolveLatestPlanUrl(String resourceUrl) throws Exception {
        Connection.Response resp = Jsoup.connect(resourceUrl)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .followRedirects(true)
                .execute();
        Document doc = resp.parse();
        if (isLoginPage(doc)) return "";

        String finalUrl = resp.url().toString();
        if (finalUrl.toLowerCase().contains(".htm")) {
            return finalUrl;
        }

        // Prefer embedded/iframe plan (usually the current week)
        Element iframe = doc.selectFirst("iframe[src]");
        if (iframe != null) {
            String src = iframe.absUrl("src");
            if (!src.isEmpty()) return src;
        }
        Element embed = doc.selectFirst("embed[src]");
        if (embed != null) {
            String src = embed.absUrl("src");
            if (!src.isEmpty()) return src;
        }
        Element object = doc.selectFirst("object[data]");
        if (object != null) {
            String data = object.absUrl("data");
            if (!data.isEmpty()) return data;
        }

        // meta refresh fallback
        Element meta = doc.selectFirst("meta[http-equiv=refresh]");
        if (meta != null) {
            String content = meta.attr("content");
            int idx = content.toLowerCase().indexOf("url=");
            if (idx >= 0) {
                String href = content.substring(idx + 4).trim();
                String resolved = doc.baseUri().isEmpty() ? href : doc.baseUri() + href;
                if (!resolved.isEmpty()) return resolved;
            }
        }

        Elements links = doc.select("a[href]");
        String bestUrl = "";
        int bestNum = -1;
        for (Element a : links) {
            String href = a.absUrl("href");
            if (href == null || href.isEmpty()) continue;
            String lower = href.toLowerCase();
            if (!lower.contains(".htm")) continue;

            int n = extractWeekNumber(lower);
            if (n > bestNum) {
                bestNum = n;
                bestUrl = href;
            } else if (bestUrl.isEmpty()) {
                bestUrl = href;
            }
        }
        return bestUrl;
    }

    // Helfer, der in einem Dateinamen nach einer Wochenzahl sucht; wird von
    // <code>resolveLatestPlanUrl</code> verwendet, um den neuesten
    // Planlink auszuwählen.
    private int extractWeekNumber(String href) {
        // Match w00024.htm -> 24
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("w(\\d+)\\.htm").matcher(href);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return -1;
    }

    /**
     * Scrape the "my courses" page and return the list of course names.  The
     * CSS selector tries a few different patterns that have been observed on
     * bonniweb over time; we deduplicate the results using a LinkedHashSet
     * to preserve ordering.
     */
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

    // schnelle Heuristik, um zu entscheiden, ob eine abgeholte Seite
    // tatsächlich der Login-Bildschirm ist (in dem Fall sind wir nicht
    // authentifiziert)
    private boolean isLoginPage(Document doc) {
        return doc.selectFirst("input[name=username]") != null
                && doc.selectFirst("input[name=password]") != null;
    }

    /**
     * Einfache Kommandozeilen‑Smoke‑Tests. Die hier verwendeten Zugangsdaten
     * sind Platzhalter und sollten vor der manuellen Ausführung ersetzt werden.
     * Diese Methode dient ausschließlich Entwicklern, um zu überprüfen, ob
     * der Client funktioniert; die echte Anwendung verwendet die Methoden aus
     * <code>VertretungsplanApp</code>.
     */
    public static void main(String[] args) {
        BonniwebClient client = new BonniwebClient();
        
        System.out.println("Teste BonniwebClient-Funktionalität...\n");
        
        // Test 1: Anmeldung
        System.out.println("TEST 1: Anmeldung");
        try {
            boolean loginSuccess = client.login("Mazz_Lau", "truck91!");
            System.out.println("Ergebnis: " + (loginSuccess ? "✓ Anmeldung erfolgreich" : "✗ Anmeldung fehlgeschlagen"));
        } catch (Exception e) {
            System.out.println("✗ Anmeldefehler: " + e.getMessage());
        }
        
        // Test 2: Kurse abrufen
        System.out.println("\nTEST 2: Kurse abrufen");
        try {
            List<String> courses = client.fetchCourses();
            System.out.println("Ergebnis: ✓ Gefunden: " + courses.size() + " Kurs(e)");
            courses.forEach(c -> System.out.println("  - " + c));
        } catch (Exception e) {
            System.out.println("✗ Fehler beim Kurse abrufen: " + e.getMessage());
        }
        
        System.out.println("\nTests abgeschlossen!");
    }
}


