package comunio.nas.objects.ligainsider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;

public class InjuryAndBlockedData {

	private String playerName;
	private String club;
	private String playerLink;
	private String playerId;
	private Instant abfrageTimestamp;
	private String reason;
	private String lastNewsLink;
	private String lastNewsText;
	private String sinceString;
	private Instant sinceTimestamp;
	private SpielerStatus status;
	
	public InjuryAndBlockedData() {
		this.abfrageTimestamp = Instant.now();	
		this.status = SpielerStatus.UNBESTIMMT;
	}

	public InjuryAndBlockedData(String name, String id, String playerLink,String club, String reason, String lastNewsLink, String lastNewsText, String sinceString, String status) {
		this.playerName = name;
		this.setPlayerId(id);
		this.setPlayerLink(playerLink);
		this.club = club;
		this.abfrageTimestamp = Instant.now();		
		this.reason = reason;
		this.lastNewsLink = lastNewsLink;
		this.lastNewsText = lastNewsText;
		this.setSinceString(sinceString);
		this.setStatus(status);
	}

	/**
	 * mögliche status von Ligainsider: Verletzung Aufbautraining Nicht im Kader
	 * Schwerer angeschlagen Gelb-Rote Karte Rote Karte
	 * 
	 * @param status2
	 * @return
	 */
	private SpielerStatus parseStatus(String status2) {
		switch (status2) {
		case "Verletzung":
			return SpielerStatus.VERLETZT;
		case "Aufbautraining":
			return SpielerStatus.AUFBAUTRAINING;
		case "Nicht im Kader":
			return SpielerStatus.NICHT_IM_KADER;
		case "Schwerer angeschlagen":
			return SpielerStatus.VERLETZT;
		case "Gelb-Rote Karte":
			return SpielerStatus.GELBROTE_KARTE;
		case "Rote Karte":
			return SpielerStatus.ROTE_KARTE;
		default:
			ComunioDataUpdater.errorDb.addError(new Error(ErrorType.LIGAINSIDERSTATUS, "Kein match für den Status: " + status2 + " gefunden! Spieler: " + this.playerName));
			return SpielerStatus.SONSTIGES;
		}
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("playerName", playerName);
		json.put("playerId", playerId);
		json.put("playerLink", playerLink);
		json.put("club", club);
		json.put("abfrageTimestamp", abfrageTimestamp != null ? abfrageTimestamp.toString() : null);
		json.put("reason", reason);
		json.put("lastNewsLink", lastNewsLink);
		json.put("lastNewsText", lastNewsText);
		json.put("sinceString", sinceString);
		json.put("sinceTimestamp", sinceTimestamp != null ? sinceTimestamp.toString() : null);
		json.put("status", status.toString());
		return json;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String name) {
		this.playerName = name;
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}

	public Instant getAbfrageTimestamp() {
		return abfrageTimestamp;
	}

	public void setAbfrageTimestamp(Instant abfrageTimestamp) {
		this.abfrageTimestamp = abfrageTimestamp;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getLastNewsLink() {
		return lastNewsLink;
	}

	public void setLastNewsLink(String lastNewsLink) {
		this.lastNewsLink = lastNewsLink;
	}

	public String getSinceString() {
		return sinceString;
	}

	public void setSinceString(String sinceString) {
		this.sinceString = sinceString;
		this.sinceTimestamp = parse(sinceString);
	}

	public Instant getSinceTimestamp() {
		return sinceTimestamp;
	}

	public void setSinceTimestamp(Instant sinceTimestamp) {
		this.sinceTimestamp = sinceTimestamp;
	}

	public SpielerStatus getStatus() {
		return status;
	}

	public void setStatus(SpielerStatus status) {
		this.status = status;
	}
	
	public void setStatus(String status) {
		this.status = parseStatus(status);
	}

	/**
	 * Wandelt einen deutschsprachigen relativen Zeittext wie "1 Woche und 3 Tagen"
	 * oder "3 Jahren und 8 Monaten" in einen Zeitpunkt der Vergangenheit um.
	 *
	 * Unterstützte Einheiten: - Jahr / Jahren - Monat / Monaten - Woche / Wochen -
	 * Tag / Tagen
	 *
	 * Der Text wird per Regex analysiert, die gefundenen Werte zu einer Period
	 * zusammengeführt und anschließend von Instant.now() abgezogen.
	 *
	 * @param text Eingabetext mit relativer Zeitangabe
	 * @return Instant, der um die angegebene Zeitspanne in der Vergangenheit liegt
	 */

	private final Pattern P = Pattern.compile("(\\d+)\\s*(Jahr|Jahren|Monat|Monaten|Woche|Wochen|Tag|Tagen)");

	public Instant parse(String text) {
	    int years = 0, months = 0, weeks = 0, days = 0;

	    Matcher m = P.matcher(text);
	    while (m.find()) {
	        int value = Integer.parseInt(m.group(1));
	        String unit = m.group(2);

	        if (unit.startsWith("Jahr")) years += value;
	        else if (unit.startsWith("Monat")) months += value;
	        else if (unit.startsWith("Woch")) weeks += value;
	        else days += value;
	    }

	    // Java 17: Wochen → Tage
	    Period p = Period.of(years, months, days + weeks * 7);

	    // Period auf LocalDate anwenden (Instant kann das nicht!)
	    LocalDate date = LocalDate.now().minus(p);

	    // Wieder zu Instant konvertieren
	    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
	}


	public String getLastNewsText() {
		return lastNewsText;
	}

	public void setLastNewsText(String lastNewsText) {
		this.lastNewsText = lastNewsText;
	}

	public String getPlayerLink() {
		return playerLink;
	}

	public void setPlayerLink(String playerLink) {
		this.playerLink = playerLink;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	@Override
	public String toString() {
		return this.playerName + ", " + this.club + ", " + this.sinceString;
		
	}
}
