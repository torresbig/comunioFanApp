package comunio.nas.dataScraper.espn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.espn.EspnClubMapObject;
import comunio.nas.objects.espn.EspnPlayerContainer;
import comunio.nas.objects.espn.EspnPlayerMapObject;
import comunio.nas.objects.helper.LogManager;

public class EspnPlayerMapper {
	private static final Logger LOGGER = LogManager.getLogger(EspnPlayerMapper.class);

	/**
	 * Baut das Player-Mapping: ESPN-Spieler-ID → Comunio-Spieler-ID. Die Zuordnung
	 * erfolgt über Verein (Comunio-ID) + Namensübereinstimmung.
	 *
	 * @return JSONObject: {"mapping": [{"espnId": "...", "comunioId": "...",
	 *         "espnName": "...", "comunioName": "..."}]}
	 */
	static EspnPlayerContainer buildPlayerMapping(JSONObject playerDBObject, JSONArray espnPlayers, EspnPlayerContainer playerContainer, Map<String, EspnClubMapObject> espnToComunioClubMap) {
		EspnPlayerContainer result = playerContainer != null ? playerContainer : new EspnPlayerContainer(new HashMap<>());

		if (result.getPlayerMap() == null) {
			result.setPlayerMap(new HashMap<>());
		}

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		int espnCount = espnPlayers != null ? espnPlayers.length() : 0;
		int playerDbCount = playerDB != null ? playerDB.length() : 0;
		int mappingExactCount = 0;
		int mappingLastnameInitialCount = 0;
		int mappingLastnameOnlyCount = 0;
		int playerAlreadyMappedCount = 0;

		if (playerDB != null) {
			for (int i = 0; i < espnCount; i++) {
				boolean match = false;
				JSONObject espnPlayer = espnPlayers.getJSONObject(i);
				if (result.getPlayerMap().containsKey(espnPlayer.optString("espnId", ""))) {
					playerAlreadyMappedCount++;
					match = true;
					continue;
				}

				String espnClubId = espnPlayer.optJSONObject("club").optString("espnId", "");
				EspnClubMapObject espnClubMapObject = (espnToComunioClubMap != null && espnToComunioClubMap.containsKey(espnClubId)) ? espnToComunioClubMap.get(espnClubId) : null;

				if (espnClubMapObject == null) {
					LOGGER.warning("ESPN-Update: Kein Mapping für ESPN-Verein mit ESPN-ID " + espnClubId + " gefunden. Spieler " + espnPlayer.optString("name", "") + " (ESPN-ID: " + espnPlayer.optString("espnId", "") + ") wird übersprungen.");
					continue;
				}

				String comunioClubId = espnClubMapObject.getComunioId();

				if (comunioClubId == null || comunioClubId.isEmpty()) {
					continue;
				}

				String espnId = espnPlayer.optString("espnId", "");
				String espnName = espnPlayer.optString("name", "");
				String espnLastName = espnPlayer.optString("lastName", "");
				String espnFirstName = espnPlayer.optString("firstName", "");
				String espnShorttName = espnPlayer.optString("shortName", "");
				String espnJersey = espnPlayer.optString("jersey", "");
				String espnStatus = espnPlayer.optString("status", "");
				String espnPosition = espnPlayer.optString("position", "");
				String espnNationality = espnPlayer.optString("nationality", "");
				String espnDateOfBirth = espnPlayer.optString("dateOfBirth", "");
				String espnHeight = espnPlayer.optString("height", "");
				String espnWeight = espnPlayer.optString("weight", "");

				// DB-Spieler suchen (gleicher Verein + Namensübereinstimmung)
				for (int j = 0; j < playerDB.length(); j++) {
					JSONObject dbPlayer = playerDB.optJSONObject(j);
					if (dbPlayer == null) {
						continue;
					}
					JSONObject data = dbPlayer.optJSONObject("data");
					if (data == null) {
						continue;
					}
					String dbClubId = data.optString("verein", "");
					if (!comunioClubId.equals(dbClubId)) {
						continue;
					}

					String dbId = dbPlayer.optString("id", "");
					String dbName = dbPlayer.optString("name", "");

					int resultNameMatcher = namesMatch(espnName, dbName);
					if (resultNameMatcher == FALSE) {
						continue;
					} else if (resultNameMatcher == TRUE_EXACT) {
						LOGGER.info("ESPN-Update: Spieler " + espnName + " (ESPN-ID: " + espnId + ", ESPN-Verein: " + comunioClubId + ") → DB-Name: " + dbName + " (EXAKT)");
						mappingExactCount++;
					} else if (resultNameMatcher == TRUE_LASTNAME_INITIAL) {
						LOGGER.info("ESPN-Update: Spieler " + espnName + " (ESPN-ID: " + espnId + ", ESPN-Verein: " + comunioClubId + ") → DB-Name: " + dbName + " (NACHNAME+INITIAL)");
						mappingLastnameInitialCount++;
					} else if (resultNameMatcher == TRUE_LASTNAME_ONLY) {
						LOGGER.info("ESPN-Update: Spieler " + espnName + " (ESPN-ID: " + espnId + ", ESPN-Verein: " + comunioClubId + ") → DB-Name: " + dbName + " (NUR NACHNAME)");
						mappingLastnameOnlyCount++;
					}

					// Treffer → Mapping-Eintrag
					EspnPlayerMapObject espnPlayerMapObject = new EspnPlayerMapObject();
					espnPlayerMapObject.setClub(espnClubMapObject);
					espnPlayerMapObject.setEspnId(espnId);
					espnPlayerMapObject.setName(espnName);
					espnPlayerMapObject.setComunioId(dbId);
					espnPlayerMapObject.setComunioName(dbName);
					espnPlayerMapObject.setLastName(espnLastName);
					espnPlayerMapObject.setFirstName(espnFirstName);
					espnPlayerMapObject.setShortName(espnShorttName);
					espnPlayerMapObject.setJersey(espnJersey);
					espnPlayerMapObject.setStatus(espnStatus);
					espnPlayerMapObject.setPosition(espnPosition);
					espnPlayerMapObject.setNationality(espnNationality);
					espnPlayerMapObject.setDateOfBirth(espnDateOfBirth);
					espnPlayerMapObject.setHeight(espnHeight);
					espnPlayerMapObject.setWeight(espnWeight);
					espnPlayerMapObject.setMappedOnlyLastName(resultNameMatcher == TRUE_LASTNAME_ONLY);
					result.getPlayerMap().put(espnId, espnPlayerMapObject);
					match = true;
					break;
				}
				if (!match) {
					EspnPlayerMapObject espnPlayerMapObject = new EspnPlayerMapObject();
					espnPlayerMapObject.setClub(espnClubMapObject);
					espnPlayerMapObject.setEspnId(espnId);
					espnPlayerMapObject.setName(espnName);
					result.addFailedPlayer(espnPlayerMapObject);

				}
			}
		}

		result.setMappedOnlyLastName(mappingLastnameOnlyCount);
		result.setMappedInitials(mappingLastnameInitialCount);

		LOGGER.info("ESPN-Update: Spieler-Mapping abgeschlossen. ESPN-Spieler: " + espnCount + ", DB-Spieler: " + playerDbCount + ", gemappt: " + result.getMapped() + ", fehlgeschlagen: " + result.getMappingFailed() + ", exakt: " + mappingExactCount + ", Nachname+Initial: " + mappingLastnameInitialCount + ", Nur Nachname: " + mappingLastnameOnlyCount + ", bereits gemappt: " + playerAlreadyMappedCount);

		return result;
	}


