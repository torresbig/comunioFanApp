package comunio.nas.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter CUSTOM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter READABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Parst einen String im Format mit +0200 Offset zu OffsetDateTime
    public static OffsetDateTime parse(String s) {
        return OffsetDateTime.parse(s, CUSTOM_FORMAT);
    }

    // Formatiert ein OffsetDateTime zurück in den String mit +0200 Offset
    public static String format(OffsetDateTime dt) {
        return dt.format(CUSTOM_FORMAT);
    }

    // Formatiert ein OffsetDateTime als lesbaren Datumsstring wie 25.08.2025
    public static String toReadableDate(OffsetDateTime dt) {
        return dt.format(READABLE_DATE_FORMAT);
    }
    
    // Addiert die verbleibenden Stunden zur aktuellen Zeit und gibt das Ergebnis als String im gewünschten Format zurück
    public static String addRemainingHoursAndFormat(int remainingHours) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime result = now.plusHours(remainingHours);
        return result.format(CUSTOM_FORMAT);
    }
    
    /** gibt die aktuelle Zeit als String im gewünschten Format zurück
     * Beispiel: 2023-10-05T14:30:00+0200
    */
    public static String getNowToString() {
        OffsetDateTime now = OffsetDateTime.now();
        return now.format(CUSTOM_FORMAT);
    }
    
    /** rückgabe im format dd.MM.yyyy HH:mm
    * Beispiel: 05.10.2023 14:30
    */
    public static String getNowToStringWithHours() {
    	   LocalDateTime jetzt = LocalDateTime.now();
           DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
           return jetzt.format(formatter);
    }
}