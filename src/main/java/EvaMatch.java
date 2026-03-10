/**
 * Einfacher Wrapper, der einen <code>PlanEntry</code> (EVA‑Eintrag) mit
 * einer Markierung verbindet, ob das Kursniveau möglicherweise nicht mit
 * dem tatsächlichen Kurs des Nutzers übereinstimmt (z. B. GK vs LK). Die
 * Markierung wird verwendet, um in der GUI eine zusätzliche Warnung anzuzeigen.
 */
public class EvaMatch {
    public final PlanEntry entry;
    public final boolean levelMismatch;

    public EvaMatch(PlanEntry entry, boolean levelMismatch) {
        this.entry = entry;
        this.levelMismatch = levelMismatch;
    }
}
