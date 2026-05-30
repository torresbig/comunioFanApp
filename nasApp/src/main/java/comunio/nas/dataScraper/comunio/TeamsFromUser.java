package comunio.nas.dataScraper.comunio;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.enu.Position;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.player.PlayerHelper;

public class TeamsFromUser {

	// https://www.comunio.de/api/users/5981249/squad?state=infoUser

	private static final Logger LOGGER = LogManager.getLogger(PlayerUpdater.class);

	public static void loadAllPlayerForUser(JSONObject playerDBObject, JSONArray userDB, JSONArray marketValueDB, Map<String, String> playerToUserDB, NewsManager newsManager, JSONObject notInligaDBObj, LastUpdates lastUpdates) {
		for (Object obj : userDB) {
			JSONObject userObj = (JSONObject) obj;
			if (userObj.has("user")) {
				JSONObject user = userObj.getJSONObject("user");
				if (user.has("id")) {
					mergePlayerDataWithDB(playerDBObject, marketValueDB, newsManager, userObj, playerToUserDB, notInligaDBObj, lastUpdates);
				}
			}
		}

		// Nach Verarbeitung der User-Spieler die Computer-Spieler
		// aktualisieren/markieren
		updateComputerPlayersWithMap(playerDBObject, playerToUserDB, newsManager);
	}

	private static JSONArray fetchlPlayerForUserJson(JSONObject userObj) {
		try {
			JSONObject user = userObj.optJSONObject("user", new JSONObject());
			if (user == null) {
				return new JSONArray();
			}
			String userID = user.optString("id", null);
			if (userID == null || userID.equals("1")) {
				return new JSONArray();
			}

			String url = "https://www.comunio.de/api/users/" + userID + "/squad?state=infoUserd";
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

			JSONObject playerData = new JSONObject(jsonResponse);
			JSONArray items = new JSONArray(playerData.getJSONArray("items"));
			JSONArray result = new JSONArray();

			String tactic = String.valueOf(playerData.optInt("tactic", 0));
			userObj.put("tactic", tactic);

			for (int i = 0; i < items.length(); i++) {
				JSONObject playerObj = items.getJSONObject(i);
				JSONObject resultObject = new JSONObject();
				int id = playerObj.getInt("id");
				if (id > 0) {
					resultObject.put("id", String.valueOf(id));
					String name = playerObj.optString("name", null);
					if(name != null) {
						name = name.trim(); // HTML-Entitäten entfernen
					}
					resultObject.put("name", name);
					JSONObject tempObj = playerObj.optJSONObject("club");
					if (tempObj != null) {
						resultObject.put("clubID", tempObj.optString("id"));
					}
					resultObject.put("withinSquad", playerObj.optBoolean("withinSquad", false));
					resultObject.put("motm", playerObj.optBoolean("motm", false));
					resultObject.put("lastPoints", playerObj.optInt("lastPoints", 0));
//					resultObject.put("matchdayPoints", playerObj.optInt("matchdayPoints", 0));
					resultObject.put("onMarket", playerObj.optBoolean("points", false));
					resultObject.put("quotedprice", playerObj.optInt("quotedprice", 0));

					resultObject.put("punkte", playerObj.optInt("points", 0));
					resultObject.put("position", playerObj.optString("position", "UNBESTIMMT"));
					result.put(playerObj);
				} else {
					LOGGER.warning("Spieler hat keine ID! ");
				}
			}
			return result;
		} catch (Exception e) {
			LOGGER.warning("" + e.getMessage());
			return null;
		}

	}

	public static Set<String> playerWithOwner = new HashSet<String>();

	public static void mergePlayerDataWithDB(JSONObject playerDBObject, JSONArray marketValueDB, NewsManager newsManager, JSONObject userObj, Map<String, String> playerToUserDB, JSONObject notInligaDBObj, LastUpdates lastUpdates) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");

		JSONObject user = userObj.optJSONObject("user", new JSONObject());
		if (user == null) {
			user = new JSONObject();
		}
		String userID = user.optString("id", null);
		if (userID == null || userID.equals("1")) {
			return;
		}
		JSONArray apiPlayerArray = fetchlPlayerForUserJson(userObj);

