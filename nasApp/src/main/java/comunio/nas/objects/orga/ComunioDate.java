package comunio.nas.objects.orga;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.json.JSONObject;

/**
 * Erweiterte Kalender-Klasse für Comunio.
 * * Besonderheiten:
 * - Standard-Zeitpunkt ist immer 6 Uhr morgens (Comunio-Berechnungsrelevanz).
 * - Unterstützt robustes Parsing von ISO-8601 und deutschen Datumsformaten.
 * - Bietet Hilfsmethoden für Vergleiche und Datumsarithmetik.
 */
public class ComunioDate extends GregorianCalendar {

    private static final long serialVersionUID = 1L;

    public ComunioDate() {
        super();
        setFirstDayOfWeek(MONDAY);
        newComunioDay();
    }

    public ComunioDate(Date date) {
        this();
        if (date != null) {
            this.setTime(date);
        }
        newComunioDay();
    }

    public ComunioDate(ComunioDate comunioDate) {
        this(comunioDate.getTime());
    }

    public ComunioDate(JSONObject json) {
        this();
        if (json.has("timeInMillis")) {
            this.setTimeInMillis(json.getLong("timeInMillis"));
        }
        newComunioDay();
    }

    public ComunioDate(String datum) {
        this();
        try {
            this.setTime(parseDate(datum));
            // Comunio-Standardzeit erzwingen
            this.set(Calendar.HOUR_OF_DAY, 6);
            this.set(Calendar.MINUTE, 0);
            this.set(Calendar.SECOND, 0);
            this.set(Calendar.MILLISECOND, 0);
        } catch (ParseException e) {
            System.err.println("Fehler beim Parsen von Datum: " + datum + " -> Setze auf Default.");
        }
    }

    /**
     * Parst einen Datums-String in ein Date-Objekt.
     * Nutzt Kaskadierung, um verschiedene Formate sicher zu erkennen.
     */
    public static Date parseDate(String inputDate) throws ParseException {
        if (inputDate == null || inputDate.isEmpty()) {
            throw new ParseException("Datum ist leer", 0);
        }

        // 1. Versuch: ISO-Format (mit T)
        if (inputDate.contains("T")) {
            try {
                return Date.from(OffsetDateTime.parse(inputDate).toInstant());
            } catch (Exception e) {
                try {
                    return Date.from(LocalDateTime.parse(inputDate).atZone(ZoneId.systemDefault()).toInstant());
                } catch (Exception e2) { /* Nächster Versuch */ }
            }
        }

        // 2. Versuch: Deutsches Format (dd.MM.yyyy...)
        if (inputDate.contains(".")) {
            try {
                String pattern = inputDate.contains(":") ? "dd.MM.yyyy HH:mm" : "dd.MM.yyyy";
                if (inputDate.contains(" - ")) pattern = "dd.MM.yyyy - HH:mm";
                
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.GERMAN);
                return sdf.parse(inputDate);
            } catch (Exception e) { /* Nächster Versuch */ }
        }

        // 3. Versuch: YYYY-MM-DD Format
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
            return sdf.parse(inputDate);
        } catch (Exception e) {
            throw new ParseException("Format nicht erkannt: " + inputDate, 0);
        }
    }

    private void newComunioDay() {
        int hours = get(Calendar.HOUR_OF_DAY);
        set(Calendar.MINUTE, 0);
        set(Calendar.SECOND, 0);
        set(Calendar.MILLISECOND, 0);
        
        if (hours <= 5) {
            add(Calendar.DATE, -1);
        }
        set(Calendar.HOUR_OF_DAY, 6);
    }

    public ComunioDate addHours(int hours) {
        add(Calendar.HOUR_OF_DAY, hours);
        return this;
    }

    public ComunioDate addDays(int days) {
        add(Calendar.DAY_OF_MONTH, days);
        return this;
    }

    @Override
    public String toString() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).format(this.getTime());
    }

    public String toDebugString() {
        return new SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.GERMAN).format(this.getTime());
    }

    public boolean isSameDay(ComunioDate vergleichsDatum) {
        return this.get(Calendar.YEAR) == vergleichsDatum.get(Calendar.YEAR)
            && this.get(Calendar.MONTH) == vergleichsDatum.get(Calendar.MONTH)
            && this.get(Calendar.DAY_OF_MONTH) == vergleichsDatum.get(Calendar.DAY_OF_MONTH);
    }

    public boolean isBeforeDay(ComunioDate vergleichsDatum) {
        ComunioDate cal1 = new ComunioDate(this);
        ComunioDate cal2 = new ComunioDate(vergleichsDatum);
        // Da wir immer auf 6 Uhr setzen, reicht ein einfacher Vergleich nach 'before'
        return cal1.before(cal2);
    }

    public boolean isDateExpiredDays(int days) {
        ComunioDate expired = new ComunioDate(this);
        expired.addDays(days);
        ComunioDate now = new ComunioDate();
        return now.after(expired) || now.isSameDay(expired);
    }
}