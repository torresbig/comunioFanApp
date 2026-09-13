package comunio.nas.dataScraper.espn;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.espn.EspnClubContainer;
import comunio.nas.objects.espn.EspnClubMapObject;
import comunio.nas.objects.helper.LogManager;

public class EspnClubUpdater {

	private static final Logger LOGGER = LogManager.getLogger(EspnClubUpdater.class);

	public static Map<String, EspnClubMapObject> updateEspnToComunioClubMap(JSONArray clubDB) {
		JSONObject espnTeams = null;
		try {
			espnTeams = EspnApiClient.getTeams();
			LOGGER.info("Erfolgreich ESPN-Teams abgerufen: " + espnTeams.toString());
		} catch (IOException | InterruptedException e) {
			LOGGER.severe("Fehler beim Abrufen der ESPN-Teams: " + e.getMessage());
			e.printStackTrace();
		}
		Map<String, EspnClubMapObject> espnToComunio = EspnClubMapper.buildEspnToComunioClubMap(espnTeams, clubDB);
				
		return espnToComunio;
	}

}
