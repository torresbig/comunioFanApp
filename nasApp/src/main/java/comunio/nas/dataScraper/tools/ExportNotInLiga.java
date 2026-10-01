package comunio.nas.dataScraper.tools;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.InjuryAndBlockedData;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.player.PlayerTools;

/**
 * Dienstklasse für das Auffinden, Exportieren und Entfernen von Spielern, die
 * nicht mehr in der Liga sind.
 */
public class ExportNotInLiga {

	private static final Logger LOGGER = LogManager.getLogger(ExportNotInLiga.class);

	/**
	 * Findet alle Spieler mit Status "NICHT_IN_LIGA" und "verein" gleich "0".
	 *
	 * @param playerDB Die JSONArray mit allen Spieler-Objekten.
	 * @return Map mit Spieler-IDs als Schlüssel und JSONObjects als Werte.
	 */
	private static Map<String, JSONObject> findPlayerNichtInLiga(JSONArray playerDB, JSONObject injuryDB) {
		Map<String, JSONObject> playerResultList = new HashMap<>();
		if (injuryDB != null) {
			int found = 0;
			for (int i = 0; i < playerDB.length(); i++) {
				JSONObject playerObj = playerDB.optJSONObject(i);
				if (playerObj == null) {
					LOGGER.warning("Ungültiges Spieler-Objekt an Index " + i);
					continue;
				}

				JSONObject data = playerObj.optJSONObject("data");
				if (data == null) {
					LOGGER.warning("Kein 'data' Feld für Spieler an Index " + i + ": " + playerObj);
					continue;
				}

				String id = playerObj.optString("id");
				if (id == null || id.isEmpty()) {
					continue;
				}

				JSONObject injuryInfo = injuryDB.optJSONObject(id, new JSONObject());

				String verein = data.optString("verein");

				String statusStr = injuryInfo.optString("status");
				
				boolean hasNotInLigaClubId = "0".equals(verein) || "61".equals(verein);

				if ("NICHT_IN_LIGA".equals(statusStr) && hasNotInLigaClubId) {
					setFlagsForNotinLiga(playerObj, injuryInfo, injuryDB);
					playerResultList.put(id, playerObj);
					found++;
				} else if ("NICHT_IN_LIGA".equals(statusStr) && !hasNotInLigaClubId) {
					LOGGER.info("-NICHT IN LIGA-, aber Verein ist nicht 0: " + playerObj.optString("name") + " (ID: " + id + ")");
				} else if (!"NICHT_IN_LIGA".equals(statusStr) && hasNotInLigaClubId) {
					setFlagsForNotinLiga(playerObj, injuryInfo, injuryDB);
					playerResultList.put(id, playerObj);
					found++;
//					ComunioDataUpdater.errorDb.addError(new Error(ErrorType.NICHTINLIGA,"NICHT -NICHT IN LIGA-, aber Verein ist 0 oder 61: " + playerObj.optString("name") + " (ID: " + id + ")"));
					LOGGER.info("NICHT -NICHT IN LIGA-, aber Verein ist 0 oder 61: " + playerObj.optString("name") + " (ID: " + id + ")");
				}
			}
			LOGGER.info("Es wurden " + found + " Spieler mit NICHT_IN_LIGA gefunden!");
		}
		
		return playerResultList;
	}
	
	private static void setFlagsForNotinLiga(JSONObject playerObj, JSONObject injuryInfo, JSONObject injuryDb ) {
		if(playerObj == null) {
			return;
		}
		JSONObject data = playerObj.optJSONObject("data");
		if(data == null || data.isEmpty()) {
			return;
		}
		data.put("verein", "0");
		data.put("retired", true);
		String playerId = playerObj.getString("id"); 
		String playerName = playerObj.getString("name"); 
		
		if(injuryInfo == null || injuryInfo.isEmpty()) {
			LOGGER.info("Keine Verletzungsinfo für Spieler " + playerName + " (ID: " + playerId + ")");
			return;
		}
		
		InjuryAndBlockedData ibd = InjuryAndBlockedData.fromJSON(injuryInfo);
		ibd = InjuryAndBlockedData.setAsNotInLiga(playerId, "ExportNotInLiga", ibd);
		injuryDb.put(playerId, ibd);
		LOGGER.info("Spieler-Flags für NotInLiga gesetzt " + playerName + " (ID: " + playerId + ")");
	}

