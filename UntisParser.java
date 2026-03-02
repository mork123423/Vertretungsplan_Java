import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UntisParser {
    private static final Pattern DAY_PATTERN = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\s+\\p{L}+)");
    private static final Pattern ENTRY_PATTERN = Pattern.compile("^(\\S+)\\s+(\\d{1,2})\\s+(.*?)\\s{2,}(\\S+)\\s+(\\S+)\\s*(.*)$");
    private static final Pattern FALLBACK_PATTERN = Pattern.compile("^(\\S+)\\s+(\\d{1,2})\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*(.*)$");
    private static final Pattern COMPACT_PATTERN = Pattern.compile("^(Q\\d)(\\d{1,2})(.*?)(---|[A-Z]{2,5}\\d{3,4})(.+)$");

    public Map<String, List<PlanEntry>> parse(String text) {
        Map<String, List<PlanEntry>> result = new LinkedHashMap<>();
        List<String> parts = splitParts(text);

        for (int i = 1; i < parts.size(); i += 2) {
            String day = parts.get(i).trim();
            result.put(day, new ArrayList<>());

            for (String line : parts.get(i + 1).split("\\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Klasse(n)Stunde")) continue;

                Matcher em = ENTRY_PATTERN.matcher(line);
                if (!em.matches()) em = FALLBACK_PATTERN.matcher(line);
                if (em.matches()) {
                    result.get(day).add(new PlanEntry(
                            em.group(1),
                            em.group(2),
                            em.group(3).trim(),
                            em.group(4),
                            em.group(5),
                            em.group(6).trim()
                    ));
                    continue;
                }

                Matcher cm = COMPACT_PATTERN.matcher(line);
                if (cm.matches()) {
                    String tail = cm.group(5).trim();
                    String info = "";
                    if (tail.endsWith("EVA")) { info = "EVA"; tail = tail.substring(0, tail.length() - 3); }
                    else if (tail.endsWith("VA")) { info = "VA"; tail = tail.substring(0, tail.length() - 2); }

                    result.get(day).add(new PlanEntry(
                            cm.group(1),
                            cm.group(2),
                            cm.group(3).trim(),
                            cm.group(4),
                            tail.trim(),
                            info
                    ));
                }
            }
        }

        return result;
    }

    public LinkedHashMap<String, String> splitByDay(String text) {
        LinkedHashMap<String, String> blocks = new LinkedHashMap<>();
        List<String> parts = splitParts(text);

        for (int i = 1; i < parts.size(); i += 2) {
            String day = parts.get(i).trim();
            String block = parts.get(i + 1);
            blocks.put(day, block);
        }

        return blocks;
    }

    public String firstAvailableDay(String text) {
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

    private List<String> splitParts(String text) {
        List<String> parts = new ArrayList<>();
        Matcher m = DAY_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            parts.add(text.substring(last, m.start()));
            parts.add(m.group());
            last = m.end();
        }
        parts.add(text.substring(last));
        return parts;
    }
}
