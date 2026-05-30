package comunio.nas.util.player;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;

public class PlayerHelper {

	private static final Logger LOGGER = LogManager.getLogger(PlayerHelper.class);

	/**
	 * Sucht einen Spieler anhand der ID in einem JSONArray.
	 * 
	 * @param playerDB    JSONArray mit Spielern
	 * @param playerId    Die zu suchende Spieler-ID
	 * @param notInLigaDB die nicht in Liga Spieler DB (kann null sein!)
	 * @return JSONObject des Spielers oder null, falls nicht gefunden
	 */
	public static JSONObject findPlayerByComunioId(JSONArray playerDB, String playerId, JSONObject notInLigaDBObj) {
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject playerObj = playerDB.getJSONObject(i);
			String dbId = convertIdToString(playerObj.get("id"));
			if (dbId.equals(playerId)) {
				return playerObj;
			}
		}

		if (notInLigaDBObj == null) {
			return null;
		}

		JSONObject db = notInLigaDBObj.optJSONObject("db");
		if (db == null) {
			return null;
		}
		JSONObject player = db.optJSONObject(playerId);
		if (player == null) {
			return null;
		}
		LOGGER.info("Player aus der NOT_IN_LIGA_DB geholt!");
		// Spieler gefunden, jetzt aus der DB entfernen, damit er nicht nochmal verwendet wird
		db.remove(playerId); // Einmalige Nutzung, danach entfernen
		
