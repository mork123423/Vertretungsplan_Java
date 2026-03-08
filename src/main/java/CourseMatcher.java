import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourseMatcher {
    public enum MatchStatus { NONE, OK, LEVEL_MISMATCH }

    public MatchStatus matchStatus(String fach, String lehrer, List<String> courses) {
        if (fach == null || fach.isEmpty()) return MatchStatus.NONE;
        String subject = fach.split("-", 2)[0].trim().toUpperCase();
        if (subject.isEmpty()) return MatchStatus.NONE;

        String teacher = lehrer == null ? "" : lehrer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (teacher.isEmpty()) return MatchStatus.NONE;
        String teacherShort = teacher.length() > 3 ? teacher.substring(0, 3) : teacher;
        String teacherShort2 = teacher.length() > 2 ? teacher.substring(0, 2) : teacher;

        String levelType = "";
        String levelNum = "";
        Matcher lm = Pattern.compile("-(GK|LK|ZK)(\\d)?").matcher(fach.toUpperCase());
        if (lm.find()) {
            levelType = lm.group(1);
            if (lm.groupCount() >= 2 && lm.group(2) != null) levelNum = lm.group(2);
        }

        String key1 = "-" + subject + "-" + teacher;
        String key2 = "-" + subject + "-" + teacherShort;
        String key3 = "-" + subject + "-" + teacherShort2;

        boolean anyTeacherMatch = false;
        boolean anyLevelMatch = false;

        for (String c : courses) {
            String s = c.replace(' ', '-').replace('_', '-').toUpperCase();
            if (!(s.contains(key1) || s.contains(key2) || s.contains(key3))) continue;

            anyTeacherMatch = true;
            if (levelType.isEmpty()) return MatchStatus.OK;
            if (courseMatchesLevel(s, levelType, levelNum)) anyLevelMatch = true;
        }

        if (!anyTeacherMatch) return MatchStatus.NONE;
        if (levelType.isEmpty()) return MatchStatus.OK;
        return anyLevelMatch ? MatchStatus.OK : MatchStatus.LEVEL_MISMATCH;
    }

    private boolean courseMatchesLevel(String s, String levelType, String levelNum) {
        String tokenBase = "-" + levelType; // -GK / -LK / -ZK
        if (levelNum == null || levelNum.isEmpty()) {
            return s.contains(tokenBase) || s.contains("-" + levelType.charAt(0) + "KB");
        }

        String pad2 = "0" + levelNum;
        String token1 = tokenBase + levelNum; // -GK2
        String token2 = "-" + levelType.charAt(0) + "KB" + pad2; // -GKB02 / -LKB02 / -ZKB02
        String token3 = "-" + levelType.charAt(0) + "KB" + levelNum;
        return s.contains(token1) || s.contains(token2) || s.contains(token3);
    }
}
