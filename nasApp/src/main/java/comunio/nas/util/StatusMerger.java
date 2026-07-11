package comunio.nas.util;

import java.util.LinkedHashSet;
import java.util.Set;

import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.player.Status;

/**
 * Zentrale Merging-Logik fuer Spieler-Status aus verschiedenen Datenquellen.
 * <p>
 * Diese Klasse fuehrt einen neuen Status (z.B. von Comunio, TmDe, Ligainsider)
 * mit dem bestehenden Status in der injuryDB zusammen.
 * </p>
 *
 * <p>Grundregeln:</p>
 * <ul>
 *   <li>Wenn {@code existing} null, {@code AKTIV} oder {@code UNBESTIMMT} ist:
 *       wird {@code newStatus} uebernommen, sofern er nicht aktiv oder unbestimmt ist.</li>
 *   <li>Wenn {@code newStatus} null, {@code AKTIV} oder {@code UNBESTIMMT} ist:
 *       bleibt {@code existing} erhalten.</li>
 *   <li>Wenn beide nicht-aktiv sind: Datums-, Text- und Severity-Felder werden intelligent zusammengefuehrt.</li>
 *   <li>Jede Quelle wird in der {@code quellen}-Liste vermerkt.</li>
 * </ul>
 */
public class StatusMerger {

    /**
     * Merged einen neuen Status aus einer Quelle in den bestehenden Status.
     *
     * <p>Der Merge ist so ausgelegt, dass doppelte Textteile in {@code grund}
     * und {@code details} vermieden werden. Bereits vorhandene doppelte Inhalte
     * werden vor dem Zusammenfuehren normalisiert.</p>
     *
     * @param existing  aktuell in der injuryDB gespeicherter Status; darf null sein
     * @param newStatus neuer Status aus der Quelle; darf null sein
     * @param source    Quellenname, z.B. {@code Comunio}, {@code TmDe}, {@code Ligainsider}
     * @return gemergter Status; nie null
     */
    public static Status merge(Status existing, Status newStatus) {
       


        // Fall 3: Beide haben nicht-aktiven Status -> mergen
        mergeDateFields(existing, newStatus);
        mergeTextField(existing, newStatus, "grund");
        mergeTextField(existing, newStatus, "details");
        return existing;
    }

    /**
     * Prueft, ob der Status als inaktiv oder nicht relevant gilt.
     *
     * <p>In diesem Kontext sind {@code AKTIV} und {@code UNBESTIMMT}
     * nicht merge-relevant, weil sie keinen konkreten Verletzungsstatus darstellen.</p>
     *
     * @param status zu pruefender Status
     * @return true, wenn der Status null, aktiv oder unbestimmt ist
     */
    private static boolean isInactiveOrNotRelevant(Status status) {
        if (status == null) {
            return true;
        }
        SpielerStatus s = status.getStatus();
        return s == null || s.isAKTIV() || s.isUNBESTIMMT();
    }

    /**
     * Merged die Datumsfelder {@code seit} und {@code bis}.
     *
     * <ul>
     *   <li>{@code seit}: fruehesten (aeltesten) Wert behalten</li>
     *   <li>{@code bis}: spaetesten Wert behalten</li>
     * </ul>
     *
     * <p>Bei ungueltigen Datumswerten bleibt der vorhandene Wert erhalten.</p>
     *
     * @param existing bestehender Status
     * @param newStatus neuer Status
     */
    private static void mergeDateFields(Status existing, Status newStatus) {
        if (isBlankOrUnknown(existing.getSeit()) && !isBlankOrUnknown(newStatus.getSeit())) {
            existing.setSeit(newStatus.getSeit());
        } else if (!isBlankOrUnknown(existing.getSeit()) && !isBlankOrUnknown(newStatus.getSeit())) {
            try {
                ComunioDate existingDate = new ComunioDate(existing.getSeit());
                ComunioDate newDate = new ComunioDate(newStatus.getSeit());
                if (newDate.before(existingDate)) {
                    existing.setSeit(newStatus.getSeit());
                }
            } catch (Exception e) {
                // Bei Fehlern den vorhandenen Wert behalten.
            }
        }

        if (isBlankOrUnknown(existing.getBis()) && !isBlankOrUnknown(newStatus.getBis())) {
            existing.setBis(newStatus.getBis());
        } else if (!isBlankOrUnknown(existing.getBis()) && !isBlankOrUnknown(newStatus.getBis())) {
            try {
                ComunioDate existingDate = new ComunioDate(existing.getBis());
                ComunioDate newDate = new ComunioDate(newStatus.getBis());
                if (newDate.after(existingDate)) {
                    existing.setBis(newStatus.getBis());
                }
            } catch (Exception e) {
                // Bei Fehlern den vorhandenen Wert behalten.
            }
        }
    }

