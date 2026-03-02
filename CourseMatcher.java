import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourseMatcher {
    public boolean matchesCourse(String fach, String lehrer, List<String> courses) {
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
}