	/**
	 * Namens-Matching: Nachname exakt ODER Nachname + Vorname-Initial ODER nur
	 * Nachname (bei langen Vornamen). Der Verein muss bereits geprüft sein. returns
	 * TRUE_EXACT, TRUE_LASTNAME_INITIAL, TRUE_LASTNAME_ONLY oder FALSE.
	 * 
	 * 
	 */

	static final int FALSE = 0;
	static final int TRUE_EXACT = 1;
	static final int TRUE_LASTNAME_INITIAL = 2;
	static final int TRUE_LASTNAME_ONLY = 3;

	static int namesMatch(String espnName, String dbName) {
		if (espnName == null || dbName == null) {
			return FALSE;
		}
		String normEspn = comunio.nas.util.player.PlayerHelper.normalizeName(espnName);
		String normDb = comunio.nas.util.player.PlayerHelper.normalizeName(dbName);
		if (normEspn.isEmpty() || normDb.isEmpty()) {
			return FALSE;
		}
		// Direkte Übereinstimmung
		if (normEspn.equals(normDb)) {
			return TRUE_EXACT;
		}
		// Nachname + Initial
		String[] espnParts = normEspn.split("\\s+");
		String[] dbParts = normDb.split("\\s+");
		if (espnParts.length >= 2 && dbParts.length >= 2) {
			String espnLast = espnParts[espnParts.length - 1];
			String dbLast = dbParts[dbParts.length - 1];
			if (espnLast.equals(dbLast)) {
				// Gleicher Nachname + gleiche Initial
				String espnInit = espnParts[0].substring(0, 1);
				String dbInit = dbParts[0].substring(0, 1);
				if (espnInit.equals(dbInit)) {
					return TRUE_LASTNAME_INITIAL;
				}
				// Nur Nachname (bei langen Vornamen) – Verein ist bereits geprüft
				return TRUE_LASTNAME_ONLY;
			}
		}
		if (dbParts.length == 1 && espnParts.length >= 2) {
			// DB-Name nur Nachname, ESPN-Name Nachname + Vorname
			String dbLast = dbParts[0];
			String espnLast = espnParts[espnParts.length - 1];
			if (dbLast.equals(espnLast)) {
				return TRUE_LASTNAME_ONLY;
			}
		}
		return FALSE;
	}

}