    /**
     * Merged ein Textfeld ({@code grund} oder {@code details}) mit Quellenangabe.
     *
     * <p>Logik:</p>
     * <ul>
     *   <li>Vorhandene Werte werden zuerst normalisiert und von Duplikaten bereinigt.</li>
     *   <li>Neue Inhalte werden nur angehaengt, wenn der reine Text noch nicht vorhanden ist.</li>
     *   <li>Die Quelle wird im Format {@code [quelle: text]} gespeichert.</li>
     * </ul>
     *
     * <p>Wichtig: Der Vergleich erfolgt auf dem reinen Text, nicht auf der Quelle.</p>
     *
     * @param existing  bestehender Status
     * @param newStatus neuer Status
     * @param source    Quellenname
     * @param fieldName  entweder {@code grund} oder {@code details}
     */
    private static void mergeTextField(Status existing, Status newStatus, String fieldName) {
        String existingValue = "grund".equals(fieldName) ? existing.getGrund() : existing.getDetails();
        String newValue = "grund".equals(fieldName) ? newStatus.getGrund() : newStatus.getDetails();

        existingValue = normalizeTextField(existingValue);
        newValue = normalizeTextField(newValue);

        String merged;

        if (isBlankOrUnknown(existingValue) && isBlankOrUnknown(newValue)) {
            merged = existingValue != null ? existingValue : "";
        } else if (isBlankOrUnknown(existingValue)) {
            merged = formatWithSource(newStatus.getQuelle(), extractSingleText(newValue));
        } else if (isBlankOrUnknown(newValue)) {
            merged = existingValue;
        } else {
            Set<String> existingTexts = extractTexts(existingValue);
            String trimmedNew = extractSingleText(newValue);

            if (existingTexts.contains(trimmedNew)) {
                merged = joinUniqueParts(existingValue);
            } else {
                merged = joinUniqueParts(existingValue) + " | " + formatWithSource(newStatus.getQuelle(), trimmedNew);
            }
        }

        if ("grund".equals(fieldName)) {
            existing.setGrund(merged);
        } else {
            existing.setDetails(merged);
        }
    }



    /**
     * Ermittelt den Schweregrad eines Status.
     *
     * <p>Hoehere Werte bedeuten hoehere Prioritaet.</p>
     *
     * @param status zu bewertender Status
     * @return Severity-Wert, wobei 0 den niedrigsten Wert darstellt
     */
    private static int getSeverity(SpielerStatus status) {
        if (status == null) {
            return 0;
        }
        if (status.isVERLETZT()) return 5;
        if (status.isAUFBAUTRAINING()) return 4;
        if (status.isGESPERRT()) return 3;
        if (status.isROTE_KARTE()) return 3;
        if (status.isGELBROTE_KARTE()) return 3;
        if (status.isFUENFTE_GELBE_KARTE()) return 3;
        if (status.isNICHT_IM_KADER()) return 2;
        if (status.isNICHT_IN_LIGA()) return 1;
        if (status.isSONSTIGES()) return 1;
        return 0;
    }

