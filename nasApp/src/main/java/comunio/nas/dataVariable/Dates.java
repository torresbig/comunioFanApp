//package comunio.nas.dataVariable;
//
//public class Dates {
//	
//	public final static String seasonStart = "28.05.2025";
//	
//
//}

package comunio.nas.dataVariable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;

import comunio.nas.objects.orga.ComunioDate;

public class Dates {
	private static ComunioDate seasonStart = createSeasonStart();

	/**
	 * Erzeugt den Saisonstart (28.05.) dynamisch in Abhängigkeit vom aktuellen Datum.
	 * Liegt heute vor dem 28.05. des laufenden Jahres, wird das Vorjahr verwendet,
	 * ansonsten das laufende Jahr. Dies stellt sicher, dass die Saison erst am 28.05.
	 * beginnt und davor noch die alte Saison gilt.
	 *
	 * @return der korrekte Saisonstart als {@link ComunioDate}
	 */
	private static ComunioDate createSeasonStart() {
	    LocalDate today = LocalDate.now();
	    int currentYear = today.getYear();
	    LocalDate startCurrentYear = LocalDate.of(currentYear, Month.MAY, 25);
	    int seasonYear = today.isBefore(startCurrentYear) ? currentYear - 1 : currentYear;
	    return new ComunioDate("28.05." + seasonYear);
	}


	public static void setSeasonStart(ComunioDate newSeasonStart) {
		seasonStart = new ComunioDate(newSeasonStart);
	}

	public static void setSeasonStart(Instant newSeasonStart) {
		if(newSeasonStart == null) {
			return; // Keine Änderung, wenn null übergeben wird
		}
		seasonStart = new ComunioDate(Date.from(newSeasonStart));
	}

	public static String getSeasonStart() {
		return seasonStart.toString();
	}
}
