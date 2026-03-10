/**
 * Einfacher Datencontainer, der einen einzelnen Eintrag im Vertretungsplan
 * repräsentiert. Alle Felder sind öffentlich und unveränderlich, damit die
 * Klasse leichtgewichtig und einfach zu verwenden ist.
 */
public class PlanEntry {
    public final String klasse;
    public final String stunde;
    public final String fach;
    public final String raum;
    public final String lehrer;
    public final String info;

    public PlanEntry(String klasse, String stunde, String fach, String raum, String lehrer, String info) {
        // constructor simply stores the provided values into final fields
        this.klasse = klasse;
        this.stunde = stunde;
        this.fach = fach;
        this.raum = raum;
        this.lehrer = lehrer;
        this.info = info;
    }
}
