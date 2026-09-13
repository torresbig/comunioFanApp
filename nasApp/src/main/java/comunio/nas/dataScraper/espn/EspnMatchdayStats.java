package comunio.nas.dataScraper.espn;

import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;

/**
 * Datenmodell für die ESPN-Spieltag-Statistiken eines Spielers.
 * <p>
 * Die Struktur ist kompatibel zur bestehenden PointsDB:
 * <pre>
 * {
 *   "playerId": "12345",
 *   "playerName": "Frederik Ronnow",
 *   "clubId": "598",
 *   "clubName": "1. FC Union Berlin",
 *   "espnId": "165424",
 *   "espnName": "Frederik Ronnow",
 *   "matchdayStats": [
 *     { "key": 1, "value": 0, "appearances": 1, "saves": 6, ... },
 *     { "key": 2, "value": 0, ... }
 *   ]
 * }
 * </pre>
 * <b>Hinweis:</b> Die echten Spieltagspunkte ({@code value}) kommen
 * ausschließlich von Comunio – ESPN liefert nur Saison-Statistiken. Diese
 * Klasse dient dazu, die ESPN-Stats pro Spieltag als Snapshot zu speichern,
 * damit sie später ausgewertet werden können.
 */
public class EspnMatchdayStats {

	private static final Logger LOGGER = LogManager.getLogger(EspnMatchdayStats.class);

	/** Comunio-Spieler-ID (Key in der PointsDB). */
	private String playerId;

	/** Spielername (aus der Comunio-DB). */
	private String playerName;

	/** Comunio-Vereins-ID. */
	private String clubId;

	/** Vereinsname (Comunio). */
	private String clubName;

	/** ESPN-Athlete-ID. */
	private String espnId;

	/** ESPN-Spielername. */
	private String espnName;

	/** Liste der Spieltag-Einträge (kompatibel zu PointsDB-Einträgen). */
	private JSONArray matchdayStats;

	public EspnMatchdayStats() {
		this.playerId = "";
		this.playerName = "";
		this.clubId = "";
		this.clubName = "";
		this.espnId = "";
		this.espnName = "";
		this.matchdayStats = new JSONArray();
	}

	// =========================================================================
	// JSON-Serialisierung
	// =========================================================================

	/**
	 * Erzeugt ein {@link EspnMatchdayStats}-Objekt aus einem JSONObject.
	 *
	 * @param json Das JSONObject (Format siehe {@link #toJSON()})
	 * @return Das geparste Objekt (leeres Objekt bei null)
	 */
	public static EspnMatchdayStats fromJSON(JSONObject json) {
		EspnMatchdayStats stats = new EspnMatchdayStats();
		if (json == null) {
			return stats;
		}
		stats.playerId = json.optString("playerId", "");
		stats.playerName = json.optString("playerName", "");
		stats.clubId = json.optString("clubId", "");
		stats.clubName = json.optString("clubName", "");
		stats.espnId = json.optString("espnId", "");
		stats.espnName = json.optString("espnName", "");
		JSONArray arr = json.optJSONArray("matchdayStats");
		if (arr != null) {
			stats.matchdayStats = arr;
		}
		return stats;
	}

	/**
	 * Serialisiert dieses Objekt als JSONObject (PointsDB-kompatibel).
	 *
	 * @return Das JSONObject
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (playerId != null && !playerId.isEmpty())
			json.put("playerId", playerId);
		if (playerName != null && !playerName.isEmpty())
			json.put("playerName", playerName);
		if (clubId != null && !clubId.isEmpty())
			json.put("clubId", clubId);
		if (clubName != null && !clubName.isEmpty())
			json.put("clubName", clubName);
		if (espnId != null && !espnId.isEmpty())
			json.put("espnId", espnId);
		if (espnName != null && !espnName.isEmpty())
			json.put("espnName", espnName);
		if (matchdayStats != null)
			json.put("matchdayStats", matchdayStats);
		return json;
	}

	// =========================================================================
	// Hilfsmethoden für matchdayStats
	// =========================================================================

	/**
	 * Fügt einen Spieltag-Eintrag hinzu (oder aktualisiert einen bestehenden mit
	 * demselben Key).
	 *
	 * @param key   Spieltag-Nummer
	 * @param value Punkte (von Comunio, kann 0 sein)
	 * @param stats Zusätzliche ESPN-Statistiken als JSONObject (optional)
	 */
	public void addOrUpdateMatchday(int key, int value, JSONObject stats) {
		// Bestehenden Eintrag suchen
		for (int i = 0; i < matchdayStats.length(); i++) {
			JSONObject entry = matchdayStats.getJSONObject(i);
			if (entry.optInt("key", -1) == key) {
				entry.put("value", value);
				if (stats != null) {
					mergeStats(entry, stats);
				}
				return;
			}
		}
		// Neuen Eintrag anlegen
		JSONObject entry = new JSONObject();
		entry.put("key", key);
		entry.put("value", value);
		if (stats != null) {
			mergeStats(entry, stats);
		}
		matchdayStats.put(entry);
	}

	/**
	 * Fügt die ESPN-Statistik-Felder in einen Spieltag-Eintrag ein.
	 */
	private static void mergeStats(JSONObject entry, JSONObject stats) {
		Iterator<String> keys = stats.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			entry.put(key, stats.get(key));
		}
	}

	/**
	 * Liefert den Punktewert für einen bestimmten Spieltag (oder -1).
	 */
	public int getValueForMatchday(int key) {
		for (int i = 0; i < matchdayStats.length(); i++) {
			JSONObject entry = matchdayStats.getJSONObject(i);
			if (entry.optInt("key", -1) == key) {
				return entry.optInt("value", -1);
			}
		}
		return -1;
	}

	// =========================================================================
	// Getter/Setter
	// =========================================================================

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getClubId() {
		return clubId;
	}

	public void setClubId(String clubId) {
		this.clubId = clubId;
	}

	public String getClubName() {
		return clubName;
	}

	public void setClubName(String clubName) {
		this.clubName = clubName;
	}

	public String getEspnId() {
		return espnId;
	}

	public void setEspnId(String espnId) {
		this.espnId = espnId;
	}

	public String getEspnName() {
		return espnName;
	}

	public void setEspnName(String espnName) {
		this.espnName = espnName;
	}

	public JSONArray getMatchdayStats() {
		return matchdayStats;
	}

	public void setMatchdayStats(JSONArray matchdayStats) {
		this.matchdayStats = matchdayStats;
	}

	@Override
	public String toString() {
		return "EspnMatchdayStats{playerId=" + playerId + ", playerName=" + playerName + ", matchdayStats=" + matchdayStats.length() + "}";
	}
}