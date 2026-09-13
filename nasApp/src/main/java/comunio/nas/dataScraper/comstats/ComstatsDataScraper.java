package comunio.nas.dataScraper.comstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken; // ← WICHTIG!
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
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class ComstatsDataScraper {

	/**
	 * Logger-Instanz für diese Klasse zur Protokollierung.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ComstatsDataScraper.class);

	private static final String MATCHDAY_URL = "https://stats.comunio.de/matchday/2025-26/";
	private static final String MATCH_DETAILS_URL = "https://stats.comunio.de/xhr/matchDetails.php?mid=%d";
	private static final Gson GSON = new GsonBuilder().create();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	public static void getPlaytimeForNewMatchdays(int maxSpieltag, JSONObject playerDBObject, JSONObject notInLigaDBObj) {
		int lastMatchday = playerDBObject.optInt("lastProcessedComstatsSpieltagsdaten", 1);
		for (int matchday = lastMatchday; matchday <= maxSpieltag; matchday++) {
			if (matchday == 0) {
				continue;
			}
			getPlaytimeForMatchdays(matchday, playerDBObject, notInLigaDBObj, true);
		}
		playerDBObject.put("lastProcessedComstatsSpieltagsdaten", maxSpieltag);

	}

	public static void getPlaytimeForInputToInput(int start, int maxSpieltag, JSONObject playerDBObject, JSONObject notInLigaDBObj) {
		for (int matchday = start; matchday <= maxSpieltag; matchday++) {
			getPlaytimeForMatchdays(matchday, playerDBObject, notInLigaDBObj, false);
		}

	}

	public static void getPlaytimeForMatchdays(int matchday, JSONObject playerDBObject, JSONObject notInLigaDBObj, boolean override) {
		ComstatsDataScraper scraper = new ComstatsDataScraper();
		List<JSONObject> results = scraper.processMatchday(matchday);

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

	public List<JSONObject> processMatchday(int matchday) {
		List<JSONObject> allPlayers = new ArrayList<>();

		try {
			List<Integer> matchIds = extractMatchIds(matchday);
//			System.out.println("Gefundene Matches: " + matchIds.size());

			for (int matchId : matchIds) {
				try {
					JSONObject matchJson = fetchMatchDetails(matchId);
					List<JSONObject> players = processMatchPlayers(matchJson);
					allPlayers.addAll(players);
				} catch (Exception e) {
					System.err.println("Match " + matchId + ": " + e.getMessage());
					// Continue processing other matches even if one fails
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println("Gesamt: " + allPlayers.size() + " Spieler");
		return allPlayers;
	}

	private List<Integer> extractMatchIds(int matchday) throws Exception {
		List<Integer> matchIds = new ArrayList<>();
		Document doc = Jsoup.connect(MATCHDAY_URL + matchday).get();
		Elements matchTitles = doc.select("div[id^=matchTitle_]");

		for (Element title : matchTitles) {
			String idAttr = title.id();
			String numStr = idAttr.replace("matchTitle_", "");
			matchIds.add(Integer.parseInt(numStr));
		}
		return matchIds;
	}

	private JSONObject fetchMatchDetails(int matchId) throws Exception {
		String url = String.format(MATCH_DETAILS_URL, matchId);
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("User-Agent", "Mozilla/5.0").build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JSONObject result = new JSONObject(response.body().toString());
		return result;
	}

	private List<JSONObject> processMatchPlayers(JSONObject matchJson) {
		List<JSONObject> players = new ArrayList<>();

		processPlayersList(matchJson.getJSONArray("homePlayers"), players);
		processPlayersList(matchJson.getJSONArray("awayPlayers"), players);
		return players;
	}

	private void processPlayersList(JSONArray playersArray, List<JSONObject> result) {
		for (int i = 0; i < playersArray.length(); i++) {
			JSONObject player = null;
			try {
				player = playersArray.getJSONObject(i);
				if (player.getInt("active") != 1) {
					continue;
				}
				int playerId = player.getInt("playerId");
				String name = player.getString("name");
				int goals = calculateGols(player);
				int playtime = calculatePlaytime(player);
				Playtime status = determineStatus(player);
				double xgoals = calculateXGoals(player);
				double rating = player.has("rating") && !player.isNull("rating") ? player.optDouble("rating", 0.0) : 0.0;
				int assists = player.has("assists") && !player.isNull("assists") ? player.getInt("assists") : 0;
				int yellow = player.has("yellow") && !player.isNull("yellow") ? player.getInt("yellow") : 0;
				int yellowRed = player.has("yellowRed") && !player.isNull("yellowRed") ? player.getInt("yellowRed") : 0;
				int red = player.has("red") && !player.isNull("red") ? player.getInt("red") : 0;
				JSONObject stats = player.has("stats") && !player.isNull("stats") ? player.getJSONObject("stats") : new JSONObject();

				int ownGoals = player.has("ownGoals") && !player.isNull("ownGoals") ? player.getInt("ownGoals") : 0;
				int pens = player.has("pens") && !player.isNull("pens") ? player.getInt("pens") : 0;
				int pensSaved = player.has("pensSaved") && !player.isNull("pensSaved") ? player.getInt("pensSaved") : 0;
				int pensMissed = player.has("pensMissed") && !player.isNull("pensMissed") ? player.getInt("pensMissed") : 0;
				int points = player.has("points") && !player.isNull("points") ? player.getInt("points") : 0;
				int subIn = player.has("subIn") && !player.isNull("subIn") ? player.getInt("subIn") : 0;
				int subOut = player.has("subOut") && !player.isNull("subOut") ? player.getInt("subOut") : 0;
				int motm = player.has("motm") && !player.isNull("motm") ? player.getInt("motm") : 0;
				int cleanSheet = player.has("cleanSheet") && !player.isNull("cleanSheet") ? player.getInt("cleanSheet") : 0;
				int active = player.has("active") && !player.isNull("active") ? player.getInt("active") : 0;

				JSONObject playerData = new JSONObject();
				playerData.put("playerId", playerId);
				playerData.put("name", name);
				playerData.put("playtime", playtime);
				playerData.put("goals", goals);
				playerData.put("status", status.name());
				playerData.put("xgoals", xgoals);
				playerData.put("rating", rating);
				playerData.put("assists", assists);
				playerData.put("yellow", yellow);
				playerData.put("yellowRed", yellowRed);
				playerData.put("red", red);
				playerData.put("stats", stats);
				playerData.put("ownGoals", ownGoals);
				playerData.put("pens", pens);
				playerData.put("pensSaved", pensSaved);
				playerData.put("pensMissed", pensMissed);
				playerData.put("points", points);
				playerData.put("subIn", subIn);
				playerData.put("subOut", subOut);
				playerData.put("motm", motm);
				playerData.put("cleanSheet", cleanSheet);
				playerData.put("active", active);

				result.add(playerData);

			} catch (Exception e) {
				System.err.println("Fehler bei Spieler: " + player.toString() + " - " + e.getMessage());
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
						System.err.println("Ungültiges xGoals-Format: " + xgoalsStr + " -> Verwende 0.0");
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
		Integer subIn = getIntOrNull(player, "subIn");
		Integer subOut = getIntOrNull(player, "subOut");
		if (subIn != null)
			return 90 - subIn;
		if (subOut != null)
			return subOut;
		return 90;
	}

	private Playtime determineStatus(JSONObject player) {
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

	// Hilfsmethode: Spielerstats-Objekt aus Comstats-JSON bauen
	private static Spielerstats buildSpielerstats(JSONObject spieler) {
		Spielerstats stats = new Spielerstats();
		stats.setEinsatzzeit(spieler.optInt("playtime", -1));
		stats.setTore(spieler.optInt("goals", 0));
		stats.setStatus(spieler.optString("status", null));
		stats.setXgoals(spieler.optDouble("xgoals", 0.0));
		stats.setRating(spieler.optDouble("rating", 0.0));
		stats.setGoalAssists(spieler.optInt("assists", 0));
		stats.setGelbekarten(spieler.optInt("yellow", 0));
		stats.setGelbrotekarten(spieler.optInt("yellowRed", 0));
		stats.setRotekarten(spieler.optInt("red", 0));
		stats.setOwnGoals(spieler.optInt("ownGoals", 0));
		stats.setTotalPenalties(spieler.optInt("pens", 0));
		stats.setPensSaved(spieler.optInt("pensSaved", 0));
		stats.setPensMissed(spieler.optInt("pensMissed", 0));
		stats.setPoints(spieler.optInt("points", 0));
		stats.setSubIns(spieler.optInt("subIn", 0));
		stats.setSubOut(spieler.optInt("subOut", 0));
		stats.setManOfTheMatchAmount(spieler.optInt("motm", 0));
		stats.setCleanSheet(spieler.optInt("cleanSheet", 0));
		stats.setActive(spieler.optInt("active", 0));
		return stats;
	}
}