    /**
     * Prueft, ob ein String leer oder semantisch unbekannt ist.
     *
     * <p>Zusammen mit der Normalisierung sorgt diese Pruefung dafuer, dass
     * Platzhalter wie {@code unbekannt} und leere Werte einheitlich behandelt
     * werden.</p>
     *
     * @param value zu pruefender String
     * @return true, wenn der String null, leer oder "unbekannt" ist
     */
    private static boolean isBlankOrUnknown(String value) {
        return value == null || value.trim().isEmpty() || "unbekannt".equalsIgnoreCase(value.trim());
    }

    /**
     * Entfernt doppelte Textteile aus einem Feld und normalisiert auf reine
     * Textinhalte.
     *
     * <p>Unterstuetzte Formate:</p>
     * <ul>
     *   <li>Rohtext: {@code Schulterverletzung}</li>
     *   <li>Quellenformat: {@code [transfermarkt: Schulterverletzung]}</li>
     *   <li>Mischformen mit Trennzeichen {@code |}</li>
     * </ul>
     *
     * <p>Die Reihenfolge der ersten Vorkommen bleibt erhalten.</p>
     *
     * @param value Eingabewert
     * @return normalisierter String ohne doppelte Textteile
     */
    private static String normalizeTextField(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return "";
        }
        if (isBlankOrUnknown(value)) {
            return value.trim();
        }

        Set<String> uniqueTexts = new LinkedHashSet<>();
        for (String part : value.split("\\|")) {
            String text = extractSingleText(part);
            if (!text.isBlank()) {
                uniqueTexts.add(text);
            }
        }

        if (uniqueTexts.isEmpty()) {
            return "";
        }
        return String.join(" | ", uniqueTexts);
    }

    /**
     * Extrahiert aus einem einzelnen Segment den eigentlichen Text.
     *
     * <p>Beispiele:</p>
     * <ul>
     *   <li>{@code "Kreuzbandriss"} -> {@code "Kreuzbandriss"}</li>
     *   <li>{@code "[transfermarkt: Kreuzbandriss]"} -> {@code "Kreuzbandriss"}</li>
     * </ul>
     *
     * @param part Segment, das analysiert werden soll
     * @return reiner Text ohne Quellenpraefix
     */
    private static String extractSingleText(String part) {
        if (part == null) {
            return "";
        }

        String trimmed = part.trim();
        if (trimmed.startsWith("[") && trimmed.contains(":")) {
            int colonIndex = trimmed.indexOf(':', 1);
            int endIndex = trimmed.lastIndexOf(']');
            if (colonIndex != -1) {
                if (endIndex != -1 && endIndex > colonIndex) {
                    return trimmed.substring(colonIndex + 1, endIndex).trim();
                }
                return trimmed.substring(colonIndex + 1).trim();
            }
        }
        return trimmed;
    }

    /**
     * Extrahiert alle Textinhalte aus einem zusammengesetzten Feld.
     *
     * @param value zusammengesetzter Feldwert
     * @return eindeutige Textinhalte in Einfuege-Reihenfolge
     */
    private static Set<String> extractTexts(String value) {
        Set<String> texts = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return texts;
        }

        for (String part : value.split("\\|")) {
            String text = extractSingleText(part);
            if (!text.isBlank()) {
                texts.add(text);
            }
        }

        return texts;
    }

    /**
     * Entfernt doppelte Teile aus einem bereits zusammengesetzten Textwert und
     * gibt die bereinigte Darstellung zurueck.
     *
     * @param value zusammengesetzter Textwert
     * @return bereinigter String mit eindeutigen Segmenten
     */
    private static String joinUniqueParts(String value) {
        Set<String> texts = extractTexts(value);
        if (texts.isEmpty()) {
            return value == null ? "" : value.trim();
        }
        return String.join(" | ", texts);
    }

    /**
     * Formatiert einen Text mit Quellenangabe.
     *
     * @param source Quellenname
     * @param text   eigentlicher Text
     * @return formatierter String im Muster {@code [quelle: text]}
     */
    private static String formatWithSource(String source, String text) {
        String safeSource = source == null ? "" : source.trim();
        String safeText = text == null ? "" : text.trim();
        return "[" + safeSource + ": " + safeText + "]";
    }
}