		return player;
	}

	/**
	 * Konvertiert ID-Objekte zu String, unabhängig vom Originaltyp.
	 * 
	 * @param idObj ID-Objekt (Integer, String, etc.)
	 * @return String-Repräsentation der ID
	 */
	public static String convertIdToString(Object idObj) {
		if (idObj instanceof Integer) {
			return String.valueOf(idObj);
		} else if (idObj instanceof String) {
			return (String) idObj;
		} else {
			return idObj.toString();
		}
	}

	/**
	 * Durchsucht das JSONArray nach dem angegebenen Pfad (z.B.
	 * "data.attribute.ligainsiderRanking.lastUpdate") und gibt den ersten
	 * gefundenen Wert als String zurück. Gibt null zurück, wenn der Pfad nicht
	 * gefunden wurde.
	 */
	public static ComunioDate getFirstDateByPath(JSONArray jsonArray, String path) {
		if (jsonArray == null || path == null || path.isEmpty()) {
			return null;
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject eintrag = jsonArray.optJSONObject(i);
			ComunioDate result = getDateByPath(eintrag, path);
			if (result != null) { // Wenn ein gültiger Wert gefunden wurde, direkt zurückgeben
				return result;
			}
		}

		return null; // Kein gültiger Eintrag gefunden
	}

	/**
	 * Durchsucht das JSONArray nach dem angegebenen Pfad (z.B.
	 * "data.attribute.ligainsiderRanking.lastUpdate") und gibt den neusten
	 * gefundenen Wert als String zurück. Gibt null zurück, wenn der Pfad nicht
	 * gefunden wurde.
	 */
	public static ComunioDate getNewestDateByPath(JSONArray jsonArray, String path) {
		if (jsonArray == null || path == null || path.isEmpty()) {
			return null;
		}

		ComunioDate newestDate = null;
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject eintrag = jsonArray.optJSONObject(i);
			ComunioDate result = getDateByPath(eintrag, path);
			if (result != null) { // Wenn ein gültiger Wert gefunden wurde, direkt zurückgeben
				if (newestDate == null) {
					newestDate = result;
				} else if (result.after(newestDate)) {
					newestDate = result;
				}
			}
		}

		return newestDate; // Kein gültiger Eintrag gefunden
	}

	public static ComunioDate getDateByPath(JSONObject jsonObject, String path) {
		if (jsonObject == null || path == null || path.isEmpty()) {
			return null;
		}

		String[] keys = path.split("\\.");

		Object current = jsonObject;
		for (String key : keys) {
			if (!(current instanceof JSONObject)) {
				return null;
			}
			current = ((JSONObject) current).opt(key);
			if (current == null) {
				return null;
			}
		}

		try {
			return new ComunioDate(current.toString());
		} catch (Exception e) {
			// z.B. ParseException oder ähnliches, falls ComunioDate einen wirft
			return null;
		}
	}

	public static JSONObject createNewEmptyPlayerJSON(String playerID, String playerName) {
		JSONObject player = new JSONObject();
		player.put("id", playerID);
		player.put("name", playerName);

		// Data-Objekt mit Standardwerten
		JSONObject data = new JSONObject();
		data.put("position", "UNBEKANNT");
		data.put("verein", "UNBEKANNT");
		data.put("punkte", 0);
		data.put("spieltagspunkte", new JSONArray());
		data.put("lastPoints", 0);

		JSONObject spielerdaten = new JSONObject();
		spielerdaten.put("nebenpositionen", new JSONArray());
		spielerdaten.put("nationalspieler", "");
		spielerdaten.put("trikotNummer", 0);
		spielerdaten.put("nationalitaet", "");
		spielerdaten.put("laenderspielTore", 0);
		spielerdaten.put("hauptposition", "");
		spielerdaten.put("fuss", "");
		spielerdaten.put("groesse", "");
		spielerdaten.put("geburtstag", "");
		spielerdaten.put("laenderspiele", 0);
		spielerdaten.put("lastUpdate", JSONObject.NULL);
		data.put("spielerDaten", spielerdaten);

		data.put("isNotName", new JSONArray());
		data.put("punktekorrektur", new JSONArray());
		data.put("wert", 0);
		data.put("realWert", 0);
		data.put("withinSquad", false);
		data.put("onMarket", false);
		data.put("stats", new JSONObject().put("notenDurchschnitt", "0").put("gelbekarten", 0).put("totalPenalties", 0).put("ratedGames", 0).put("lastUpdate", new ComunioDate().toString()).put("playedGames", 0).put("totalGoals", 0).put("punkteDurchschnitt", "0").put("gelbrotekarten", 0).put("rotekarten", 0).put("manOfTheMatchAmount", 0));
		data.put("attribute", new JSONArray());
		data.put("possibleNames", new JSONArray());
		data.put("comunioStatus", new JSONObject().put("grund", "unbekannt").put("lastUpdate", new ComunioDate().toString()).put("seit", "unbekannt").put("bis", "unbekannt").put("details", "").put("historie", new JSONArray()).put("status", "AKTIV"));
		data.put("status", new JSONObject().put("grund", "unbekannt").put("lastUpdate", new ComunioDate().toString()).put("seit", "unbekannt").put("details", "").put("historie", new JSONArray()).put("status", "AKTIV"));

		player.put("data", data);
		return player;
	}

	/**
	 * Prüft, ob zwei Spielernamen als gleich oder ähnlich betrachtet werden können,
	 * wobei auch Initialen anstelle des ausgeschriebenen Vornamens erlaubt sind.
	 *
	 * Beispiel:
	 * <ul>
	 * <li>"Thomas Sborn" und "T. Sborn" werden als Match erkannt.</li>
	 * <li>Kleine Tippfehler werden ebenfalls toleriert.</li>
	 * </ul>
	 *
	 * @param name1 Erster Name (z. B. "Thomas Sborn")
	 * @param name2 Zweiter Name (z. B. "T. Sborn")
	 * @return {@code true} wenn die Namen als passend gelten, sonst {@code false}
	 */
	public static boolean namesMatchWithInitial(String name1, String name2) {
		if (name1 == null || name2 == null)
			return false;

		String n1 = normalizeSimple(name1);
		String n2 = normalizeSimple(name2);

		if (n1.equals(n2))
			return true;

		int lev = LevenshteinDistance.getDefaultInstance().apply(n1, n2);
		double similarity = 1.0 - (double) lev / Math.max(n1.length(), n2.length());

		return similarity > 0.9;
	}

	/**
	 * Hilfsmethode, um Namen in das Format "Initial Nachname" zu bringen. Entfernt
	 * Sonderzeichen und gibt Kleinbuchstaben zurück.
	 *
	 * @param name Name, z. B. "Thomas Sborn"
	 * @return Normalisierte Zeichenkette z. B. "t sborn"
	 */
	private static String normalizeSimple(String name) {
		if (name == null || name.isBlank())
			return "";
		String[] parts = name.trim().split("\\s+");
		if (parts.length < 2) {
			return name.toLowerCase().replaceAll("[^a-z]", "");
		}
		String firstInitial = parts[0].substring(0, 1).toLowerCase();
		String lastName = parts[parts.length - 1].toLowerCase().replaceAll("[^a-z]", "");
		return firstInitial + " " + lastName;
	}
	
	

	/**
	 * Ermittelt den Besitzstand eines Spielers zu einem bestimmten Zeitpunkt
	 * (normalerweise: Spieltag-Start).
	 *
	 * @param playerId     Die playerId des Spielers.
	 * @param matchdayInfo Das MatchdayInfo-Objekt für den Spieltag.
	 * @param newsArray    JSONArray mit allen News- und Transfer-Events (sortiert
	 *                     nach Datum).
	 * @param playerToUser Aktuelle Map<PlayerID, UserID>.
	 * @return UserID des Besitzers am entsprechenden Spieltag, oder null wenn
	 *         unbekannt.
	 */
	public static String getOwnerAtMatchday(String playerId, MatchdayInfo matchdayInfo, JSONArray newsArray, Map<String, String> playerToUser) {
		// Referenz-Zeitpunkt (idealerweise: frühester oder spätester Kickoff des
		// Spieltags)
		ZonedDateTime matchdayStart = matchdayInfo.getEarliestKickoff();
		if (matchdayStart == null) {
			matchdayStart = matchdayInfo.getLatestKickoff();
		}
		if (matchdayStart == null) {
			// Fallback: aktuelles Datum
			matchdayStart = ZonedDateTime.now();
		}

		// DateTime Formatter, falls News-Datum als String (z.B. "17.06.2025")
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		String currentOwnerId = null;

		// Chronologisch alle Transfers zum Spieler bis zum Matchday-Datum durchgehen
		for (int i = 0; i < newsArray.length(); i++) {
			JSONObject newsDay = newsArray.getJSONObject(i);
			if (!newsDay.has("news"))
				continue;
			JSONArray items = newsDay.getJSONArray("news");
			for (int j = 0; j < items.length(); j++) {
				JSONObject event = items.getJSONObject(j);

				// Nur Transfers relevant!
				if (event.has("art") && "TRANSFER".equals(event.getString("art"))) {
					// Ist es der richtige Spieler?
					if (playerId.equals(event.optString("playerId"))) {
						// Datum des Transfers (Vergleichspunkt)
						String newsDateStr = event.optString("date");
						ZonedDateTime newsDate;
						try {
							// Format "17.06.2025" verarbeiten
							LocalDateTime localDate = LocalDateTime.parse(newsDateStr, formatter);
							// Default: Berlin-Zeit
							newsDate = localDate.atZone(matchdayStart.getZone());
						} catch (Exception e) {
							// Fallback: ignoriere fehlerhafte Datumsangaben
							continue;
						}

						if (!newsDate.isAfter(matchdayStart)) {
							// Besitzerwechsel vor/nach dem Spieltag -> setze neuen Besitzer
							JSONObject transferDetails;
							try {
								transferDetails = new JSONObject(event.optString("text"));
							} catch (Exception e) {
								continue; // Fehlerhaftes Transfer-Details-Format
							}
							currentOwnerId = transferDetails.optString("buyerId", null);
						}
					}
				}
			}
		}

		// Wenn Transfers gefunden wurden, wurde Besitzer angepasst
		if (currentOwnerId != null) {
			return currentOwnerId;
		}

		// Falls kein Transfer, den aktuellen Besitzstand nutzen
		return playerToUser.getOrDefault(playerId, null);
	}

}