	/**
	 * Entfernt alle Spieler mit IDs aus notInLigaIds aus dem playerDB-Array.
	 *
	 * @param notInLigaIds Die Menge der zu entfernenden Spieler-IDs.
	 * @param playerDB     Das JSONArray mit allen Spieler-Objekten.
	 */
	private static void removeNotInLigaFromPlayerDB(Set<String> notInLigaIds, JSONArray playerDB) {
		for (String key : notInLigaIds) {
			for (int i = playerDB.length() - 1; i >= 0; i--) { // Von hinten nach vorne!
				JSONObject jsonObj = playerDB.optJSONObject(i);
				if (jsonObj != null && key.equals(jsonObj.optString("id"))) {
					playerDB.remove(i);
					LOGGER.info("Spieler mit ID " + key + " aus playerDB entfernt.");
				}
			}
		}
	}

	/**
	 * Exportiert und entfernt alle Spieler mit Status NICHT_IN_LIGA aus der DB.
	 *
	 * @param playerDBObj    Das JSONObject, das das playerDB Feld enthält.
	 * @param notInligaDBObj Das Ziel-JSONObject für exportierte Spieler.
	 */
	public static void exportAndRemoveNotInLiga(JSONObject playerDBObj, JSONObject notInligaDBObj, LastUpdates lastUpdates, JSONObject injuryDB) {
		LOGGER.info("Datenbank für NICHT_IN_LIGA-Spieler durchsucht.");
		JSONArray playerDB = playerDBObj.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.warning("Keine PlayerDB vorhanden! Abbruch.");
			return;
		}
		System.out.println();
		Map<String, JSONObject> notInLigaPlayerMap = findPlayerNichtInLiga(playerDB, injuryDB);
		if (notInLigaPlayerMap.isEmpty()) {
			LOGGER.info("Keine NICHT_IN_LIGA-Spieler gefunden.");
			return;
		}

		if (notInligaDBObj == null) {
			notInligaDBObj = new JSONObject();
		}
		notInligaDBObj.put("lastUpdate", new ComunioDate()); // oder Custom Updater
		// TODO: DATUM RAUS
		lastUpdates.setNotInLigaDb(Instant.now());
		removeNotInLigaFromPlayerDB(notInLigaPlayerMap.keySet(), playerDB);
		addToNotInLigaDB(notInligaDBObj, notInLigaPlayerMap);
		LOGGER.info("NICHT_IN_LIGA-Spieler exportiert und entfernt.");
	}

	/**
	 * Fügt die gefundenen NICHT_IN_LIGA-Spieler in die separate Export-DB ein.
	 *
	 * @param notInLigaDBObj     Ziel-DB.
	 * @param notInLigaPlayerMap Map der zu exportierenden Spielerobjekte.
	 */
	private static void addToNotInLigaDB(JSONObject notInLigaDBObj, Map<String, JSONObject> notInLigaPlayerMap) {
		JSONObject db = notInLigaDBObj.optJSONObject("db");
		if (db == null) {
			db = new JSONObject();
		}
		for (Map.Entry<String, JSONObject> entry : notInLigaPlayerMap.entrySet()) {
			PlayerTools.deletePlayerDbAttributeForNewSeason(entry.getValue());
			LOGGER.info("Spieler mit ID " + entry.getKey() + " vor Export bereinigt: " + entry.getValue());
			db.put(entry.getKey(), entry.getValue());
			LOGGER.info("Spieler mit ID " + entry.getKey() + " in NotInLigaDB exportiert.");
		}
		notInLigaDBObj.put("db", db);
	}
}
