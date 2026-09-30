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

public class InjuryAndBlockedData implements LigainsiderInterface {

	private String playerName;
	private String club;
	private String playerLink;
	private String ligainsiderPlayerId;
	private String comunioPlayerId; // für das Mapping
	private Instant abfrageTimestamp;
	private String reason;
	private String lastNewsLink;
	private String lastNewsText;
	private String sinceString;
	private Instant sinceTimestamp;
	private Instant statusChangeTimestamp;
	private String statusChangeString;
	private SpielerStatus status;
	private String quelle;

	public InjuryAndBlockedData() {
		this.abfrageTimestamp = Instant.now();
		this.status = SpielerStatus.UNBESTIMMT;
	}

	public InjuryAndBlockedData(String name, String ligainsiderPlayerId, String comunioPlayerId, String playerLink, String club, String reason, String lastNewsLink, String lastNewsText, String sinceString, String statusChangeTimestamp, String status, String quelle) {
		this.playerName = name;
		this.setLigainsiderPlayerId(ligainsiderPlayerId);
		this.setComunioPlayerId(comunioPlayerId);
		this.setPlayerLink(playerLink);
		this.club = club;
		this.abfrageTimestamp = Instant.now();
		this.reason = reason;
		this.lastNewsLink = lastNewsLink;
		this.lastNewsText = lastNewsText;
		this.setSinceString(sinceString);
		this.setStatusChangeString(statusChangeTimestamp);
		this.setStatus(status);
		this.setQuelle(quelle);
	}

	public InjuryAndBlockedData(InjuryAndBlockedData other) {
		if (other != null) {
			this.playerName = other.playerName;
			this.club = other.club;
			this.playerLink = other.playerLink;
			this.ligainsiderPlayerId = other.ligainsiderPlayerId;
			this.comunioPlayerId = other.comunioPlayerId;
			this.abfrageTimestamp = other.abfrageTimestamp;
			this.reason = other.reason;
			this.lastNewsLink = other.lastNewsLink;
			this.lastNewsText = other.lastNewsText;
			this.sinceString = other.sinceString;
			this.sinceTimestamp = other.sinceTimestamp;
			this.statusChangeTimestamp = other.statusChangeTimestamp;
			this.statusChangeString = other.statusChangeString;
			this.status = other.status;
			this.quelle = other.quelle;
		}
	}
	
	public static InjuryAndBlockedData setAsActive(String comunioPlayerId, String quelle, InjuryAndBlockedData injuredPlayerData) {
		InjuryAndBlockedData result = new InjuryAndBlockedData(injuredPlayerData);
		
		result.setComunioPlayerId(comunioPlayerId);
		result.setQuelle(quelle);
		result.setStatus(SpielerStatus.AKTIV);
		result.setSinceTimestamp(Instant.now());
		result.setLastNewsText("Spieler ist wieder gesund");

		return result;
	}
	
	public static InjuryAndBlockedData setAsNotInLiga(String comunioPlayerId, String quelle, InjuryAndBlockedData injuredPlayerData) {
		InjuryAndBlockedData result = new InjuryAndBlockedData(injuredPlayerData);
		
		result.setComunioPlayerId(comunioPlayerId);
		result.setQuelle(quelle);
		result.setStatus(SpielerStatus.NICHT_IN_LIGA);
		result.setSinceTimestamp(Instant.now());
		result.setLastNewsText("Spieler ist nicht mehr in der liga!");

		return result;
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

	public static InjuryAndBlockedData fromJSON(JSONObject json) {
		InjuryAndBlockedData data = new InjuryAndBlockedData();
		data.playerName = json.optString("playerName");
		data.ligainsiderPlayerId = json.optString("ligainsiderPlayerId");
		data.playerLink = json.optString("playerLink");
		data.club = json.optString("club");
		data.comunioPlayerId = json.optString("comunioPlayerId");
		data.abfrageTimestamp = json.has("abfrageTimestamp") && !json.isNull("abfrageTimestamp") ? Instant.parse(json.getString("abfrageTimestamp")) : null;
		data.reason = json.optString("reason");
		data.lastNewsLink = json.optString("lastNewsLink");
		data.lastNewsText = json.optString("lastNewsText");
		data.sinceString = json.optString("sinceString");
		data.sinceTimestamp = json.has("sinceTimestamp") && !json.isNull("sinceTimestamp") ? Instant.parse(json.getString("sinceTimestamp")) : null;
		data.statusChangeString = json.optString("statusChangeString");
		data.statusChangeTimestamp = json.has("statusChangeTimestamp") && !json.isNull("statusChangeTimestamp") ? Instant.parse(json.getString("statusChangeTimestamp")) : null;
		data.status = SpielerStatus.valueOf(json.optString("status", "SONSTIGES"));
		data.quelle = json.optString("quelle");
		return data;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("playerName", playerName);
		json.put("ligainsiderPlayerId", ligainsiderPlayerId);
		json.put("playerLink", playerLink);
		json.put("club", club);
		json.put("comunioPlayerId", comunioPlayerId);
		json.put("abfrageTimestamp", abfrageTimestamp != null ? abfrageTimestamp.toString() : null);
		json.put("reason", reason);
		json.put("lastNewsLink", lastNewsLink);
		json.put("lastNewsText", lastNewsText);
		json.put("sinceString", sinceString);
		json.put("sinceTimestamp", sinceTimestamp != null ? sinceTimestamp.toString() : null);
		json.put("statusChangeString", statusChangeString);
		json.put("statusChangeTimestamp", statusChangeTimestamp != null ? statusChangeTimestamp.toString() : null);
		json.put("status", status.toString());
		json.put("quelle", quelle);
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

			if (unit.startsWith("Jahr"))
				years += value;
			else if (unit.startsWith("Monat"))
				months += value;
			else if (unit.startsWith("Woch"))
				weeks += value;
			else
				days += value;
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

	public String getLigainsiderPlayerId() {
		return ligainsiderPlayerId;
	}

	public void setLigainsiderPlayerId(String playerId) {
		this.ligainsiderPlayerId = playerId;
	}

	@Override
	public String toString() {
		return this.playerName + ", " + this.club + ", " + this.sinceString;

	}

	@Override
	public JSONObject toPlayerDataJSON() {
		JSONObject result = new JSONObject();
		result.put("playerName", playerName);
		result.put("playerId", ligainsiderPlayerId);
		result.put("playerLink", playerLink);
		return result;
	}

	public Instant getStatusChangeTimestamp() {
		return statusChangeTimestamp;
	}

	public void setStatusChangeString(String statusChangeTimestamp) {
		this.statusChangeString = statusChangeTimestamp;
		this.statusChangeTimestamp = parse(this.statusChangeString);
	}

	public String getQuelle() {
		return quelle;
	}

	public void setQuelle(String quelle) {
		this.quelle = quelle;
	}

	public String getComunioPlayerId() {
		return comunioPlayerId;
	}

	public void setComunioPlayerId(String comunioPlayerId) {
		this.comunioPlayerId = comunioPlayerId;
	}

}
