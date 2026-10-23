package comunio.nas.util.player;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;

public class PlayerHelper {

	private static final Logger LOGGER = LogManager.getLogger(PlayerMatcher.class);

	
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
		data.put("verein", "0");
		data.put("punkte", 0);
		data.put("lastPoints", 0);
		data.put("retired", false);

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
		data.put("spielerDaten", spielerdaten);

		data.put("isNotName", new JSONArray());
		data.put("punktekorrektur", new JSONArray());
		data.put("wert", 0);
		data.put("realWert", 0);
		data.put("withinSquad", false);
		data.put("onMarket", false);
		data.put("stats", new JSONObject().put("notenDurchschnitt", "0").put("gelbekarten", 0).put("totalPenalties", 0).put("ratedGames", 0).put("lastUpdate", new ComunioDate().toString()).put("playedGames", 0).put("tore", 0).put("punkteDurchschnitt", "0").put("gelbrotekarten", 0).put("rotekarten", 0).put("manOfTheMatchAmount", 0));
		data.put("attribute", new JSONObject());
		data.put("possibleNames", new JSONArray());
		player.put("data", data);
		return player;
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

	public static boolean deleteAttributeFromPlayer(JSONObject playerObj, String attribut) {
		if (attribut != null && !attribut.isBlank() && playerObj != null) {
			JSONObject pDataObj = playerObj.has("data") ? playerObj.getJSONObject("data") : new JSONObject();
			JSONObject attribute = new JSONObject();
			if(pDataObj.has("attribute")) {
				 if(pDataObj.get("attribute") instanceof JSONObject ){
					  attribute =pDataObj.optJSONObject("attribute", new JSONObject());
				 } else {
					 if(pDataObj.getJSONArray("attribute").isEmpty()){
						 pDataObj.remove("attribute");
					 } else {
						 ComunioDataUpdater.errorDb.addError(new Error(ErrorType.DELETE_ATTRIBUT_ARRAY_NOT_EMPTY, "beim löschen des unnötigen Array für attribute (soll ein JSONObject sein, wurde festgestellt, dass es nciht leer ist: " + pDataObj.getJSONArray("attribute").toString()));
					 }
				 }
			}
			
			if (attribute != null && !attribute.isEmpty()) {
				if (attribute.has(attribut)) {
					attribute.remove(attribut);
					
					LOGGER.info(attribut + " << Attribut wurde vom Spieler " + playerObj.getString("name") + " gelöscht");
					return true;
				}
			}
		} else {
			if (playerObj == null) {
				LOGGER.info(attribut + " << Attribut wurde nicht gelöscht! PLAYER-Objekt ist null");
			} else if (attribut == null || attribut.isBlank()) {
				LOGGER.info("kein Attribut als input!");
			} else {
				LOGGER.info(attribut + " << Attribut vom Spieler " + playerObj.getString("name") + " NICHT gelöscht! Irgendein Fehler. sollte nicht vorkommen");
			}

		}
		return false;
	}
	
	public static void deleteAttributFromAllPlayers(String attribut, JSONArray playerDB) {
	    long success = StreamSupport.stream(playerDB.spliterator(), false)
	            .map(JSONObject.class::cast)
	            .filter(pObj -> deleteAttributeFromPlayer(pObj, attribut))
	            .count();

	    LOGGER.info(attribut + " << Attribut von " + success + " Spielern gelöscht!");
	}
}