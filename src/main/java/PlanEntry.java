public class PlanEntry {
    public final String klasse;
    public final String stunde;
    public final String fach;
    public final String raum;
    public final String lehrer;
    public final String info;

    public PlanEntry(String klasse, String stunde, String fach, String raum, String lehrer, String info) {
        this.klasse = klasse;
        this.stunde = stunde;
        this.fach = fach;
        this.raum = raum;
        this.lehrer = lehrer;
        this.info = info;
    }
}
