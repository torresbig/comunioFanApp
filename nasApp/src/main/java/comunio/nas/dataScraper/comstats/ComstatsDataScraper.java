package comunio.nas.dataScraper.comstats;

import comunio.nas.dataVariable.Dates;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.Playtime; // ← Deine Enum
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.player.Spielerstats;
import comunio.nas.util.player.PlayerHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ComstatsDataScraper {

	/**
	 * Logger-Instanz für diese Klasse zur Protokollierung.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ComstatsDataScraper.class);

	private final HttpClient httpClient = HttpClient.newHttpClient();

	public static void getPlaytimeForNewMatchdays(int maxSpieltag, JSONObject playerDBObject, JSONObject notInLigaDBObj, LastUpdates lastUpdates) {
		int lastMatchday = playerDBObject.optInt("lastProcessedComstatsSpieltagsdaten", 1);
		for (int matchday = lastMatchday; matchday <= maxSpieltag; matchday++) {
			if (matchday == 0) {
				continue;
			}
			getPlaytimeForMatchdays(matchday, playerDBObject, notInLigaDBObj, true, lastUpdates);
		}
		playerDBObject.put("lastProcessedComstatsSpieltagsdaten", maxSpieltag);

	}

	public static void getPlaytimeForInputToInput(int start, int maxSpieltag, JSONObject playerDBObject, JSONObject notInLigaDBObj, LastUpdates lastUpdates) {
		for (int matchday = start; matchday <= maxSpieltag; matchday++) {
			getPlaytimeForMatchdays(matchday, playerDBObject, notInLigaDBObj, false, lastUpdates);
		}

	}

	public static void getPlaytimeForMatchdays(int matchday, JSONObject playerDBObject, JSONObject notInLigaDBObj, boolean override, LastUpdates lastUpdates) {
		ComstatsDataScraper scraper = new ComstatsDataScraper();
		List<JSONObject> results = scraper.processMatchday(lastUpdates, matchday);

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.warning("Keine playerDB im playerDBObject gefunden!");
			return;
		}

		if (results != null && results.size() > 0) {

			for (int i = 0; i < results.size(); i++) {
				JSONObject spieler = results.get(i);
				String playerId = PlayerHelper.convertIdToString(spieler.get("playerId"));

				JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, playerId, notInLigaDBObj);
				if (player != null) {

					JSONObject data = player.optJSONObject("data");
					if (data == null) {
						data = new JSONObject();
					}

					JSONArray spieltagspunkte = data.optJSONArray("spieltagspunkte");
					if (spieltagspunkte == null) {
						spieltagspunkte = new JSONArray();
					}

					for (int j = 0; j < spieltagspunkte.length(); j++) {
						JSONObject spP = spieltagspunkte.getJSONObject(j);
						if (spP.optInt("key", 0) == matchday) {

							// Spielerstats als Transportobjekt bauen (Mapping der Comstats-Felder)
							Spielerstats stats = buildSpielerstats(spieler);
							stats.setLastUpdate(null); // lastUpdate ist ein Saison-Feld, gehört nicht in den Spieltag-Eintrag

							// Alle Felder flach in den Spieltag-Eintrag schreiben (kein stats-Unterobjekt)
							JSONObject statsJson = stats.toJSON();
							Iterator<String> keys = statsJson.keys();
							while (keys.hasNext()) {
								String key = keys.next();
								// einsatzzeit nur überschreiben, wenn override oder noch nicht gesetzt
								if ("einsatzzeit".equals(key) && !(override || spP.optInt("einsatzzeit", -1) == -1)) {
									continue;
								}
								spP.put(key, statsJson.get(key));
							}
						}
					}
					player.put("data", data);
				} else {
					LOGGER.fine("Kein Match für Spieler mit ComunioId " + spieler.getInt("playerId") + " Name: " + spieler.getString("name"));
				}
			}
		}
	}

	public List<JSONObject> processMatchday(LastUpdates lastUpdates, int matchday) {
		List<JSONObject> allPlayers = new ArrayList<>();

		try {
			Set<Integer> matchIds = extractMatchIds(lastUpdates, matchday);
//			System.out.println("Gefundene Matches: " + matchIds.size());

			for (int matchId : matchIds) {
				try {
					JSONObject matchJson = fetchMatchDetails(matchId);

					List<JSONObject> players = processMatchPlayers(matchJson);
					allPlayers.addAll(players);
				} catch (Exception e) {
					LOGGER.warning("Match " + matchId + ": " + e.getMessage());
					// Continue processing other matches even if one fails
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println("Gesamt: " + allPlayers.size() + " Spieler");
		return allPlayers;
	}

	private Set<Integer> extractMatchIds(LastUpdates lastUpdate, int matchday) throws Exception {
		Set<Integer> matchIds = new HashSet<>();

		String url = Urls.MATCHDAY_URL(Dates.calculateTargetYear(lastUpdate.getSeasonStart()), matchday);
		Document doc = Jsoup.connect(url).get();
		Elements matchTitles = doc.select("div[id^=matchTitle_]");

		for (Element title : matchTitles) {
			String idAttr = title.id();
			String numStr = idAttr.replace("matchTitle_", "");
			matchIds.add(Integer.parseInt(numStr));
		}
		return matchIds;
	}

	private JSONObject fetchMatchDetails(int matchId) throws Exception {
		String url = String.format(Urls.MATCH_DETAILS_URL(matchId));
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("User-Agent", "Mozilla/5.0").build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JSONObject result = new JSONObject(response.body().toString());
		return result;
	}

	private List<JSONObject> processMatchPlayers(JSONObject matchJson) {
		List<JSONObject> players = new ArrayList<>();

		processPlayersList(matchJson.getJSONArray("homePlayers"), players);
		processPlayersList(matchJson.getJSONArray("awayPlayers"), players);
		prozessGoalieList(matchJson, players);
		return players;
	}

	private void prozessGoalieList(JSONObject matchJson, List<JSONObject> result) {
		JSONArray goalPlayersArray = matchJson.getJSONArray("goals");
		if (goalPlayersArray.isEmpty()) {
			return;
		}

		for (int i = 0; i < goalPlayersArray.length(); i++) {
			JSONObject player = null;
			try {
				player = goalPlayersArray.getJSONObject(i);
				String name = player.has("name") && !player.isNull("name") ? player.getString("name") : null;
				Integer clubId = player.has("clubId") && !player.isNull("clubId") ? player.getInt("clubId") : null;

				if (name == null || clubId == null) {
					continue;
				}

				String assists = player.has("nameAssist") && !player.isNull("nameAssist") ? player.getString("nameAssist") : null;
				Integer pens = player.has("pen") && !player.isNull("pen") ? player.optInt("pen") : null;
				Integer ownGoals = player.has("og") && !player.isNull("og") ? player.optInt("og") : null;

				for (int j = 0; j < result.size(); j++) {
					JSONObject array_element = result.get(j);
					if (array_element.has("name")) {
						String arrayName = array_element.getString("name");
						if (arrayName.equals(name)) {
							if (pens != null && pens > 0) {
								int pensInt = array_element.has("pens") && !array_element.isNull("pens") ? array_element.optInt("pens") : 0;
								pensInt++;
								array_element.put("pens", pensInt);
							}
							if (ownGoals != null && ownGoals > 0) {
								int ogInt = array_element.has("ownGoals") && !array_element.isNull("ownGoals") ? array_element.optInt("ownGoals") : 0;
								ogInt++;
								array_element.put("ownGoals", ogInt);
							}
						}

						if (assists != null && assists.equals(arrayName)) {
							int assi = array_element.has("assists") && !array_element.isNull("assists") ? array_element.optInt("assists") : 0;
							assi++;
							array_element.put("assists", assi);

						}
					}
				}

			} catch (Exception e) {
				LOGGER.warning("Fehler bei Spieler: " + player + " - " + e.getMessage());
			}
		}

	}

	private void processPlayersList(JSONArray playersArray, List<JSONObject> result) {
		for (int i = 0; i < playersArray.length(); i++) {
			JSONObject player = null;
			try {
				player = playersArray.getJSONObject(i);
				// Skippe nur Spieler, die komplett inaktiv sind (active == 0)
				// Spieler mit active=-2, active=1, etc. werden verarbeitet
				int activeValue = player.optInt("active", -2);
				if (activeValue != 1) {
					continue;
				}
				int playerId = player.getInt("playerId");

				// ... restlicher Code unverändert ...

				String name = player.getString("name");
				int goals = calculateGols(player);
				int playtime = calculatePlaytime(player);
				Playtime status = determineStatus(player);
				Double xgoals = calculateXGoals(player); // Double statt double!
				Double rating = player.has("rating") && !player.isNull("rating") ? player.optDouble("rating") : null;
				Integer assists = player.has("assists") && !player.isNull("assists") ? player.optInt("assists") : null;
				Integer yellow = player.has("yellow") && !player.isNull("yellow") ? player.optInt("yellow") : null;
				Integer yellowRed = player.has("yellowRed") && !player.isNull("yellowRed") ? player.optInt("yellowRed") : null;
				Integer red = player.has("red") && !player.isNull("red") ? player.optInt("red") : null;
				JSONObject stats = player.has("stats") && !player.isNull("stats") ? player.getJSONObject("stats") : new JSONObject();

				Integer ownGoals = player.has("ownGoals") && !player.isNull("ownGoals") ? player.optInt("ownGoals") : null;
				Integer pens = player.has("pens") && !player.isNull("pens") ? player.optInt("pens") : null;
				Integer pensSaved = player.has("pensSaved") && !player.isNull("pensSaved") ? player.optInt("pensSaved") : null;
				Integer pensMissed = player.has("pensMissed") && !player.isNull("pensMissed") ? player.optInt("pensMissed") : null;
				Integer points = player.has("points") && !player.isNull("points") ? player.optInt("points") : null;
				Integer subIn = player.has("subIn") && !player.isNull("subIn") ? player.optInt("subIn") : null;
				Integer subOut = player.has("subOut") && !player.isNull("subOut") ? player.optInt("subOut") : null;
				Integer motm = player.has("motm") && !player.isNull("motm") ? player.optInt("motm") : null;
				Integer cleanSheet = player.has("cleanSheet") && !player.isNull("cleanSheet") ? player.optInt("cleanSheet") : null;
				Integer active = player.has("active") && !player.isNull("active") ? player.optInt("active") : null;

				// ... restlicher Code unverändert ...

				JSONObject playerData = new JSONObject();
				playerData.put("playerId", playerId);
				playerData.put("name", name);
				playerData.put("playtime", playtime);
				playerData.put("goals", goals);
				playerData.put("status", status.name());

				if (xgoals != null)
					playerData.put("xgoals", xgoals);
				if (rating != null)
					playerData.put("rating", rating);
				if (assists != null)
					playerData.put("assists", assists);
				if (yellow != null)
					playerData.put("yellow", yellow);
				if (yellowRed != null)
					playerData.put("yellowRed", yellowRed);
				if (red != null)
					playerData.put("red", red);
				playerData.put("stats", stats);
				if (ownGoals != null)
					playerData.put("ownGoals", ownGoals);
				if (pens != null)
					playerData.put("pens", pens);
				if (pensSaved != null)
					playerData.put("pensSaved", pensSaved);
				if (pensMissed != null)
					playerData.put("pensMissed", pensMissed);
				if (points != null)
					playerData.put("points", points);
				if (subIn != null)
					playerData.put("subIn", subIn);
				if (subOut != null)
					playerData.put("subOut", subOut);
				if (motm != null)
					playerData.put("motm", motm);
				if (cleanSheet != null)
					playerData.put("cleanSheet", cleanSheet);
				if (active != null)
					playerData.put("active", active);

				result.add(playerData);

			} catch (Exception e) {
				LOGGER.warning("Fehler bei Spieler: " + player.toString() + " - " + e.getMessage());
			}
		}
	}

	private double calculateXGoals(JSONObject player) {
		double xgoals = 0.0;
		if (player.has("xgoals") && !player.isNull("xgoals")) {
			Object xgoalsObj = player.get("xgoals");
			if (xgoalsObj instanceof Number) {
				xgoals = ((Number) xgoalsObj).doubleValue();
			} else if (xgoalsObj instanceof String) {
				String xgoalsStr = ((String) xgoalsObj).trim();
				if (!xgoalsStr.isEmpty()) {
					String normalizedStr = xgoalsStr.replace(",", ".");
					try {
						xgoals = Double.parseDouble(normalizedStr);
					} catch (NumberFormatException e) {
						LOGGER.warning("Ungültiges xGoals-Format: " + xgoalsStr + " -> Verwende 0.0");
						xgoals = 0.0;
					}
				}
			}
		}
		return xgoals;
	}

	private int calculateGols(JSONObject player) {
		if (player.has("pens") && !player.isNull("pens")) {
			if (player.getInt("pens") > 0) {
				return player.getInt("goals") + player.getInt("pens") - player.getInt("pensMissed");
			}
			return player.getInt("goals");
		}

		return player.getInt("goals");
	}

	private int calculatePlaytime(JSONObject player) {
		int active = player.optInt("active", -2);
		// Wenn Spieler nicht aktiv (active != 1), dann 0 Minuten
		if (active != 1) {
			return 0;
		}
		Integer subIn = getIntOrNull(player, "subIn");
		Integer subOut = getIntOrNull(player, "subOut");
		if (subIn != null)
			return 90 - subIn;
		if (subOut != null)
			return subOut;
		return 90;
	}

	private Playtime determineStatus(JSONObject player) {
		int active = player.optInt("active", -2);
		// Wenn Spieler nicht aktiv, dann NONE
		if (active != 1) {
			return Playtime.NONE;
		}
		Integer subIn = getIntOrNull(player, "subIn");
		Integer subOut = getIntOrNull(player, "subOut");
		if (subIn != null)
			return Playtime.SUBIN;
		if (subOut != null)
			return Playtime.SUBOUT;
		return Playtime.FULL;
	}

	private Integer getIntOrNull(JSONObject obj, String key) {
		return obj.has(key) && !obj.isNull(key) ? obj.getInt(key) : null;
	}

	private static Spielerstats buildSpielerstats(JSONObject spieler) {
		Spielerstats stats = new Spielerstats();

		setIntIfPresent(spieler, "playtime", stats::setEinsatzzeit);
		setIntIfPresent(spieler, "goals", stats::setTore);

		int active = spieler.has("active") && !spieler.isNull("active") ? spieler.optInt("active") : -2;
		if (active != 1) {
			stats.setStatus("NONE");
		} else if (spieler.has("status") && !spieler.isNull("status")) {
			stats.setStatus(spieler.optString("status"));
		}

		setDoubleIfPresent(spieler, "xgoals", stats::setXgoals);
		setDoubleIfPresent(spieler, "rating", stats::setRating);
		setIntIfPresent(spieler, "assists", stats::setGoalAssists);
		setIntIfPresent(spieler, "yellow", stats::setGelbekarten);
		setIntIfPresent(spieler, "yellowRed", stats::setGelbrotekarten);
		setIntIfPresent(spieler, "red", stats::setRotekarten);
		setIntIfPresent(spieler, "ownGoals", stats::setOwnGoals);
		setIntIfPresent(spieler, "pens", stats::setTotalPenalties);
		setIntIfPresent(spieler, "pensSaved", stats::setPensSaved);
		setIntIfPresent(spieler, "pensMissed", stats::setPensMissed);
		setIntIfPresent(spieler, "points", stats::setPoints);
		setIntIfPresent(spieler, "subIn", stats::setSubIn);
		setIntIfPresent(spieler, "subOut", stats::setSubOut);
		setIntIfPresent(spieler, "motm", stats::setManOfTheMatchAmount);
		setIntIfPresent(spieler, "cleanSheet", stats::setCleanSheet);
		setIntIfPresent(spieler, "active", stats::setActive);

		return stats;
	}

	private static void setIntIfPresent(JSONObject json, String key, java.util.function.IntConsumer setter) {
		if (json.has(key) && !json.isNull(key)) {
			setter.accept(json.optInt(key));
		}
	}

	private static void setDoubleIfPresent(JSONObject json, String key, java.util.function.DoubleConsumer setter) {
		if (json.has(key) && !json.isNull(key)) {
			Object value = json.get(key);
			if (value instanceof Number) {
				setter.accept(((Number) value).doubleValue());
			} else if (value instanceof String) {
				String normalized = ((String) value).trim().replace(",", ".");
				try {
					setter.accept(Double.parseDouble(normalized));
				} catch (NumberFormatException ignored) {
				}
			}
		}
	}

}