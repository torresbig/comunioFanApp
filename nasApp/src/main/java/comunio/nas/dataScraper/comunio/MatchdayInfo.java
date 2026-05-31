package comunio.nas.dataScraper.comunio;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.EventInfo;

/**
 * Klasse stellt die Informationen zum aktuellen Spieltag bereit. Die Daten
 * werden beim ersten Aufruf aus der Comunio-API geholt und gecached. Jeder
 * weitere Aufruf in derselben Programmsitzung gibt den gespeicherten Wert
 * zurück. Enthält außerdem frühestes und spätestes Kickoff-Datum über alle
 * Spiele des Spieltags.
 */
public class MatchdayInfo {

	private final int currentMatchday;
	private final boolean isFinished;
	private boolean fetchNow = false;
	private final boolean isStarted;
	private final boolean isShifted;
	private final ZonedDateTime earliestKickoff;
	private final ZonedDateTime latestKickoff;
	private final EventInfo eventInfo;

	private int pointsMatchday;

	// Statischer Cache für die Laufzeit der aktuellen Programmausführung
	private static MatchdayInfo cached = null;

	/**
	 * Konstruktor (privat, zur Erzeugung über fetchCurrentMatchday)
	 */
	private MatchdayInfo(int currentMatchday, boolean isStarted, boolean isFinished, ZonedDateTime earliestKickoff,
			ZonedDateTime latestKickoff, EventInfo eventInfo, boolean isShifted) {
		this.currentMatchday = currentMatchday;
		this.isFinished = isFinished;
		this.isStarted = isStarted;
		this.earliestKickoff = earliestKickoff;
		this.latestKickoff = latestKickoff;
		this.pointsMatchday = getPointsMatchday();
		this.eventInfo = eventInfo;
		this.isShifted = isShifted;

	}

	public void setFetchNow() {
		this.fetchNow = true;
	}

	/**
	 * Gibt die aktuelle Spieltagsnummer zurück.
	 */
	public int getCurrentMatchday() {
		return currentMatchday;
	}

	/**
	 * Gibt zurück, ob der Spieltag abgeschlossen ist.
	 */
	public boolean isFinished() {
		return isFinished;
	}

	/**
	 * Gibt zurück, ob der Spieltag begonnen hat.
	 */
	public boolean isStarted() {
		return isStarted;
	}

	/**
	 * Frühestes Kickoff-Datum der Spiele dieses Spieltags.
	 */
	public ZonedDateTime getEarliestKickoff() {
		return earliestKickoff;
	}

	/**
	 * Spätestes Kickoff-Datum der Spiele dieses Spieltags.
	 */
	public ZonedDateTime getLatestKickoff() {
		return latestKickoff;
	}

	/**
	 * Holt die aktuellen Spieltagsinformationen von der Comunio-API. Beim ersten
	 * Aufruf werden die Daten über die API geladen, beim Folgeruf werden die
	 * gespeicherten (gecacheten) Daten zurückgegeben.
	 *
	 * @return MatchdayInfo-Objekt mit allen Infos
	 */
	public static MatchdayInfo fetchCurrentMatchday() {

		Logger logger = Logger.getLogger(MatchdayInfo.class.getName());

		// Cache vorhanden? Zurückgeben!
		if (cached != null) {
			return cached;
		}

		try {
			// Abfrage-URL
			String url = Urls.COM_MATCHDAY;
			logger.info("Hole aktuellen Spieltag von: " + url);

			// API-Abfrage per Jsoup
			String jsonResponse = Jsoup.connect(url)//
					.userAgent(Login.USER_AGENT)//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Accept-Encoding", "gzip, deflate, br, zstd")//
					.header("Accept-Language", "de-DE,en-EN;q=0.9")//
					.header("x-timezone", "Europe/Berlin")//
					.ignoreContentType(true)//
					.execute()//
					.body();

			JSONObject matchdayData = new JSONObject(jsonResponse);

			int matchday = matchdayData.getInt("matchdayKey");
			boolean isFinished = matchdayData.getBoolean("finished");
			boolean isStarted = matchdayData.getBoolean("started");

			// Kickoff-Zeitpunkte suchen
			ZonedDateTime earliest = null;
			ZonedDateTime latest = null;
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

			boolean isShifted = false;

			if (matchdayData.has("type")) {
				if (matchdayData.getString("type").equalsIgnoreCase("SHIFTED")) {
					logger.info("NACHHOLSPIEL!!!");
					isShifted = true;
				}

			}

			if (matchdayData.has("items")) {
				JSONArray items = matchdayData.getJSONArray("items");
				for (int i = 0; i < items.length(); i++) {
					JSONObject item = items.getJSONObject(i);
					if (item.has("kickoff")) {
						try {
							String kickoffStr = item.getString("kickoff");
							ZonedDateTime kickoff = ZonedDateTime.parse(kickoffStr, fmt);
							if (earliest == null || kickoff.isBefore(earliest)) {
								earliest = kickoff;
							}
							if (latest == null || kickoff.isAfter(latest)) {
								latest = kickoff;
							}
						} catch (Exception pe) {
							logger.log(Level.WARNING,
									"Fehler beim Parsen von kickoff '" + item.optString("kickoff") + "'", pe);
						}
					}
				}
			}
			EventInfo eventInfo = null;
			if (matchdayData.has("eventInfo")) {
				try {
					eventInfo = EventInfo.fromJson(matchdayData.getJSONObject("eventInfo"));
				} catch (Exception e) {
					logger.log(Level.WARNING, "Fehler beim Parsen von eventInfo", e);
				}
			}

			cached = new MatchdayInfo(matchday, isStarted, isFinished, earliest, latest, eventInfo, isShifted);
			logger.info("Matchday geladen: " + matchday + " (Started: " + isStarted + ", Finished: " + isFinished
					+ ", Frühester Kickoff: " + earliest + ", Spätester Kickoff: " + latest + ")");
			return cached;

		} catch (Exception e) {
			logger.log(Level.WARNING, "Fehler bei Spieltagprüfung", e);
			cached = new MatchdayInfo(-1, false, false, null, null, null, false);
			return cached;
		}
	}

