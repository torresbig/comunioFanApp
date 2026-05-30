package comunio.nas.dataScraper.tools;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;

public class PlayerpointsToPlayerObject {

	private static final Logger LOGGER = LogManager.getLogger(PlayerpointsToPlayerObject.class);
	
	/**
	 * Überträgt die Punkte aus dem pointsDB-Objekt in die entsprechenden Spielerobjekte im playerDBObject.
	 * Für jeden Spieler im playerDBObject wird geprüft, ob es einen Eintrag im pointsDB gibt. Wenn ja, werden die Punkte als "spieltagspunkte" in den Daten des Spielers hinzugefügt.
	 * Es wird auch eine Überprüfung auf doppelte Punkteentrys durchgeführt, bei denen eine interpoliert wurde, um Datenlücken zu füllen. In diesem Fall wird die interpolierte Entry entfernt und der Fixes-Counter erhöht.
	 * Am Ende werden Informationen über die Anzahl der übertragenen Punkte, die Anzahl der Spieler mit Punkten und die Anzahl der Spieler im playerDBObject geloggt.
	 * @param pointsDB JSONObject, das die Punkte für jeden Spieler enthält (Schlüssel: Spieler-ID, Wert: JSONArray von Punkteentrys)
	 * @param playerDBObject JSONObject, das die Spielerobjekte enthält, in die die Punkte übertragen werden sollen
	 */

	public static void putPointsToPlayerObject(JSONObject pointsDB, JSONObject playerDBObject) {
		fixes = 0; // Reset fixes counter before processing
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null || playerDB.length() == 0) {
			LOGGER.warning("PlayerDB ist leer oder null im playerDBObject.");
			return; // Leeres Array zurückgeben, wenn keine Spieler vorhanden sind
		}
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			String playerID = player.getString("id");
			JSONObject playerData = player.optJSONObject("data");
			if (playerData == null) {
				continue; // Überspringe, wenn keine Spieldaten vorhanden sind
			}

			JSONArray pointsEntrys = pointsDB.optJSONArray(playerID);
			if (pointsEntrys != null && pointsEntrys.length() > 0) {
				//TODO: ggf. den fix wieder raus, wenn er keine fehler mehr macht!
				JSONArray fixedPointsEntrys = fixDoubleEntries(pointsEntrys);
				playerData.put("spieltagspunkte", fixedPointsEntrys);
				LOGGER.fine("Punkte für Spieler " + playerID + " erfolgreich übertragen. Anzahl Punkteentrys: " + fixedPointsEntrys.length() + " (vor Fix: " + pointsEntrys.length() + ")");
				
			}
		}
		LOGGER.info("Punkte erfolgreich in Spielerobjekte übertragen. Anzahl Fixes (interpoliert-Fehler): " + fixes);
		LOGGER.info("Anzahl Spieler mit Punkten: " + pointsDB.length());
		LOGGER.info("Anzahl Spieler in playerDBObject: " + playerDB.length());
		
	}
	
	private static int fixes = 0;
	
	/**
	 * Entfernt doppelte Punkteentrys, bei denen eine interpoliert wurde, weil eine Datenlücke erkannt wurde.
	 * Wenn eine solche interpolierte Entry gefunden wird, wird überprüft, ob es eine nicht-interpolierte Entry mit demselben Key gibt.
	 * Wenn ja, wird die interpolierte Entry entfernt und der Fixes-Counter erhöht.
	 * @param pointsEntrys JSONArray der Punkteentrys eines Spielers
	 */
	
	private static JSONArray fixDoubleEntries(JSONArray pointsEntrys) {
		JSONArray uniqueEntries = new JSONArray();
		for (int i = 0; i < pointsEntrys.length(); i++) {
			JSONObject entry = pointsEntrys.getJSONObject(i);
			if(entry.has("info") && entry.optString("info", "").contains("Interpoliert")){
				boolean isDuplicate = false;
				for (int j = 0; j < pointsEntrys.length(); j++) {
					JSONObject entryJ = pointsEntrys.getJSONObject(j);
					if(entryJ.getInt("key") == entry.getInt("key") && !entryJ.has("info")){
						isDuplicate = true;
						fixes++;
						LOGGER.info("Doppelte Punkteentry mit Interpolation gefunden. Entferne interpolierte Entry: " + entry);
						break;
					}
				}
				if(!isDuplicate) {
					uniqueEntries.put(entry);
				}
			} else {
				uniqueEntries.put(entry);
			}
		}
		return uniqueEntries;
	}

	public static JSONObject getPointsArrayFromAllPlayer(JSONObject pointsDB, JSONObject playerDBObject) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");

		if (playerDB == null || playerDB.length() == 0) {
			LOGGER.warning("PlayerDB ist leer oder null im playerDBObject.");
			return new JSONObject(); // Leeres Array zurückgeben, wenn keine Spieler vorhanden sind
		}

		for (int j = 0; j < playerDB.length(); j++) {
			JSONObject playerEntry = playerDB.getJSONObject(j);
			JSONObject playerData = playerEntry.optJSONObject("data");
			if (playerData == null) {
				continue; // Überspringe, wenn keine Spieldaten vorhanden sind
			}
			String playerID = playerEntry.getString("id");
			if (playerData.has("spieltagspunkte")) {
				JSONArray spieltagsPunkte = playerData.getJSONArray("spieltagspunkte");
				pointsDB.put(playerID, spieltagsPunkte);
			}

		}
		deleteOldPlayerPoints(playerDBObject);
		return pointsDB;

	}
	
	private static void deleteOldPlayerPoints(JSONObject playerDBObject) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null || playerDB.length() == 0) {
			LOGGER.warning("PlayerDB ist leer oder null im playerDBObject.");
			return; // Leeres Array zurückgeben, wenn keine Spieler vorhanden sind
		}

		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject playerData = player.optJSONObject("data");
			if (playerData == null) {
				continue; // Überspringe, wenn keine Spieldaten vorhanden sind
			}

			if (playerData.has("spieltagspunkte")) {
				playerData.remove("spieltagspunkte");
			}
		}
	}

}