		try {
			if (playerDB != null && apiPlayerArray != null && apiPlayerArray.length() > 0) {
				for (Object obj : apiPlayerArray) {
					JSONObject apiJObj = (JSONObject) obj;
					String id = apiJObj.optString("id", null);
					String name = apiJObj.optString("name", null);

					if (id == null) {
						LOGGER.warning("Spieler ohne ID gefunden, wird übersprungen.");
						continue;
					}
					if (name == null) {
						LOGGER.warning("Spieler ohne NAME gefunden, wird übersprungen. ID: " + id);
						continue;
					}

					playerToUserDB.put(id, userID);
					JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, id, notInligaDBObj);
					if(player == null) {
						LOGGER.warning("Spieler mit ID " + id + " und Name " + name + " konnte in der DB nicht gefunden werden.");
						player = PlayerHelper.createNewEmptyPlayerJSON(id, name);
					}
					JSONObject data = player.optJSONObject("data");

					if (player == null || data == null) {
						player = PlayerHelper.createNewEmptyPlayerJSON(userID, name);
						data = player.optJSONObject("data");
						LOGGER.warning("Player war null. nicht vorhanden in der DB! wird neu angelegt. " + player.optString("name") + " (ID: " + player.optString("id") + ")");
					}

					int marktwert = apiJObj.optInt("quotedprice", 0);
					if (apiJObj.has("quotedprice")) {
						PlayerUpdater.updateMarketValueData(apiJObj, marketValueDB);
						data.put("wert", marktwert);

					}

					// ownerchange. wenn er aufeinmal bei einem anderen owener ist.
					if (playerToUserDB.containsKey(id)) {
						String oldUserID = playerToUserDB.get(id);
						playerToUserDB.remove(id);
						playerToUserDB.put(id, userID);
						GitHubUploader.mappingChanged = true; 
						if (!oldUserID.equals(userID)) {
							News news = News.getOwnerwechsel(oldUserID, userID, marktwert, id, name);
							newsManager.addNews(news, true);
						}
					} else {
						playerToUserDB.put(id, userID);
						GitHubUploader.mappingChanged = true; 
						LOGGER.info("Spieler " + name + " war noch keinem Spieler zugeordnet");
					}

					// um später abzugleichen, welcher spieler nicht bei einem spieler sondern beim
					// computer sind.
					playerWithOwner.add(id);

					// Prüfe und setze jedes Feld einzeln
					// Beispiel: withinSquad (boolean)
					if (apiJObj.has("withinSquad")) {
						data.put("withinSquad", apiJObj.optBoolean("withinSquad", false));
					}

					// motm (boolean)
//					if (apiJObj.has("motm")) {
//						boolean motm =  apiJObj.optBoolean("motm", false);
//						if(true) {
//							stats.setManOfTheMatchAmount(stats.getManOfTheMatchAmount() + 1);
//							// TODO: so wird es bei jeder abfrage hochgezhählt man müsste einbauen, dass der spieltag dabei steht. also eine map
//						}
//					}

					// ------------------POINTS---------------------

					// points (int)
					if (apiJObj.has("points")) {
						data.put("punkte", apiJObj.optInt("points", 0));
					}
					// points (int)
					if (apiJObj.has("lastPoints")) {
						data.put("lastPoints", apiJObj.optInt("lastPoints", 0));
					}
					PlayerUpdater.updateSpielerPointsFromJson(playerDBObject, player.optJSONObject("data", new JSONObject()), apiJObj, lastUpdates);

//					if (apiJObj.has("matchdayPoints")) {
//						data.put("matchdayPoints", apiJObj.optInt("matchdayPoints", 0));
//					}

					// ---------------------------------------

					// matchdayPoints (int)
					// TODO: wofpr sind die=

					// onMarket (boolean)
					if (apiJObj.has("onMarket")) {
						data.put("onMarket", apiJObj.optBoolean("onMarket", false));
					}

					// position (String)
					if (apiJObj.has("position")) {
						String position = apiJObj.optString("position", "");

						String oldPosition = data.optString("position", "UNBEKANNT");
						if (position != null && !position.isBlank()) {
							position = Position.fromString(position).toString();

							if (!oldPosition.equals(position)) {
								News news = News.getPositionswechsel(oldPosition, position.toString(), id, name);
								newsManager.addNews(news, true);
								data.put("position", position.toString());
							}

						}
					}

					// clubID (String), verschachteltes Objekt "club"
					if (apiJObj.has("club")) {
						JSONObject club = apiJObj.optJSONObject("club");
						if (club != null) {
							String clubId = club.optString("id", null);
							String oldClubId = data.optString("verein", "0");

							if (clubId != null && !clubId.isBlank()) {
								if (!clubId.equals(oldClubId)) {
									if (!oldClubId.equals("0")) {
										data.put("clubID", clubId);

										News news = News.getVereinswechsel(oldClubId, clubId, id, name);
										newsManager.addNews(news, true);
									}

								}

							}
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Markiert alle Spieler aus playerDB im playerDBObject, die keinem echten User
	 * zugeordnet sind (d.h. deren User-ID in playerToUserMap entweder "1" oder
	 * nicht vorhanden ist), als Computer-Spieler. Das "owner"-Attribut wird auf
	 * "computer" gesetzt.
	 *
	 * @param playerDBObject  JSON-Objekt mit dem Array "playerDB" aller Spieler
	 * @param playerToUserMap Map mit Zuordnungen {playerId -> userId}
	 */
	public static void updateComputerPlayersWithMap(JSONObject playerDBObject, Map<String, String> playerToUserMap, NewsManager newsManager) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.warning("Spieler-Datenbank (playerDB) ist leer oder null.");
			return;
		}
		int changes = 0;
		for (int i = 0; i < playerDB.length(); ++i) {
			JSONObject spieler = playerDB.getJSONObject(i);
			String id = spieler.optString("id", null);
			if (id != null) {
				if (!playerWithOwner.contains(id)) {
					String userIDFromMapping = playerToUserMap.getOrDefault(id, "UNBEKANNT");
					if (userIDFromMapping.equals("1") || userIDFromMapping.equals("0")) {
						continue;
					}
					playerToUserMap.remove(id);
					playerToUserMap.put(id, "1");
					
					News news = News.getOwnerwechsel(userIDFromMapping, "1", 0, id, spieler.optString("name", ""));
					newsManager.addNews(news, true);
					
					changes++;
					LOGGER.info("Spieler wurde dem Computer zugeordnet, da er in Comunio bei keinem spieler ist.");
				}
			}
		}
		StringBuilder nachricht = new StringBuilder();
		nachricht.append("Spielerdatenbank wurde überprüft \n");
		nachricht.append(changes + " von " + playerDB.length() + " Spielern wurden dem Computer zugeordnet");
		LOGGER.info(nachricht.toString());
	}

}
