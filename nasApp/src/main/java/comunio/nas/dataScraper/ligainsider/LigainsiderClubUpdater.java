package comunio.nas.dataScraper.ligainsider;

import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.LigainsiderClubObject;
import comunio.nas.util.ClubMapper;

public class LigainsiderClubUpdater {
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderClubParser.class);

	public static JSONArray updateLigainsiderClubDate(JSONArray clubDB) {

		if (!allClubDataAvailable(clubDB)) {

			Map<String, LigainsiderClubObject> extractedClubs = LigainsiderClubParser.parseAndMapLigaInsider();

			for (Map.Entry<String, LigainsiderClubObject> entry : extractedClubs.entrySet()) {
				LigainsiderClubObject clubObj = entry.getValue();
				JSONObject matchingComunioClub = ClubMapper.getComunioClubJSONFromName(clubObj.getName(), clubDB);
				matchingComunioClub.put("ligainsider", clubObj.toJSON());
			}
		}
		return clubDB;
	}

	private static boolean allClubDataAvailable(JSONArray clubDB) {
		for (int i = 0; i < clubDB.length(); i++) {
			JSONObject club = clubDB.getJSONObject(i);
			if (!club.has("ligainsider") || !club.isNull("ligainsider") || club.isEmpty()) {
				return false;
			}
			LigainsiderClubObject lco = LigainsiderClubObject.fromJSON(club.getJSONObject("ligainsider"));
			if (lco.getLink() == null || lco.getId() == null || lco.getName() == null || lco.getLink().isBlank() || lco.getId().isBlank() || lco.getName().isBlank()) {
				return false;
			}
		}
		return true;
	}

}
