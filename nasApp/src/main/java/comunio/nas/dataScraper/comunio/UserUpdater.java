package comunio.nas.dataScraper.comunio;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.enu.Position;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.community.Community;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.user.User;
import comunio.nas.util.HttpHeaderUtil;

public class UserUpdater {

	private static final Logger LOGGER = LogManager.getLogger(UserUpdater.class);

	public static void updateAllUsers(LastUpdates lastUpdates, JSONObject playerDbObject, JSONArray marketValueDB, JSONObject notInligaDBObj, Map<String, String> playerToUserMap, JSONArray userDB, Community community, MatchdayInfo matchdayInfo, NewsManager newsManager, User user) {
		
		Instant now = Instant.now();
		Instant lastUserUpdateInstant = lastUpdates.getUsers();
		if (lastUserUpdateInstant == null) {
			lastUserUpdateInstant = Instant.EPOCH; // Sehr alt, damit Update erzwungen wird
		}
		ComunioDate lastUserUpdate = new ComunioDate(Date.from(lastUserUpdateInstant));

		
		if (lastUserUpdate.before(new ComunioDate(Date.from(now)).addDays(-30))) {
			try {
				JSONArray apiUsers = fetchStandingsAsArray(community);
				if (apiUsers == null || apiUsers.length() == 0) {
					LOGGER.warning("updateAllUsers: Keine API-User-Daten – Abbruch.");
					return;
				}
				syncUsersJsonArray(playerToUserMap, apiUsers, userDB, matchdayInfo, newsManager, user);
				
				boolean foundComputer = false;
				for (int i = 0; i < userDB.length(); i++) {
					JSONObject userObj = (JSONObject) userDB.get(i);
					JSONObject userNew = userObj.optJSONObject("user", new JSONObject());
					if (userNew.has("id")) {
						TeamsFromUser.mergePlayerDataWithDB(playerDbObject, marketValueDB, newsManager, userObj, playerToUserMap, notInligaDBObj, lastUpdates);
					}
					if (!foundComputer && userNew.optString("id", "").equals("1")) {
						foundComputer = true;
						LOGGER.info("COMPUTER-User (id=1) gefunden in userDB.");
					}
				}
				if (!foundComputer) {
					LOGGER.warning("COMPUTER-User (id=1) nicht gefunden in userDB nach Sync! Bitte prüfen.");
					// Optional: COMPUTER-User automatisch hinzufügen, um Inkonsistenzen zu vermeiden
						JSONObject computerUser = new JSONObject();
						JSONObject computerUserData = new JSONObject();
						computerUserData.put("id", "1");
						computerUserData.put("name", "Computer");
						computerUserData.put("firstname", "Computer");
						computerUserData.put("lastname", "");
						computerUserData.put("negativesBudget", false);
						computerUser.put("user", computerUserData);
						computerUser.put("punkte", 0);
						computerUser.put("teamValue", 0L);
						userDB.put(computerUser);
						LOGGER.info("COMPUTER-User automatisch hinzugefügt zur userDB.");
				}

				lastUpdates.setUsers(now);
			} catch (Exception e) {
				LOGGER.warning("Fehler in updateAllUsers: " + e.getMessage());
			}
		}

	}

