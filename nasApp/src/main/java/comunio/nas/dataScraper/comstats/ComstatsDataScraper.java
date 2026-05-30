package comunio.nas.dataScraper.comstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken; // ← WICHTIG!

import comunio.nas.enu.Playtime; // ← Deine Enum
import comunio.nas.objects.helper.LogManager;
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

	public static void main(String[] args) {
		ComstatsDataScraper scraper = new ComstatsDataScraper();
		List<JsonObject> results = scraper.processMatchday(12);

		// Pretty Print (erste 10 Spieler)
		Gson pretty = new GsonBuilder().setPrettyPrinting().create();
		System.out.println("=== TOP 10 SPIELER SPIELTAG 19 ===");
		for (int i = 0; i < Math.min(10, results.size()); i++) {
			System.out.println(pretty.toJson(results.get(i)));
		}

		// Optional: Als Datei speichern
		// Gson.toJson(results, new FileWriter("comunio_spieltag19.json"));
	}

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
		List<JsonObject> results = scraper.processMatchday(matchday);

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.warning("Keine playerDB im playerDBObject gefunden!");
			return;
		}

		if (results != null && results.size() > 0) {

			for (int i = 0; i < results.size(); i++) {
				JsonObject spieler = results.get(i);
				JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, spieler.get("playerId").toString(), notInLigaDBObj);
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
						JSONObject spP = (JSONObject) spieltagspunkte.get(j);
						if (spP.optInt("key", 0) == matchday) {
							if (override || spP.optInt("einsatzzeit", -1) == -1) {
								spP.put("einsatzzeit", spieler.get("playtime").getAsInt());
							}

							spP.put("tore", spieler.get("goals").getAsInt());
							spP.put("status", spieler.get("status").getAsString()); // mit status ist gemeint ob voll, subin, subout
							spP.put("xgoals", spieler.get("xgoals").getAsDouble());
							spP.put("rating", spieler.get("rating").getAsDouble());
							spP.put("assists", spieler.get("assists").getAsInt());
							spP.put("yellow", spieler.get("yellow").getAsInt());
							spP.put("yellowRed", spieler.get("yellowRed").getAsInt());
							spP.put("red", spieler.get("red").getAsInt());
							spP.put("stats", spieler.get("stats").getAsJsonObject().toString());
						}
					}
					player.put("data", data);
				} else {
					LOGGER.fine("Kein Match für Spieler mit ComunioId " + spieler.get("playerId").getAsInt() + " Name: " + spieler.get("name").getAsString());
				}
			}
		}
	}

	public List<JsonObject> processMatchday(int matchday) {
		List<JsonObject> allPlayers = new ArrayList<>();

		try {
			List<Integer> matchIds = extractMatchIds(matchday);
//			System.out.println("Gefundene Matches: " + matchIds.size());

			for (int matchId : matchIds) {
				try {
					JsonObject matchJson = fetchMatchDetails(matchId);
					List<JsonObject> players = processMatchPlayers(matchJson);
					allPlayers.addAll(players);
				} catch (Exception e) {
					System.err.println("Match " + matchId + ": " + e.getMessage());
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

	private JsonObject fetchMatchDetails(int matchId) throws Exception {
		String url = String.format(MATCH_DETAILS_URL, matchId);
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("User-Agent", "Mozilla/5.0").build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		Type type = new TypeToken<JsonObject>() {
		}.getType();
		return GSON.fromJson(response.body(), type);
	}

	private List<JsonObject> processMatchPlayers(JsonObject matchJson) {
		List<JsonObject> players = new ArrayList<>();

		processPlayersList(matchJson.getAsJsonArray("homePlayers"), players);
		processPlayersList(matchJson.getAsJsonArray("awayPlayers"), players);
		return players;
	}

	private void processPlayersList(JsonArray playersArray, List<JsonObject> result) {
		for (int i = 0; i < playersArray.size(); i++) {
			JsonObject player = null;
			try {
				player = playersArray.get(i).getAsJsonObject();
				if (player.get("active").getAsInt() != 1)
					continue;

				int playerId = player.get("playerId").getAsInt();
				String name = player.get("name").getAsString();
				int goals = calculateGols(player);
				int playtime = calculatePlaytime(player);
				Playtime status = determineStatus(player);
				double xgoals = calculateXGoals(player);
				double rating = player.has("rating") && !player.get("rating").isJsonNull() ? player.get("rating").getAsDouble() : 0.0;
				int assists = player.has("assists") && !player.get("assists").isJsonNull() ? player.get("assists").getAsInt() : 0;
				int yellow = player.has("yellow") && !player.get("yellow").isJsonNull() ? player.get("yellow").getAsInt() : 0;
				int yellowRed = player.has("yellowRed") && !player.get("yellowRed").isJsonNull() ? player.get("yellowRed").getAsInt() : 0;
				int red = player.has("red") && !player.get("red").isJsonNull() ? player.get("red").getAsInt() : 0;
				JsonObject stats = player.has("stats") && !player.get("stats").isJsonNull() ? player.get("stats").getAsJsonObject() : new JsonObject();

				JsonObject playerData = new JsonObject();
				playerData.addProperty("playerId", playerId);
				playerData.addProperty("name", name);
				playerData.addProperty("playtime", playtime);
				playerData.addProperty("goals", goals);
				playerData.addProperty("status", status.name());
				playerData.addProperty("xgoals", xgoals);
				playerData.addProperty("rating", rating);
				playerData.addProperty("assists", assists);
				playerData.addProperty("yellow", yellow);
				playerData.addProperty("yellowRed", yellowRed);
				playerData.addProperty("red", red);
				playerData.add("stats", stats);

				result.add(playerData);

			} catch (Exception e) {
				System.err.println("Fehler bei Spieler: " + player.toString() + " - " + e.getMessage());
			}
		}
	}

	private double calculateXGoals(JsonObject player) {
		// Angenommen, 'player' ist dein JsonObject
		double xgoals = 0.0;

		if (player.has("xgoals") && !player.get("xgoals").isJsonNull()) {
			JsonElement xgoalsElement = player.get("xgoals");
			if (xgoalsElement.isJsonPrimitive()) {
				String xgoalsStr = xgoalsElement.getAsString().trim(); // Trim entfernt Leerzeichen
				if (!xgoalsStr.isEmpty()) {
					// Ersetze Komma durch Punkt für Java-Double-Parsing
					String normalizedStr = xgoalsStr.replace(",", ".");
					try {
						xgoals = Double.parseDouble(normalizedStr);
					} catch (NumberFormatException e) {
						// Fallback bei ungültigem Format (z.B. "abc")
						System.err.println("Ungültiges xGoals-Format: " + xgoalsStr + " -> Verwende 0.0");
						xgoals = 0.0;
					}
				}
			}
		}
		return xgoals;

	}

	private int calculateGols(JsonObject player) {
		if (player.has("pens") && !player.get("pens").isJsonNull()) {
			if (player.get("pens").getAsInt() > 0) {
				return player.get("goals").getAsInt() + player.get("pens").getAsInt() - player.get("pensMissed").getAsInt();
			}
			return player.get("goals").getAsInt();
		}

		return player.get("goals").getAsInt();
	}

	private int calculatePlaytime(JsonObject player) {
		Integer subIn = getIntOrNull(player, "subIn");
		Integer subOut = getIntOrNull(player, "subOut");
		if (subIn != null)
			return 90 - subIn;
		if (subOut != null)
			return subOut;
		return 90;
	}

	private Playtime determineStatus(JsonObject player) {
		Integer subIn = getIntOrNull(player, "subIn");
		Integer subOut = getIntOrNull(player, "subOut");
		if (subIn != null)
			return Playtime.SUBIN;
		if (subOut != null)
			return Playtime.SUBOUT;
		return Playtime.FULL;
	}

	private Integer getIntOrNull(JsonObject obj, String key) {
		return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : null;
	}

}