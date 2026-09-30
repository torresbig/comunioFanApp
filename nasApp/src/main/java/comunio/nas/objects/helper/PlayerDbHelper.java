package comunio.nas.objects.helper;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.PlayerUpdater;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.NewsManager;

public class PlayerDbHelper {

	private static final Logger LOGGER = LogManager.getLogger(PlayerDbHelper.class);

	public static void checkPlayersSetWithDbAndUpdateSingel(JSONObject playerDbObject, Set<String> playerSet, Map<String, String> playerToUserMap, NewsManager newsManager, LastUpdates lastUpdates, JSONArray marketValueDB) {

		if (playerDbObject == null || playerSet == null || playerSet.isEmpty()) {
			return;
		}

		JSONArray playerDB = playerDbObject.optJSONArray("playerDB");
		if (playerDB.isEmpty()) {
			return;
		}

		StringBuilder log = new StringBuilder();
		int countOk = 0;
		int countNotOk = 0;
		int countRetired = 0 ;
		
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			String playerId = player.optString("id");
			if (playerSet.contains(playerId)) {
				continue;
			}

			String name = player.getString("name");
			log.append("Spieler wurde nicht geupdatet! " + name + " ID: " + playerId);
			PlayerUpdater.loadPlayerData(player, playerToUserMap, newsManager, playerDbObject, ComunioDataUpdater.user, lastUpdates, marketValueDB);
			JSONObject data = player.getJSONObject("data");
			String verein = data.optString("verein");
			log.append("\n");
			if ("61".equals(verein) || "0".equals(verein)) {
				data.put("verein", "0");
				log.append("Spieler " + name + " nach Clubupdate und spielerupdate nicht mehr in der Liga!");
				ComunioDataUpdater.errorDb.addError(new Error(ErrorType.NICHTINLIGA, log.toString()));
				countNotOk++;
			} else {
				if (data.getBoolean("retired")) {
					log.append("Spieler " + name + " hat Status RETIRED!! VereinsId: " + verein);
					log.append("\n");
					log.append("Verein wird auf 0 gesetzt!");
					countRetired++;
					data.put("verein", "0");

				} else {
					log.append("Spieler " + name + " nach einzel spielerupdate wieder in der Liga!! VereinsId: " + verein);
					countOk++;
				}

			}
		}
		log.append("\n");
		log.append("überprüfung abgeschlossen! nach Update erfolgreich: " + countOk + " - gescheitert: " + countNotOk + " - flag retired: " + countRetired);
		LOGGER.info(log.toString());
	}
}