	/**
	 * 
	 * Synchronisiert userDB mit apiUsers.
	 * 
	 * Vorhandene Benutzer in userDB werden aktualisiert.
	 * 
	 * Nicht vorhandene Benutzer aus apiUsers werden neu am Ende von userDB
	 * hinzugefügt.
	 * 
	 * Benutzer, die in userDB existieren, aber nicht in apiUsers vorkommen, werden
	 * aus userDB gelöscht
	 * 
	 * und in playerToUserMap auf COMPUTER ("1") umgehängt (Value-Mapping).
	 * 
	 * Wichtige Hinweise:
	 * 
	 * Es wird nur in Schritt 2 gelöscht (gezielt die Elemente, die nicht mehr in
	 * der API sind).
	 * 
	 * Beim Aktualisieren/Anlegen werden bestehende Einträge nicht verschoben.
	 * 
	 * @param playerToUserMap Map von playerId -> userId; alle Einträge, deren Value
	 *                        der gelöschten userId entspricht, werden auf "1"
	 *                        gesetzt (COMPUTER).
	 *
	 * @param apiUsers        JSONArray aus der API
	 * @param userDB          das lokale JSONArray (wird in-place verändert)
	 */
	public static void syncUsersJsonArray(Map<String, String> playerToUserMap, JSONArray apiUsers, JSONArray userDB, MatchdayInfo matchdayInfo, NewsManager newsManager, User user) {
		if (apiUsers == null) {
			LOGGER.warning("syncUsersJsonArray: apiUsers ist null – Abbruch.");
			return;
		}
		if (userDB == null) {
			LOGGER.warning("syncUsersJsonArray: userDB ist null – Abbruch.");
			return;
		}

		// Index für userDB aufbauen: userId -> Index im userDB-Array
		Map<String, Integer> dbIndexById = buildIndexByUserId(userDB);

		// Set der in API gesehenen IDs
		Set<String> seenInApi = new HashSet<>();

		StringBuilder addedLog = new StringBuilder();
		StringBuilder updatedLog = new StringBuilder();
		StringBuilder removedLog = new StringBuilder();
		StringBuilder reassignLog = new StringBuilder();

		int added = 0;
		int updated = 0;
		int skipped = 0;

		// 1) Einfügen/Aktualisieren anhand API
		for (int i = 0; i < apiUsers.length(); i++) {
			JSONObject apiItem = apiUsers.optJSONObject(i);
			if (apiItem == null) {
				skipped++;
				continue;
			}
			String userId = extractIdAsString(apiItem.opt("userID"));
			if (userId == null || userId.isBlank()) {
				skipped++;
				continue;
			}
			seenInApi.add(userId);

			Integer dbIdx = dbIndexById.get(userId);
			String loginNameLog = "";
			JSONObject apiUserForLog = apiItem.optJSONObject("user");
			if (apiUserForLog != null) {
				loginNameLog = apiUserForLog.optString("loginName", "");
			}

			if (dbIdx == null) {
				// Neu anlegen (am Ende anhängen)
				JSONObject newEntry = createDbEntryFromApi(apiItem);
				userDB.put(newEntry);
				dbIndexById.put(userId, userDB.length() - 1);
				added++;
				addedLog.append("Neuer User angelegt: userId=").append(userId).append(", loginName=").append(loginNameLog).append("\n");
			} else {
				// Aktualisieren (keine Verschiebung)
				JSONObject existing = userDB.optJSONObject(dbIdx);
				if (existing == null) {
					JSONObject replacement = createDbEntryFromApi(apiItem);
					userDB.put(dbIdx, replacement);
					updated++;
					updatedLog.append("Defekter Eintrag ersetzt an Index ").append(dbIdx).append(": userId=").append(userId).append(", loginName=").append(loginNameLog).append("\n");
				} else {
					updateDbEntryFromApi(existing, apiItem, matchdayInfo, newsManager);
					// loginName nach Update ggf. aus DB holen
					JSONObject exUser = existing.optJSONObject("user");
					if (exUser != null && (loginNameLog == null || loginNameLog.isBlank())) {
						loginNameLog = exUser.optString("loginName", "");
					}
					updated++;
					updatedLog.append("User aktualisiert: userId=").append(userId).append(", loginName=").append(loginNameLog).append("\n");
				}
			}

		}

		for (int i = 0; i < userDB.length(); i++) {
			JSONObject userObjJson = (JSONObject) userDB.get(i);
			JSONObject userJson = userObjJson.getJSONObject("user");
			if (userJson.getString("id").equals(user.getId())) {
				user.updateFromJson(userObjJson);
			}

		}

		// 2) Entfernen: DB-User, die nicht in der API sind, löschen (rückwärts
		// iterieren)
		int removed = 0;
		int reassignedTotal = 0;

		for (int idx = userDB.length() - 1; idx >= 0; idx--) {
			JSONObject dbItem = userDB.optJSONObject(idx);
			if (dbItem == null)
				continue;

			JSONObject dbUser = dbItem.optJSONObject("user");
			if (dbUser == null)
				continue;

			String userId = extractIdAsString(dbUser.opt("id"));
			if (userId == null || userId.isBlank())
				continue;

			if (userId.equals("1")) {
				continue;
			}

			if (!seenInApi.contains(userId)) {
				// PlayerToUserMap: alle Values, die dieser userId entsprechen, auf "1" setzen
				int reassigned = 0;
				if (playerToUserMap != null) {
					for (Map.Entry<String, String> entry : playerToUserMap.entrySet()) {
						if (userId.equals(entry.getValue())) {
							entry.setValue("1"); // auf COMPUTER setzen
							reassigned++;
						}
					}
				}
				reassignedTotal += reassigned;
				if (reassigned > 0) {
					reassignLog.append("PlayerToUserMap: ").append(reassigned).append(" Zuordnungen auf COMPUTER (1) gesetzt für entfernte userId=").append(userId).append("\n");
				}

				// Aus userDB entfernen
				userDB.remove(idx);
				removed++;
				removedLog.append("User aus DB entfernt (nicht in API vorhanden): userId=").append(userId).append("\n");
			}

		}

		// Zusammenfassung
		StringBuilder summary = new StringBuilder();
		summary.append("syncUsersJsonArray: API=").append(apiUsers.length())//
				.append(", DB_nachSync=").append(userDB.length())//
				.append(", hinzugefügt=").append(added)//
				.append(", aktualisiert=").append(updated)//
				.append(", entfernt=").append(removed)//
				.append(", reassignedInMap=").append(reassignedTotal)//
				.append(", übersprungen=").append(skipped).append("\n");

		if (addedLog.length() > 0)
			summary.append(addedLog);
		if (updatedLog.length() > 0)
			summary.append(updatedLog);
		if (removedLog.length() > 0)
			summary.append(removedLog);
		if (reassignLog.length() > 0)
			summary.append(reassignLog);

		LOGGER.warning(summary.toString());
	}

