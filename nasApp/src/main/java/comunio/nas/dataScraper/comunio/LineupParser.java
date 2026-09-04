package comunio.nas.dataScraper.comunio;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;
import comunio.nas.util.HttpHeaderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LineupParser {

	private static final Logger LOGGER = LogManager.getLogger(LineupParser.class);

	// Struktur: Spieltag -> (UserID -> UserDaten)
	private Map<String, Map<String, Object>> rootDataMap = new HashMap<>();

	// Prüft, ob der User für diesen Spieltag bereits in der Map ist
	private boolean isUserCached(MatchdayInfo matchdayInfo, User user) {
		String spieltag = String.valueOf(matchdayInfo.getCurrentMatchday());
		return isMatchdayCached(matchdayInfo) && rootDataMap.get(spieltag).containsKey(String.valueOf(user.getId()));
	}

	private boolean isMatchdayCached(MatchdayInfo matchdayInfo) {
		String spieltag = String.valueOf(matchdayInfo.getCurrentMatchday());
		return rootDataMap.containsKey(spieltag);
	}

	public void fetchLineupForAllUsers(Map<String, User> users, MatchdayInfo matchdayInfo) {
		if (matchdayInfo.isStarted() && !matchdayInfo.isFinished()) {
			// Wenn der Spieltag gestartet ist, aber noch nicht beendet, werden die
			// aufstellungen abgefragt. sonst nicht.

			if (isMatchdayCached(matchdayInfo)) {
				// Wenn der Spieltag schon in der Map ist, nur die User abfragen, die noch nicht
				// drin sind
				for (User user : users.values()) {
					if (!isUserCached(matchdayInfo, user)) {
						fetchAndStoreLineup(user, matchdayInfo);
					}
				}
				return;
			}
			for (User user : users.values()) {
				fetchAndStoreLineup(user, matchdayInfo);
			}
		}
	}

	// Wandelt ein vorhandenes JSON (als String) wieder in die interne Map-Struktur
	// um
	@SuppressWarnings("unchecked")
	public void loadMapFromJsonString(JSONObject rootJson) {
		Map<String, Object> genericMap = rootJson.toMap();

		rootDataMap.clear();
		for (Map.Entry<String, Object> entry : genericMap.entrySet()) {
			if (entry.getValue() instanceof Map) {
				rootDataMap.put(entry.getKey(), (Map<String, Object>) entry.getValue());
			}
		}
	}

	// 6. Am Ende die gesamte Map mit einem Befehl in ein JSONObject umwandeln
	public JSONObject getFinalJSONObject() {
		return new JSONObject(rootDataMap);
	}

	private void fetchAndStoreLineup(User user, MatchdayInfo matchdayInfo) {

		// Prüfen, ob User-Objekt oder ID vorhanden ist (id ist String in deinem
		// User-Objekt)
		if (user == null || user.getId() == null || user.getId().equals("1")) {
			String userName = (user != null) ? user.getName() : "Unbekannt";
			LOGGER.info("fetchAndStoreLineup: User " + userName + " hat die ID 1 oder ist ungültig, wird übersprungen.");
			return;
		}

		String spieltag = String.valueOf(matchdayInfo.getCurrentMatchday());

		// Wenn die Daten für den User an diesem Spieltag schon existieren, nicht
		// nochmal abfragen
		if (isUserCached(matchdayInfo, user)) {
			return;
		}

		try {
			// 1. API-Daten via Jsoup abrufen (mit ignoreContentType für JSON)

			String doc = Jsoup.connect(Urls.COM_LINEUP(user)).userAgent(HttpHeaderUtil.getRandomUserAgent()).header("Accept", "application/json, text/plain, */*").header("Authorization", "Bearer " + Login.getToken()).header("Accept-Encoding", "gzip, deflate, br, zstd").header("Accept-Language", "de-DE,en-EN;q=0.9").header("x-timezone", "Europe/Berlin").ignoreContentType(true).execute().body();

			JSONObject responseJson = new JSONObject(doc);

			// 2. Taktik auslesen (falls im Squad-Endpoint vorhanden, sonst Default)
			String taktik = responseJson.optString("tactic", "unknown");

			// 3. Spieler-Liste (aus dem "items"-Array der Squad-API) auslesen und in Maps
			// verpacken
			JSONArray sourcePlayers = responseJson.optJSONArray("items");
			List<Map<String, Object>> playerList = new ArrayList<>();

			if (sourcePlayers != null) {
				for (int i = 0; i < sourcePlayers.length(); i++) {
					JSONObject p = sourcePlayers.getJSONObject(i);
					boolean linedup = p.optBoolean("linedup", false);
					if (!linedup) {
						continue; // Spieler nicht in der Startaufstellung, überspringen
					}
					Map<String, Object> playerMap = new HashMap<>();
					// Hier greifen wir auf die Felder aus dem Squad-Endpoint zu
					playerMap.put("id", p.optLong("id"));
					playerMap.put("name", p.optString("name"));
					playerMap.put("kaderPos", Integer.parseInt(p.optString("pos"))); // Entspricht deiner alten "type"-Struktur
					
					playerMap.put("wasLiveSubstituted", p.optBooleanObject("wasLiveSubstituted", false));	
					// Weitere Attribute, die die Squad-API liefert (kannst du anpassen):
					playerMap.put("quotedprice", p.optLong("quotedprice"));
					playerMap.put("substitute", p.optBooleanObject("substitute", false));
					playerMap.put("position", p.optString("position", "unbekannt"));
					playerList.add(playerMap);
				}
			}

			// 4. User-Daten-Map befüllen (enthält Taktik und die Spieler-Liste)
			Map<String, Object> userDataMap = new HashMap<>();
			userDataMap.put("tactic", taktik);
			userDataMap.put("players", playerList);

			// 5. In die Spieltags-Struktur einhängen
			rootDataMap.computeIfAbsent(spieltag, k -> new HashMap<>()).put(user.getId(), userDataMap);

		} catch (Exception e) {
			LOGGER.severe("Fehler beim Abrufen der Squad-Daten für User " + user.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

}