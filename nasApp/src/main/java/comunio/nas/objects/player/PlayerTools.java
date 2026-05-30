package comunio.nas.objects.player;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerTools {
	/**
	 * Entfernt spielerspezifische Attribute aus dem {@code data}-Objekt jedes
	 * Spielers in der übergebenen Spielerdatenbank. Diese Attribute (z. B. Punkte,
	 * Statistiken, Elf-des-Spieltags-Daten) werden für den Start einer neuen Saison
	 * zurückgesetzt.
	 *
	 * @param playerDBObject das JSON-Objekt, das ein Array unter dem Schlüssel
	 *                       "playerDB" enthält
	 */
	public static void deletePlayerDbAttributeForNewSeason(JSONObject playerDBObject) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			return;
		}
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");
			if (data != null) {
				data.remove("elfDesSpieltages");
				data.remove("punktekorrektur");
				data.remove("fussballdatenDe");
				data.remove("lastPoints");
				data.remove("lastSeasonPoints");
				data.remove("punkte");
				data.remove("stats");
				data.remove("attribute");
				if (data.has("spieltagspunkte")) {
					data.remove("spieltagspunkte");
				}
			}
		}
	}


}
