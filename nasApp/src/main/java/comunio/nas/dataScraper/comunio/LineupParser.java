package comunio.nas.dataScraper.comunio;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;
import comunio.nas.util.HttpHeaderUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LineupParser {

	private static final Logger LOGGER = LogManager.getLogger(LineupParser.class);
	private static final String CURRENT_KEY = "current";

	// Struktur: Spieltag -> (UserID -> UserDaten)
	private Map<String, Map<String, UserLineupData>> rootDataMap = new HashMap<>();
	private Instant lastCurrentLineupRefresh;

	/**
	 * Prüft, ob der aktuelle Tagesstand der Lineups neu geladen werden muss.
	 * Der aktuelle Cache wird einmal pro lokaler Tagesgrenze neu aktualisiert.
	 *
	 * @return true, wenn der Current-Stand noch nicht für den aktuellen Tag
	 *         geladen wurde.
	 */
	private boolean shouldRefreshCurrentLineup() {
		if (lastCurrentLineupRefresh == null) {
			return true;
		}
		LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
		LocalDate lastRefreshDay = lastCurrentLineupRefresh.atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
		return !today.equals(lastRefreshDay);
	}

	/**
	 * Prüft, ob für den Nutzer unter dem angegebenen Cache-Key bereits ein
	 * gültiger Lineup-Satz vorhanden ist.
	 * <p>
	 * Da einzelne Nutzer in Einzelfällen nur neun statt elf Spieler melden,
	 * gilt ein Eintrag bereits als gecached, sobald mindestens neun Spieler im
	 * Cache sind.
	 *
	 * @param cacheKey Schlüssel des Caches, z. B. "current" oder die Spieltag-ID.
	 * @param user Nutzer, dessen Eintrag geprüft werden soll.
	 * @return true, wenn ein valider Eintrag vorhanden ist.
	 */
	private boolean isUserCachedForKey(String cacheKey, User user) {
		if (cacheKey == null || user == null || user.getId() == null) {
			return false;
		}
		Map<String, UserLineupData> usersMap = rootDataMap.get(cacheKey);
		if (usersMap == null) {
			return false;
		}
		UserLineupData userLineup = usersMap.get(String.valueOf(user.getId()));
		if (userLineup == null || userLineup.players == null) {
			return false;
		}
		int size = userLineup.players.size();
		if (size >= 9) {
			LOGGER.fine("isUserCachedForKey: cacheKey=" + cacheKey + " UserId=" + user.getId() + " -> cached (players=" + size + ")");
			return true;
		}
		LOGGER.fine("isUserCachedForKey: cacheKey=" + cacheKey + " UserId=" + user.getId() + " -> NOT cached (players=" + size + ", expected>=9)");
		return false;
	}

	/**
	 * Extrahiert die Position eines Spielers aus dem API-Objekt.
	 *
	 * @param p JSON-Objekt des Spielers.
	 * @return Ganzzahlige Position im Lineup, oder null, falls keine gültige
	 *         Position vorhanden ist.
	 */
	private Integer parsePos(JSONObject p) {
		Object posObj = p.opt("pos");
		if (posObj == null) {
			return null;
		}
		if (posObj instanceof Number) {
			return ((Number) posObj).intValue();
		}
		String s = posObj.toString();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	/**
	 * Prüft, ob für den aktuellen Spieltag bereits ein Matchday-Snapshot im Cache
	 * vorhanden ist.
	 *
	 * @param matchdayInfo Informationen zum aktuellen Spieltag.
	 * @return true, wenn für diesen Spieltag bereits Daten gespeichert wurden.
	 */
	private boolean isMatchdayCached(MatchdayInfo matchdayInfo) {
		String spieltag = String.valueOf(matchdayInfo.getCurrentMatchday());
		return rootDataMap.containsKey(spieltag);
	}

	/**
	 * Kopiert den bereits für einen Spieltag gespeicherten Snapshot in den
	 * aktuellen Stand unter "current", ohne einen zweiten API-Call auszulösen.
	 *
	 * @param matchdayKey Schlüssel des Spieltags, dessen Daten in den Current-
	 *                    Cache übernommen werden sollen.
	 */
	private void copyMatchdaySnapshotToCurrent(String matchdayKey) {
		Map<String, UserLineupData> matchdayUsers = rootDataMap.get(matchdayKey);
		if (matchdayUsers == null || matchdayUsers.isEmpty()) {
			return;
		}
		Map<String, UserLineupData> currentUsers = rootDataMap.computeIfAbsent(CURRENT_KEY, k -> new HashMap<>());
		for (Map.Entry<String, UserLineupData> entry : matchdayUsers.entrySet()) {
			currentUsers.put(entry.getKey(), entry.getValue());
		}
		lastCurrentLineupRefresh = Instant.now();
	}

	/**
	 * Lädt für alle Nutzer die Lineups für den aktiven Spieltag.
	 * <p>
	 * Der erste erfolgreiche Fetch nach Anpfiff wird nur im Matchday-Cache
	 * gespeichert. Die gleichen Daten werden zusätzlich in "current" abgelegt,
	 * ohne einen zweiten API-Call zu starten. Wenn der aktuelle Stand später
	 * erneut aktualisiert werden muss, wird der vorhandene Matchday-Snapshot in
	 * "current" kopiert statt noch einmal zu laden.
	 *
	 * @param users Map aller Nutzer.
	 * @param matchdayInfo Informationen zum aktiven Spieltag.
	 */
	public void fetchLineupForAllUsers(Map<String, User> users, MatchdayInfo matchdayInfo) {
		if (users == null || matchdayInfo == null) {
			return;
		}

		String spieltag = String.valueOf(matchdayInfo.getCurrentMatchday());
		boolean activeMatchday = matchdayInfo.isStarted() && !matchdayInfo.isFinished();

		if (activeMatchday) {
			if (!isMatchdayCached(matchdayInfo)) {
				for (User user : users.values()) {
					fetchAndStoreLineup(user, matchdayInfo, true);
				}
			} else {
				for (User user : users.values()) {
					if (!isUserCachedForKey(spieltag, user)) {
						fetchAndStoreLineup(user, matchdayInfo, true);
					}
				}
			}
		}

		if (shouldRefreshCurrentLineup()) {
			if (activeMatchday && isMatchdayCached(matchdayInfo)) {
				copyMatchdaySnapshotToCurrent(spieltag);
			} else {
				for (User user : users.values()) {
					fetchAndStoreLineup(user, matchdayInfo, false);
				}
			}
		}
	}

	/**
	 * Lädt die Lineup-Daten aus einer bereits gespeicherten JSON-Struktur in den
	 * internen Cache.
	 *
	 * @param rootJson JSON-Root-Objekt mit Spieltag- und Userdaten.
	 */
	public void loadMapFromJsonString(JSONObject rootJson) {
		rootDataMap.clear();
		for (String key : rootJson.keySet()) {
			Object value = rootJson.get(key);
			if (!(value instanceof JSONObject)) {
				continue;
			}
			JSONObject matchdayObj = (JSONObject) value;
			Map<String, UserLineupData> userMap = new HashMap<>();
			for (String userId : matchdayObj.keySet()) {
				Object userObj = matchdayObj.get(userId);
				if (!(userObj instanceof JSONObject)) {
					continue;
				}
				JSONObject userJson = (JSONObject) userObj;
				UserLineupData lineup = new UserLineupData();
				lineup.tactic = userJson.optString("tactic", "unknown");
				JSONArray playersArray = userJson.optJSONArray("players");
				if (playersArray != null) {
					for (int i = 0; i < playersArray.length(); i++) {
						JSONObject playerJson = playersArray.getJSONObject(i);
						LineupPlayerData player = new LineupPlayerData();
						player.id = playerJson.optLong("id", 0L);
						player.name = playerJson.optString("name", "");
						player.kaderPos = playerJson.optInt("kaderPos", 0);
						player.wasLiveSubstituted = playerJson.optBoolean("wasLiveSubstituted", false);
						player.quotedprice = playerJson.optLong("quotedprice", 0L);
						player.substitute = playerJson.optBoolean("substitute", false);
						player.position = playerJson.optString("position", "unbekannt");
						lineup.players.add(player);
					}
				}
				userMap.put(userId, lineup);
			}
			rootDataMap.put(key, userMap);
		}
	}

	/**
	 * Serialisiert den kompletten Lineup-Cache als JSON-Objekt.
	 *
	 * @return JSON-Wurzelstruktur mit allen Matchday- und Current-Daten.
	 */
	public JSONObject getFinalJSONObject() {
		JSONObject json = new JSONObject();
		for (Map.Entry<String, Map<String, UserLineupData>> entry : rootDataMap.entrySet()) {
			JSONObject matchdayJson = new JSONObject();
			for (Map.Entry<String, UserLineupData> userEntry : entry.getValue().entrySet()) {
				matchdayJson.put(userEntry.getKey(), userEntry.getValue().toJson());
			}
			json.put(entry.getKey(), matchdayJson);
		}
		return json;
	}

	/**
	 * Ruft die Lineups eines einzelnen Nutzers aus der API ab und speichert sie.
	 * <p>
	 * Wenn <code>fixedMatchdaySnapshot</code> true ist, wird der Eintrag nur beim
	 * ersten erfolgreichen Fetch des Spieltags im Matchday-Cache abgelegt. Der
	 * aktuelle Stand unter "current" wird bei jedem erfolgreichen Fetch neu
	 * aktualisiert.
	 *
	 * @param user Nutzer, dessen Lineup geladen werden soll.
	 * @param matchdayInfo Aktueller Spieltag.
	 * @param fixedMatchdaySnapshot true für den einmaligen Matchday-Snapshot,
	 *                             false für den flüchtigen Current-Stand.
	 */
	private void fetchAndStoreLineup(User user, MatchdayInfo matchdayInfo, boolean fixedMatchdaySnapshot) {
		if (user == null || user.getId() == null || user.getId().equals("1")) {
			String userName = (user != null) ? user.getName() : "Unbekannt";
			LOGGER.info("fetchAndStoreLineup: User " + userName + " hat die ID 1 oder ist ungültig, wird übersprungen.");
			return;
		}

		String storageKey = fixedMatchdaySnapshot ? String.valueOf(matchdayInfo.getCurrentMatchday()) : CURRENT_KEY;
		if (fixedMatchdaySnapshot && isUserCachedForKey(storageKey, user)) {
			return;
		}

		try {
		String url = Urls.COM_LINEUP_LIVE(user);
			if(storageKey.equals("current")) {
				url = Urls.COM_LINEUP_MATCHDAY(user, matchdayInfo);
			}
			String doc = Jsoup.connect(url).userAgent(HttpHeaderUtil.getRandomUserAgent())
					.header("Accept", "application/json, text/plain, */*")
					.header("Authorization", "Bearer " + Login.getToken())
					.header("Accept-Encoding", "gzip, deflate, br, zstd")
					.header("Accept-Language", "de-DE,en-EN;q=0.9")
					.header("x-timezone", "Europe/Berlin")
					.ignoreContentType(true)
					.execute()
					.body();

			JSONObject responseJson = new JSONObject(doc);
			String taktik = responseJson.optString("tactic", "unknown");
			JSONArray sourcePlayers = responseJson.optJSONArray("items");
			List<LineupPlayerData> playerList = new ArrayList<>();

			if (sourcePlayers != null) {
				List<LineupPlayerData> tempPlayerList = new ArrayList<>();
				for (int i = 0; i < sourcePlayers.length(); i++) {
					JSONObject p = sourcePlayers.getJSONObject(i);
					Integer pos = parsePos(p);
					if (pos == null || pos < 1 || pos > 11) {
						LOGGER.fine("Skipping player (not in lineup 1-11): " + p.optString("name") + " id=" + p.optString("id"));
						continue;
					}
					LineupPlayerData player = new LineupPlayerData();
					player.id = p.optLong("id");
					player.name = p.optString("name");
					player.kaderPos = pos;
					player.wasLiveSubstituted = p.optBoolean("wasLiveSubstituted", false);
					player.quotedprice = p.optLong("quotedprice");
					player.substitute = p.optBoolean("substitute", false);
					player.position = p.optString("position", "unbekannt");
					tempPlayerList.add(player);
				}
				tempPlayerList.sort((a, b) -> Integer.compare(a.kaderPos, b.kaderPos));
				int count = Math.min(tempPlayerList.size(), 11);
				for (int i = 0; i < count; i++) {
					playerList.add(tempPlayerList.get(i));
				}
			}

			LOGGER.fine("fetchAndStoreLineup: User=" + user.getId() + " parsedStartPlayers=" + playerList.size());
			if (playerList.size() < 11) {
				LOGGER.fine("fetchAndStoreLineup: User=" + user.getId() + " hat weniger als 11 Startspieler (" + playerList.size() + "). Wird trotzdem gespeichert, aber gilt als unvollständig.");
			}

			UserLineupData userLineup = new UserLineupData();
			userLineup.tactic = taktik;
			userLineup.players = playerList;

			rootDataMap.computeIfAbsent(CURRENT_KEY, k -> new HashMap<>()).put(user.getId(), userLineup);
			lastCurrentLineupRefresh = Instant.now();

			if (fixedMatchdaySnapshot && !isUserCachedForKey(storageKey, user)) {
				rootDataMap.computeIfAbsent(storageKey, k -> new HashMap<>()).put(user.getId(), userLineup);
			}

		} catch (Exception e) {
			LOGGER.severe("Fehler beim Abrufen der Squad-Daten für User " + user.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

}