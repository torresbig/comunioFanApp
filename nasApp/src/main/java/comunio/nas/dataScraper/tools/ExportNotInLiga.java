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
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.player.PlayerTools;

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

				JSONObject injuryInfo = injuryDB.optJSONObject(id);

				String verein = data.optString("verein");
				if (injuryInfo == null) {
					continue;
				}
				String statusStr = injuryInfo.optString("status");

				if ("NICHT_IN_LIGA".equals(statusStr) && "0".equals(verein)) {
					playerResultList.put(id, playerObj);
					found++;
				} else if ("NICHT_IN_LIGA".equals(statusStr) && !"0".equals(verein)) {
					LOGGER.info("-NICHT IN LIGA-, aber Verein ist nicht 0: " + playerObj.optString("name") + " (ID: " + id + ")");
				} else if (!"NICHT_IN_LIGA".equals(statusStr) && "0".equals(verein)) {
					LOGGER.info("NICHT -NICHT IN LIGA-, aber Verein ist 0: " + playerObj.optString("name") + " (ID: " + id + ")");
				}
			}
			LOGGER.info("Es wurden " + found + " Spieler mit NICHT_IN_LIGA gefunden!");
		}
		
		return playerResultList;
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
