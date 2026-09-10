package comunio.nas.dataScraper.espn;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.util.ClubMapper;

/**
 * Mappt ESPN-Team-IDs auf Comunio-Vereins-IDs (1. Bundesliga).
 * <p>
 * Die ESPN-API verwendet eigene Team-IDs (z. B. {@code 598} für Union Berlin),
 * die nicht mit den Comunio-Vereins-IDs übereinstimmen. Dieses Mapping wird
 * einmalig aus den Vereinsnamen abgeleitet und kann als JSON exportiert werden.
 * <p>
 * Referenz-Team-IDs (Stand Saison 2026/27):
 * <pre>
 * 598  = 1. FC Union Berlin
 * 131  = Bayer Leverkusen
 * 132  = Bayern Munich
 * 124  = Borussia Dortmund
 * 268  = Borussia Mönchengladbach
 * 125  = Eintracht Frankfurt
 * 3841 = FC Augsburg
 * 122  = FC Cologne
 * 127  = Hamburg SV
 * 2950 = Mainz
 * 11420= RB Leipzig
 * 126  = SC Freiburg
 * 3307 = SC Paderborn 07
 * 10388= SV Elversberg
 * 133  = Schalke 04
 * 7911 = TSG Hoffenheim
 * 134  = VfB Stuttgart
 * 137  = Werder Bremen
 * </pre>
 */
public class EspnClubMapper {

	/** Manuelle Korrekturen: ESPN-Name → Comunio-Name (falls Normalisierung nicht reicht). */
	private static final Map<String, String> MANUAL_ESPN_TO_COMUNIO = new HashMap<>();

	static {
		MANUAL_ESPN_TO_COMUNIO.put("Bayern Munich", "FC Bayern München");
		MANUAL_ESPN_TO_COMUNIO.put("FC Cologne", "1. FC Köln");
		MANUAL_ESPN_TO_COMUNIO.put("Hamburg SV", "Hamburger SV");
		MANUAL_ESPN_TO_COMUNIO.put("Mainz", "1. FSV Mainz 05");
		MANUAL_ESPN_TO_COMUNIO.put("SC Paderborn 07", "SC Paderborn 07");
		MANUAL_ESPN_TO_COMUNIO.put("SV Elversberg", "SV Elversberg");
		MANUAL_ESPN_TO_COMUNIO.put("Borussia Mönchengladbach", "Borussia Mönchengladbach");
		MANUAL_ESPN_TO_COMUNIO.put("Borussia Dortmund", "Borussia Dortmund");
		MANUAL_ESPN_TO_COMUNIO.put("Eintracht Frankfurt", "Eintracht Frankfurt");
		MANUAL_ESPN_TO_COMUNIO.put("FC Augsburg", "FC Augsburg");
		MANUAL_ESPN_TO_COMUNIO.put("RB Leipzig", "RB Leipzig");
		MANUAL_ESPN_TO_COMUNIO.put("SC Freiburg", "SC Freiburg");
		MANUAL_ESPN_TO_COMUNIO.put("Schalke 04", "FC Schalke 04");
		MANUAL_ESPN_TO_COMUNIO.put("TSG Hoffenheim", "TSG Hoffenheim");
		MANUAL_ESPN_TO_COMUNIO.put("VfB Stuttgart", "VfB Stuttgart");
		MANUAL_ESPN_TO_COMUNIO.put("Werder Bremen", "Werder Bremen");
		MANUAL_ESPN_TO_COMUNIO.put("1. FC Union Berlin", "1. FC Union Berlin");
		MANUAL_ESPN_TO_COMUNIO.put("Bayer Leverkusen", "Bayer 04 Leverkusen");
	}

	/**
	 * Baut das Mapping ESPN-Team-ID → Comunio-Vereins-ID anhand der Vereinsnamen.
	 *
	 * @param espnTeams JSONObject aus {@link EspnApiClient#getTeams()}
	 * @param clubDB    JSONArray der Comunio-Vereinsdatenbank
	 * @return Map: ESPN-Team-ID → Comunio-Vereins-ID (nur gemappte Einträge)
	 */
	public static Map<String, String> buildEspnToComunioMap(JSONObject espnTeams, JSONArray clubDB) {
		Map<String, String> result = new HashMap<>();

		// ESPN-Teams durchgehen
		JSONArray sports = espnTeams.optJSONArray("sports");
		if (sports == null || sports.isEmpty()) {
			return result;
		}
		JSONArray leagueTeams = sports.getJSONObject(0).optJSONArray("leagues").getJSONObject(0).optJSONArray("teams");
		if (leagueTeams == null) {
			return result;
		}

		for (int i = 0; i < leagueTeams.length(); i++) {
			JSONObject team = leagueTeams.getJSONObject(i).optJSONObject("team");
			if (team == null) {
				continue;
			}
			String espnId = team.optString("id", "");
			String espnName = team.optString("displayName", "");

			// 1. Manuelle Korrektur versuchen
			String comunioName = MANUAL_ESPN_TO_COMUNIO.get(espnName);
			if (comunioName == null) {
				comunioName = espnName;
			}

			// 2. Comunio-Vereins-ID anhand des Namens suchen
			String comunioId = ClubMapper.getComunioIdFromName(comunioName, clubDB);
			if (comunioId != null && !comunioId.isEmpty()) {
				result.put(espnId, comunioId);
			}
		}

		return result;
	}

	/**
	 * Baut das Mapping als JSONObject (für Export als espnClubMapping.json).
	 *
	 * @param espnTeams JSONObject aus {@link EspnApiClient#getTeams()}
	 * @param clubDB    JSONArray der Comunio-Vereinsdatenbank
	 * @return JSONObject: {"mapping": [{"espnId": "...", "comunioId": "...", "espnName": "...", "comunioName": "..."}]}
	 */
	public static JSONObject buildMappingJson(JSONObject espnTeams, JSONArray clubDB) {
		JSONObject result = new JSONObject();
		JSONArray mapping = new JSONArray();

		JSONArray sports = espnTeams.optJSONArray("sports");
		if (sports != null && !sports.isEmpty()) {
			JSONArray leagueTeams = sports.getJSONObject(0).optJSONArray("leagues").getJSONObject(0).optJSONArray("teams");
			if (leagueTeams != null) {
				for (int i = 0; i < leagueTeams.length(); i++) {
					JSONObject team = leagueTeams.getJSONObject(i).optJSONObject("team");
					if (team == null) {
						continue;
					}
					String espnId = team.optString("id", "");
					String espnName = team.optString("displayName", "");

					String comunioName = MANUAL_ESPN_TO_COMUNIO.getOrDefault(espnName, espnName);
					String comunioId = ClubMapper.getComunioIdFromName(comunioName, clubDB);

					JSONObject entry = new JSONObject();
					entry.put("espnId", espnId);
					entry.put("espnName", espnName);
					entry.put("comunioName", comunioName);
					if (comunioId != null && !comunioId.isEmpty()) {
						entry.put("comunioId", comunioId);
					} else {
						entry.put("comunioId", JSONObject.NULL);
					}
					mapping.put(entry);
				}
			}
		}

		result.put("mapping", mapping);
		return result;
	}
}