	// Index map: userId -> index im JSONArray
	private static Map<String, Integer> buildIndexByUserId(JSONArray userDB) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < userDB.length(); i++) {
			JSONObject item = userDB.optJSONObject(i);
			if (item == null)
				continue;
			JSONObject user = item.optJSONObject("user");
			if (user == null)
				continue;
			String userId = extractIdAsString(user.opt("id"));
			if (userId == null || userId.isBlank())
				continue;
			// Nur den ersten Vorkommnis-Index behalten (Duplikate vermeiden)
			map.putIfAbsent(userId, i);
		}
		return map;
	}

	private static String extractIdAsString(Object idVal) {
		if (idVal == null)
			return null;
		if (idVal instanceof String)
			return (String) idVal;
		if (idVal instanceof Number)
			return String.valueOf(((Number) idVal).longValue());
		return String.valueOf(idVal);
	}

	// Erzeugt einen vollständigen DB-Eintrag aus einem API-Item
	private static JSONObject createDbEntryFromApi(JSONObject apiItem) {
		JSONObject dbItem = new JSONObject();
		// user-Objekt übernehmen (oder minimal aus userID bauen)
		JSONObject apiUser = apiItem.optJSONObject("user");
		if (apiUser != null) {
			// sicherstellen, dass negativesBudget (DB-Feld) vorhanden ist
			if (!apiUser.has("negativesBudget") && apiUser.has("negativeBudget")) {
				JSONObject tmp = new JSONObject(apiUser.toString());
				tmp.put("negativesBudget", apiUser.optBoolean("negativeBudget", false));
				apiUser = tmp;
			}
			dbItem.put("user", new JSONObject(apiUser.toString()));
		} else {
			String uid = extractIdAsString(apiItem.opt("userID"));
			JSONObject u = new JSONObject();
			if (uid != null)
				u.put("id", uid);
			dbItem.put("user", u);
		}

		// Punkte aus totalPoints
		dbItem.put("punkte", apiItem.optInt("totalPoints", 0));

		// Rank hinzufügen, wenn vorhanden!
		if (apiItem.optInt("rank", 0) > 0) {
			dbItem.put("rank", apiItem.optInt("rank", 0));
		}

		// Position übernehmen!
		if (apiItem.optInt("position", 0) > 0) {
			dbItem.put("position", apiItem.optInt("position", 0));
		}

		// teamValue
		dbItem.put("teamValue", safeLong(apiItem.opt("teamValue"), 0L));

		// lastPoints (String möglich, z. B. "-")
		if (apiItem.has("lastPoints")) {
			dbItem.put("lastPoints", apiItem.opt("lastPoints"));
		} else {
			dbItem.put("lastPoints", JSONObject.NULL);
		}

		if (apiItem.has("mitgliedSeit")) {
			dbItem.put("mitgliedSeit", apiItem.optString("mitgliedSeit", ""));
		}

		UserDataLoader.fetchDataForUserJson(dbItem);

		return dbItem;

	}

	/**
	 * Aktualisiert einen bestehenden DB-Eintrag basierend auf Daten aus der API.
	 *
	 * - Aktualisiert Gesamtpunkte ("punkte"), Teamwert ("teamValue"), lastPoints
	 * und Basis-Userdaten. - Führt zusätzlich eine Punkte-Historie
	 * ("punkteHistorie") pro Spieltag. -> Nur wenn ein neuer Spieltag vorliegt
	 * (ermittelt über MatchdayInfo). -> Fehlende Spieltage werden nachgetragen,
	 * indem Differenzen aus totalPoints und Historie errechnet werden. -> Falls
	 * Differenzwerte negativ wirken, wird stattdessen lastPoints genutzt.
	 *
	 * @param dbItem       Das bestehende User-Objekt in der lokalen Datenbank
	 *                     (JSONArray).
	 * @param apiItem      Eintrag des Users wie aus der Comunio-API geliefert
	 *                     (Standings).
	 * @param matchdayInfo Der aktuelle Spieltag (inkl. Informationen ob gestartet /
	 *                     beendet).
	 */
	private static void updateDbEntryFromApi(JSONObject dbItem, JSONObject apiItem, MatchdayInfo matchdayInfo, NewsManager newsManager) {
		// --- Grundwerte übernehmen ---
		int totalPoints = apiItem.optInt("totalPoints", dbItem.optInt("punkte", 0));
		dbItem.put("punkte", totalPoints);

		// teamValue aktualisieren
		dbItem.put("teamValue", safeLong(apiItem.opt("teamValue"), dbItem.optLong("teamValue", 0L)));

		// lastPoints (kann Zahl oder "-" sein)
		if (apiItem.has("lastPoints")) {
			dbItem.put("lastPoints", apiItem.opt("lastPoints"));
		}
		Object lastPointsObj = apiItem.opt("lastPoints");

		// --- User-Block aktualisieren ---
		JSONObject apiUser = apiItem.optJSONObject("user");

		JSONObject dbUser = dbItem.optJSONObject("user");
		if (dbUser == null) {
			dbUser = new JSONObject();
			dbItem.put("user", dbUser);
		}
		if (apiUser != null) {
			putIfPresent(dbUser, "id", apiUser);
			putIfPresent(dbUser, "name", apiUser);

			// Rank hinzufügen, wenn vorhanden!
			if (apiItem.optInt("rank", 0) > 0) {
				dbItem.put("rank", apiItem.optInt("rank", 0));
			}

			// Position übernehmen!
			if (apiItem.optInt("position", 0) > 0) {
				dbItem.put("position", apiItem.optInt("position", 0));
			}

			// negativesBudget finalisieren
			if (apiUser.has("negativesBudget")) {
				dbUser.put("negativesBudget", apiUser.optBoolean("negativesBudget", dbUser.optBoolean("negativesBudget", false)));
			} else if (apiUser.has("negativeBudget")) {
				dbUser.put("negativesBudget", apiUser.optBoolean("negativeBudget", dbUser.optBoolean("negativesBudget", false)));
			}

			if (apiUser.has("blocked")) {
				dbUser.put("blocked", apiUser.optBoolean("blocked", dbUser.optBoolean("blocked", false)));
			}
			if (apiUser.has("position")) {
				dbUser.put("position", apiUser.optInt("position", dbUser.optInt("position", 0)));
			}
		}

		// --- Punktehistorie führen ---
		try {
			// ermitteln, welcher Spieltag der letzte in der Historie ist
			int lastInHistorie = getLastMatchdayFromHistorie(dbItem);

			int currentMatchday = matchdayInfo.getPointsMatchday();

			// Prüfen, ob neuer Spieltag vorliegt und Punkte verfügbar sind
			if (currentMatchday > lastInHistorie && matchdayInfo.canFetchPoints()) {

				// lastPoints normalisieren (int)
				int lastPoints = 0;
				if (lastPointsObj instanceof Number) {
					lastPoints = ((Number) lastPointsObj).intValue();
				} else if (lastPointsObj instanceof String) {
					try {
						lastPoints = Integer.parseInt((String) lastPointsObj);
					} catch (Exception e) {
						lastPoints = 0;
					}
				}

				newsManager.addNews(News.getUserpoints(dbUser.getString("name"), dbUser.getString("id"), lastPoints, totalPoints), true);

				updatePunkteHistorie(dbItem, currentMatchday, totalPoints, lastPoints, LOGGER);
			} else {
				LOGGER.fine("Kein neuer Spieltag für user=" + dbUser.optString("id") + " (aktueller=" + currentMatchday + ", letzter in Historie=" + lastInHistorie + ")");
			}
		} catch (Exception e) {
			LOGGER.warning("Fehler beim Aktualisieren der PunkteHistorie für user=" + dbUser.optString("id") + ": " + e.getMessage());
		}
	}

	/**
	 * Ermittelt den höchsten vorhandenen Spieltag in der Historie. Falls noch keine
	 * vorhanden ist, wird 0 zurückgegeben.
	 */
	private static int getLastMatchdayFromHistorie(JSONObject dbItem) {
		JSONObject historie = dbItem.optJSONObject("punkteHistorie");
		if (historie == null)
			return 0;

		int max = 0;
		for (String key : historie.keySet()) {
			try {
				int k = Integer.parseInt(key);
				if (k > max)
					max = k;
			} catch (Exception ignored) {
			}
		}
		return max;
	}

	private static void putIfPresent(JSONObject target, String key, JSONObject source) {
		if (source != null && source.has(key)) {
			target.put(key, source.opt(key));
		}
	}

	private static long safeLong(Object val, long def) {
		if (val == null)
			return def;
		if (val instanceof Number)
			return ((Number) val).longValue();
		try {
			return Long.parseLong(String.valueOf(val));
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Ruft die Comunio-Standings ab und extrahiert pro Item: - userID (long) -
	 * teamValue (long) - lastPoints (String; kann "-" sein) - totalPoints (int) -
	 * rank (int; Position im Array + 1)
	 *
	 * Fehler- und Sonderfälle: - Fehlen Felder, werden Defaults genutzt (optXxx)
	 * oder Einträge übersprungen (kritisch: userID). - Bei Fehlern wird ein leeres
	 * Array zurückgegeben und geloggt.
	 *
	 * @param communityId ID der Community (z. B. 884691)
	 * @return JSONArray mit JSONObjects: { "userID", "teamValue", "lastPoints",
	 *         "totalPoints", "rank" }
	 */
	public static JSONArray fetchStandingsAsArray(Community community) {
		String url = "https://www.comunio.de/api/communities/" + community.getId() + "?include=standings&lineBreaks2Description=1";

		try {
			Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
			String jsonResponse = Jsoup.connect(url)//
					.userAgent(HttpHeaderUtil.getRandomUserAgent())//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Accept-Encoding", "gzip, deflate, br, zstd")//
					.header("Accept-Language", "de-DE,en-EN;q=0.9")//
					.header("x-timezone", "Europe/Berlin")//
					.ignoreContentType(true)//
					.execute()//
					.body();

			JSONObject root = new JSONObject(jsonResponse);

			if (!root.has("standings")) {
				LOGGER.warning("fetchStandingsAsArray: Antwort enthält kein 'standings'-Objekt. URL: " + url);
				return new JSONArray();
			}

			JSONObject standings = root.optJSONObject("standings");
			if (standings == null) {
				LOGGER.warning("fetchStandingsAsArray: 'standings' ist null. URL: " + url);
				return new JSONArray();
			}

			JSONArray items = standings.optJSONArray("items");
			if (items == null) {
				LOGGER.warning("fetchStandingsAsArray: 'standings.items' ist null oder fehlt. URL: " + url);
				return new JSONArray();
			}

			JSONObject rules = standings.optJSONObject("rules");
			if (rules != null) {
				community.setSettings(rules);
			}

			JSONArray result = new JSONArray();
			StringBuilder log = new StringBuilder();
			int skipped = 0;

			for (int i = 0; i < items.length(); i++) {
				JSONObject item = items.optJSONObject(i);
				if (item == null) {
					skipped++;
					log.append("Item an Position ").append(i).append(" ist null – übersprungen.\n");
					continue;
				}

				int rank = i + 1;

				int totalPoints = item.optInt("totalPoints", 0);
				String lastPoints = item.optString("lastPoints", "");
				int totalPerennialPoints = item.optInt("totalPerennialPoints", 0);
				Object livePoints = item.has("livePoints") ? item.opt("livePoints") : JSONObject.NULL;

				JSONObject embedded = item.optJSONObject("_embedded");
				if (embedded == null) {
					skipped++;
					log.append("_embedded fehlt für Rank ").append(rank).append(" – Item wird übersprungen.\n");
					continue;
				}

				JSONObject user = embedded.optJSONObject("user");
				if (user == null) {
					skipped++;
					log.append("user fehlt für Rank ").append(rank).append(" – Item wird übersprungen.\n");
					continue;
				}
				if (!user.has("id")) {
					skipped++;
					log.append("Spieler hat keine ID! Rank ").append(rank).append("\n");
					continue;
				}

				long userIdLong = user.optLong("id", -1L);
				if (userIdLong <= 0) {
					skipped++;
					log.append("Ungültige userID ").append(userIdLong).append(" bei Rank ").append(rank).append("\n");
					continue;
				}
				String userId = String.valueOf(userIdLong);

				String userName = user.optString("name", "");
				boolean userNegativeBudget = user.optBoolean("negativeBudget", false);
				int userPosition = user.optInt("position", 0);

				JSONObject teamInfo = embedded.optJSONObject("teamInfo");
				long teamValue = 0L;
				if (teamInfo == null) {
					log.append("teamInfo fehlt für userID ").append(userId).append(" (Rank ").append(rank).append(") – setze teamValue=0.\n");
				} else {
					teamValue = teamInfo.optLong("teamValue", 0L);
				}

				JSONObject out = new JSONObject();
				// ID separat für Sync
				out.put("userID", userId);

				// Rang + Punkte
				out.put("rank", rank); // TODO: WIESO ist der nciht in der json
				out.put("totalPoints", totalPoints);
				out.put("lastPoints", lastPoints);
				out.put("totalPerennialPoints", totalPerennialPoints);
				out.put("livePoints", livePoints == null ? JSONObject.NULL : livePoints);

				// teamInfo (reduziert auf Wert; bei Bedarf erweiterbar)
				out.put("teamValue", teamValue);

				// eingebetteter user-Block, passend zu deiner userDB-Namensgebung
				JSONObject outUser = new JSONObject();
				outUser.put("id", userId);
				outUser.put("name", userName);
				// beachte Namenskonvention deiner DB: "negativesBudget"
				outUser.put("negativesBudget", userNegativeBudget);
				if (userPosition > 0) {
					outUser.put("position", userPosition);
				}
				out.put("user", outUser);

				result.put(out);
			}

			LOGGER.warning("fetchStandingsAsArray: Standings verarbeitet: " + result.length() + " Einträge, übersprungen: " + skipped + (log.length() > 0 ? "\n" + log.toString() : ""));
			return result;

		} catch (Exception e) {
			LOGGER.warning("fetchStandingsAsArray: Fehler beim Abrufen/Verarbeiten der Standings für Community " + community.getId() + ": " + e.toString());
			return new JSONArray();
		}

	}

	public static void getSquadForMatchday(JSONObject matchdayInfoList, JSONArray userDB, MatchdayInfo matchdayInfo) {
		for (int i = 0; i < userDB.length(); i++) {

			JSONObject array_element = (JSONObject) userDB.get(i);
			JSONObject user = array_element.getJSONObject("user");
			String userID = user.getString("id");
			if (userID != null && !userID.equals("1")) {
				fetchSquadFromStandingsAsArray(userID, matchdayInfo.getEventInfo().getPrevious());
//				map noch to json und guvken wo hin. 
			}
		}
	}

	public static Map<Position, Set<String>> fetchSquadFromStandingsAsArray(String userID, long eid) {

		String url = "https://www.comunio.de/api/users/" + userID + "/squad?communityId=" + ComunioDataUpdater.community.getId() + "&eid=" + eid + "&state=standings.mode";

		try {
			Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
			String jsonResponse = Jsoup.connect(url)//
					.userAgent(HttpHeaderUtil.getRandomUserAgent())//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Accept-Encoding", "gzip, deflate, br, zstd")//
					.header("Accept-Language", "de-DE,en-EN;q=0.9")//
					.header("x-timezone", "Europe/Berlin")//
					.ignoreContentType(true)//
					.execute()//
					.body();

			JSONObject root = new JSONObject(jsonResponse);

			Map<Position, Set<String>> result = new HashMap<Position, Set<String>>();

			if (!root.has("items")) {
				LOGGER.warning("fetchStandingsAsArray: Antwort enthält kein 'standings'-Objekt. URL: " + url);
				return result;
			}

			JSONArray items = root.getJSONArray("items");
			for (int i = 0; i < items.length(); i++) {
				JSONObject array_element = items.getJSONObject(i);
				String id = String.valueOf(array_element.getInt("id"));
				String position = String.valueOf(array_element.getString("position"));
				Position enuPos = Position.fromString(position);
				Set<String> playerIds = new HashSet<String>();
				if (result.get(enuPos) != null) {
					playerIds = result.get(enuPos);
				}
				playerIds.add(id);
				result.put(enuPos, playerIds);

			}
			System.out.println();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	/**
	 * Aktualisiert die Punkte-Historie eines Users.
	 *
	 * - Nur wenn aktueller Spieltag noch nicht eingetragen ist - Fehlende vorherige
	 * Spieltage werden ergänzt: -> Differenz aus totalPoints - Summe(Historie) -
	 * Falls Differenz < 0, werden 0 Punkte eingetragen
	 *
	 * @param dbItem        das User-DB-Objekt
	 * @param spieltag      der aktuelle Spieltag (abgeschlossen oder wertbar)
	 * @param totalPoints   Gesamtpunkte laut API
	 * @param lastPointsObj Punkte laut API für den letzten Spieltag (kann Zahl oder
	 *                      "-" sein)
	 */
	private static void updatePunkteHistorie(JSONObject dbItem, int spieltag, int totalPoints, int lastPoints, Logger LOGGER) {
		if (spieltag < 1 || spieltag > 34) {
			LOGGER.warning("Ungültiger Spieltag: " + spieltag);
			return;
		}

		// Historie laden oder neu anlegen
		JSONObject historie = dbItem.optJSONObject("punkteHistorie");
		if (historie == null) {
			historie = new JSONObject();
			dbItem.put("punkteHistorie", historie);
		}

		// Wenn Spieltag schon eingetragen, nichts tun
		if (historie.has(String.valueOf(spieltag))) {
			LOGGER.fine("Spieltag " + spieltag + " bereits in Historie -> überspringe.");
			return;
		}

		// Summe bisheriger Punkte in der Historie
		int sumHistorie = 0;
		for (String key : historie.keySet()) {
			sumHistorie += historie.optInt(key, 0);
		}

		// Vorheriger Spieltag fehlt?
		int prev = spieltag - 1;
		if (prev > 0 && !historie.has(String.valueOf(prev))) {
			int diff = totalPoints - sumHistorie - lastPoints;
			if (diff < 0)
				diff = 0;
			historie.put(String.valueOf(prev), diff);
			LOGGER.info("Fehlender Spieltag " + prev + " ergänzt mit Differenzpunkten=" + diff);
			sumHistorie += diff;
		}

		// Punkte für aktuellen Spieltag
		int calcPoints = totalPoints - sumHistorie;
		// Fallback auf lastPoints, wenn Differenz unsinnig wirkt
		if (calcPoints < 0) {
			LOGGER.warning("Berechnete Differenzpunkte < 0 für user=" + dbItem.optJSONObject("user").optString("id") + " -> nutze lastPoints=" + lastPoints);
			calcPoints = lastPoints;
		}

		historie.put(String.valueOf(spieltag), calcPoints);
		LOGGER.info("Spieltag " + spieltag + " eingetragen mit " + calcPoints + " Punkten");
	}

}