	/**
	 * Macht den Laufzeit-Cache explizit leer. Kann genutzt werden, um einen
	 * erneuten Fetch zu erzwingen.
	 */
	public static void clearCache() {
		cached = null;
	}

	public int getPointsMatchday() {
		if (!isStarted && !isFinished) {
			// Saison noch nicht gestartet, Spieltag bisher abgelaufen
			return Math.max(currentMatchday - 1, 0); // Nicht negativ
		} else if (isFinished) {
			// Spieltag abgeschlossen
			return currentMatchday;
		} else if (isStarted) {
			// Laufender Spieltag, Punkte nur vom vorherigen Spieltag
			return Math.max(currentMatchday - 1, 0);
		} else {
			return 0;
		}
	}

	/**
	 * Prüft, ob ein neuer Spieltag vorliegt gegenüber dem zuletzt verarbeiteten.
	 * 
	 * @param lastProcessedMatchday zuletzt verarbeiteter Spieltag (z.B. aus DB)
	 * @return true, wenn aktueller Spieltag größer ist (neuer Spieltag da)
	 */
	public boolean isNewMatchday(int lastProcessedMatchday) {
		return this.currentMatchday > lastProcessedMatchday;
	}

	/**
	 * Prüft, ob die Punkte für den aktuellen (oder letzten) Spieltag abgefragt
	 * werden dürfen. Das heißt: Ist der Spieltag abgeschlossen oder ist es bereits
	 * 5 Uhr am Tag nach Kickoff?
	 * 
	 * @return true, wenn Punkte abgefragt werden können
	 */
	public boolean canFetchPoints() {
		if (isFinished || fetchNow) {
			return true; // Spieltag ist beendet, Punkte verfügbar
		}
		if (pointsMatchday < currentMatchday && !isFinished) {
			return true;
		}
		if (latestKickoff == null) {
			return false; // Kein Datum, zu unsicher
		}
		// 5 Uhr morgens am Tag nach dem Kickoff
		ZonedDateTime fiveAMNextDay = latestKickoff.toLocalDate().plusDays(1).atTime(5, 0)
				.atZone(latestKickoff.getZone());
		boolean time = ZonedDateTime.now(latestKickoff.getZone()).isAfter(fiveAMNextDay);
		return time;
	}

	public EventInfo getEventInfo() {
		return eventInfo;
	}

	/**
	 * Serialisiert das MatchdayInfo-Objekt als JSONObject. Beachte: ZonedDateTime
	 * wird als RFC 3339 String abgespeichert.
	 */
	public JSONObject toJson() {
		JSONObject result = new JSONObject();
		// Pflichtfelder
		result.put("currentMatchday", currentMatchday);
		result.put("isFinished", isFinished);
		result.put("isStarted", isStarted);
		result.put("isShifted", isShifted);

		// optionale Felder (nur wenn nicht null)
		DateTimeFormatter fmt = DateTimeFormatter.ISO_ZONED_DATE_TIME;
		result.put("earliestKickoff", earliestKickoff != null ? earliestKickoff.format(fmt) : JSONObject.NULL);
		result.put("latestKickoff", latestKickoff != null ? latestKickoff.format(fmt) : JSONObject.NULL);
		result.put("pointsMatchday", pointsMatchday);

		// EventInfo serialisieren
		result.put("eventInfo", eventInfo != null ? eventInfo.toJson() : JSONObject.NULL);

		// fetchNow als Zustand (optional, weil es ein temporärer Trigger ist)
		result.put("fetchNow", fetchNow);

		return result;
	}

	/**
	 * Deserialisiert ein MatchdayInfo-Objekt aus einem JSONObject. Erfordert ein
	 * vergleichbares Format wie von toJson() erzeugt.
	 */
	public static MatchdayInfo fromJson(JSONObject obj) {
		if (obj == null || obj.isEmpty()) {
			return null;
		}
		int currentMatchday = obj.optInt("currentMatchday", -1);
		boolean isFinished = obj.optBoolean("isFinished", false);
		boolean isStarted = obj.optBoolean("isStarted", false);
		boolean isShifted = obj.optBoolean("isShifted", false);

		DateTimeFormatter fmt = DateTimeFormatter.ISO_ZONED_DATE_TIME;

		// earliestKickoff und latestKickoff parsen
		ZonedDateTime earliestKickoff = null;
		ZonedDateTime latestKickoff = null;
		String earliestStr = obj.optString("earliestKickoff", null);
		if (earliestStr != null && !"null".equals(earliestStr)) {
			earliestKickoff = ZonedDateTime.parse(earliestStr, fmt);
		}
		String latestStr = obj.optString("latestKickoff", null);
		if (latestStr != null && !"null".equals(latestStr)) {
			latestKickoff = ZonedDateTime.parse(latestStr, fmt);
		}

		// EventInfo parsen
		EventInfo eventInfo = null;
		if (obj.has("eventInfo") && !obj.isNull("eventInfo")) {
			eventInfo = EventInfo.fromJson(obj.getJSONObject("eventInfo"));
		}

		// MatchdayInfo erzeugen
		MatchdayInfo info = new MatchdayInfo(currentMatchday, isStarted, isFinished, earliestKickoff, latestKickoff,
				eventInfo, isShifted);

		// Punktewert setzen, falls im JSON vorhanden
		if (obj.has("pointsMatchday")) {
			info.pointsMatchday = obj.getInt("pointsMatchday");
		}

		// fetchNow als Zustand setzen (wird normalerweise nur temporär verwendet)
		if (obj.optBoolean("fetchNow", false)) {
			info.setFetchNow();
		}

		return info;
	}

	/**
	 * Prüft, ob die aktuelle Saison beendet ist.
	 * <p>
	 * Die Saison gilt als beendet, wenn der aktuelle Spieltag >= 34 ist und
	 * abgeschlossen ist. Ausnahme: Wenn der Saisonstart aus den LastUpdates
	 * nach dem letzten Kickoff des aktuellen Spieltags liegt, dann hat bereits
	 * eine neue Saison begonnen – in diesem Fall ist die Saison nicht vorbei
	 * (sondern die neue läuft).
	 *
	 * @param lastUpdates die letzten Update-Informationen mit Saisonstart-Datum
	 * @return {@code true}, wenn die Saison beendet ist
	 */
	public boolean isSeasonOver(LastUpdates lastUpdates) {
		// Grundvoraussetzungen prüfen
		if (currentMatchday < 34) {
			return false; // Noch nicht am letzten Spieltag
		}

		if (!isFinished) {
			return false; // 34. Spieltag noch nicht abgeschlossen
		}

		// Prüfen, ob eine neue Saison bereits gestartet ist
		if (lastUpdates == null || lastUpdates.getSeasonStart() == null) {
			System.err.println("Warnung: Keine Saisonstart-Informationen vorhanden – nehme Saisonende an.");
			return true;
		}

		// Wenn der Saisonstart NACH dem letzten Kickoff dieses Spieltags liegt,
		// dann ist der aktuelle Spieltag bereits ein Spieltag der neuen Saison.
		// (z.B. API zeigt noch den alten 34. Spieltag an, aber die neue Saison läuft
		// schon)
		if (lastUpdates.getSeasonStart().isAfter(latestKickoff.toInstant())) {
			return false; // Neue Saison hat begonnen
		}

		return true; // Letzter Spieltag abgeschlossen, keine neue Saison
	}

	public boolean askForTransfers(LastUpdates lastUpdates) {
		if(this.getCurrentMatchday() < 34){
			return true;
		}
		if (!isFinished) {
			return true; // 34. Spieltag noch nicht abgeschlossen
		}

		if (lastUpdates == null || lastUpdates.getSeasonStart() == null) {
			return false;
		}

		if (lastUpdates.getSeasonStart().isAfter(latestKickoff.toInstant())) {
			return true; // Neue Saison hat begonnen
		} else {
			return false;
		}
		
	}

	public boolean isStuckBetweenTheSeasons(LastUpdates lastUpdates) {
	
		Instant seasonTransmission = lastUpdates.getSeasonStart();

		if (seasonTransmission == null) {
			return false;
		}

		// Wenn der Saisonstart NACH dem letzten Kickoff dieses Spieltags liegt,
		// dann ist der aktuelle Spieltag bereits ein Spieltag der neuen Saison.
		// (z.B. API zeigt noch den alten 34. Spieltag an, aber die neue Saison läuft
		// schon)
		Instant latestKickoff = getLatestKickoff().toInstant();
		if (lastUpdates.getSeasonStart().isAfter(latestKickoff)) {
			return true; // Neue Saison hat begonnen
		}
		return false;

	}


	public boolean isShifted() {
		return isShifted;
	}

}
