package comunio.nas.dataScraper.comAnalystics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.ZahlenParser;

public class ComAnalysticsUpdater {

	public static void main(String[] args) {
		
		// Wird hier nicht eingebaut, da die abfrage täglich zu oft ist. 
		// wird über die App oder handyapp gemacht und dann mit datum markiert. 
		fetchTopRankingForAllPlayers();
		fetchPredictonForAllPlayers();
		fetchPlayerData("34014");
	}

	private static final Logger LOGGER = LogManager.getLogger(ComAnalysticsUpdater.class);

	public static Map<String, JSONObject> fetchPredictonForAllPlayers() {
		// Name Position Verein Marktwert Punkte am letzten Spieltag Worst case1 Neutral
		// case2 Best case3 Zielwert
		String link = "https://www.com-analytics.de/ajax/predictions?draw=2&columns%5B0%5D%5Bdata%5D=0&columns%5B0%5D%5Bname%5D=player_name&columns%5B0%5D%5Bsearchable%5D=true&columns%5B0%5D%5Borderable%5D=true&columns%5B0%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B0%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B1%5D%5Bdata%5D=1&columns%5B1%5D%5Bname%5D=position&columns%5B1%5D%5Bsearchable%5D=false&columns%5B1%5D%5Borderable%5D=true&columns%5B1%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B1%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B2%5D%5Bdata%5D=2&columns%5B2%5D%5Bname%5D=team&columns%5B2%5D%5Bsearchable%5D=true&columns%5B2%5D%5Borderable%5D=true&columns%5B2%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B2%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B3%5D%5Bdata%5D=3&columns%5B3%5D%5Bname%5D=market_value&columns%5B3%5D%5Bsearchable%5D=false&columns%5B3%5D%5Borderable%5D=true&columns%5B3%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B3%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B4%5D%5Bdata%5D=4&columns%5B4%5D%5Bname%5D=points&columns%5B4%5D%5Bsearchable%5D=false&columns%5B4%5D%5Borderable%5D=true&columns%5B4%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B4%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B5%5D%5Bdata%5D=5&columns%5B5%5D%5Bname%5D=worst_case_trend&columns%5B5%5D%5Bsearchable%5D=false&columns%5B5%5D%5Borderable%5D=false&columns%5B5%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B5%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B6%5D%5Bdata%5D=6&columns%5B6%5D%5Bname%5D=worst_case&columns%5B6%5D%5Bsearchable%5D=false&columns%5B6%5D%5Borderable%5D=true&columns%5B6%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B6%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B7%5D%5Bdata%5D=7&columns%5B7%5D%5Bname%5D=neutral_case_trend&columns%5B7%5D%5Bsearchable%5D=false&columns%5B7%5D%5Borderable%5D=false&columns%5B7%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B7%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B8%5D%5Bdata%5D=8&columns%5B8%5D%5Bname%5D=neutral_case&columns%5B8%5D%5Bsearchable%5D=false&columns%5B8%5D%5Borderable%5D=true&columns%5B8%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B8%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B9%5D%5Bdata%5D=9&columns%5B9%5D%5Bname%5D=best_case_trend&columns%5B9%5D%5Bsearchable%5D=false&columns%5B9%5D%5Borderable%5D=false&columns%5B9%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B9%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B10%5D%5Bdata%5D=10&columns%5B10%5D%5Bname%5D=best_case&columns%5B10%5D%5Bsearchable%5D=false&columns%5B10%5D%5Borderable%5D=true&columns%5B10%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B10%5D%5Bsearch%5D%5Bregex%5D=false&columns%5B11%5D%5Bdata%5D=11&columns%5B11%5D%5Bname%5D=target_value&columns%5B11%5D%5Bsearchable%5D=false&columns%5B11%5D%5Borderable%5D=true&columns%5B11%5D%5Bsearch%5D%5Bvalue%5D=&columns%5B11%5D%5Bsearch%5D%5Bregex%5D=false&order%5B0%5D%5Bcolumn%5D=0&order%5B0%5D%5Bdir%5D=asc&start=0&length=1000&search%5Bvalue%5D=&search%5Bregex%5D=false&_=1753881654899";

		try {
			Document doc = Jsoup.connect(link)//
					.headers(HttpHeaderUtil.getRandomHeaders())//
					.ignoreContentType(true)//
					.timeout(25000)//
					.get();

			JSONObject response = new JSONObject(doc.text());

			JSONArray list = new JSONArray(response.getJSONArray("data"));

			Map<String, JSONObject> finalMap = new HashMap<String, JSONObject>();

			for (int i = 0; i < list.length(); i++) {
				JSONObject playerData = new JSONObject();
				JSONArray apiPlayer = list.getJSONArray(i);
				String playerId = "";
				for (int apiPlIdx = 0; apiPlIdx < apiPlayer.length(); apiPlIdx++) {
					if (apiPlIdx == 0) {
						JSONArray nameAndId = (JSONArray) apiPlayer.get(apiPlIdx);
						playerId = String.valueOf(nameAndId.getInt(0));
						playerData.put("playerId", playerId);
						playerData.put("playerName", nameAndId.getString(1));
					} else if (apiPlIdx == 1) {
						playerData.put("position", apiPlayer.getString(apiPlIdx));
					} else if (apiPlIdx == 2) {
						playerData.put("club", apiPlayer.getString(apiPlIdx));
					} else if (apiPlIdx == 3) {
						playerData.put("marktWert", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					} else if (apiPlIdx == 4) {
						playerData.put("lastPoints", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					} else if (apiPlIdx == 5) {
						playerData.put("trendWorstImg", getImgUrl(apiPlayer.getString(apiPlIdx)));
					} else if (apiPlIdx == 6) {
						playerData.put("trendWorst", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					} else if (apiPlIdx == 7) {
						playerData.put("trendNeutralImg", getImgUrl(apiPlayer.getString(apiPlIdx)));
					} else if (apiPlIdx == 8) {
						playerData.put("trendNeutral", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					} else if (apiPlIdx == 9) {
						playerData.put("trendBestImg", getImgUrl(apiPlayer.getString(apiPlIdx)));
					} else if (apiPlIdx == 10) {
						playerData.put("trendBest", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					} else if (apiPlIdx == 11) {
						playerData.put("target", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
					}
				}
				if (playerId != null && !playerId.isBlank()) {
					finalMap.put(playerId, playerData);
				}
			}

			return finalMap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private static String getImgUrl(String apiUrl) {
		// <img class='trend_arrow' src='/assets/images/icons/arrow-downright-icon.png'
		// alt=''>"
		String resultString = apiUrl.replace("<img class='trend_arrow' src='", "https://www.com-analytics.de").replace("' alt=''>", "");
		return resultString;
	}

	public static Map<String, JSONObject> fetchTopRankingForAllPlayers() {
		// Spieler Position Verein Marktwert Gesamtpunkte Punkte pro Spiel
		// Preis-Leistung Historische Punkteausbeute
		String[] links = { "keeper", "defender", "midfielder", "striker" };
		try {
			Map<String, JSONObject> finalMap = new HashMap<String, JSONObject>();

			for (String pos : links) {
				String link = "https://www.com-analytics.de/ajax/topplayers/" + pos;
				Document doc = Jsoup.connect(link)//
						.headers(HttpHeaderUtil.getRandomHeaders())//
						.ignoreContentType(true)//
						.timeout(25000)//
						.get();

				JSONObject response = new JSONObject(doc.text());

				JSONArray list = new JSONArray(response.getJSONArray("data"));
				int countAdd = 0;
				for (int i = 0; i < list.length(); i++) {
					JSONObject playerData = new JSONObject();
					JSONArray apiPlayer = list.getJSONArray(i);
					String playerId = "";
					for (int apiPlIdx = 0; apiPlIdx < apiPlayer.length(); apiPlIdx++) {
						if (apiPlIdx == 0) {
							JSONArray nameAndId = (JSONArray) apiPlayer.get(apiPlIdx);
							playerId = String.valueOf(nameAndId.getInt(0));
							playerData.put("playerId", playerId);
							playerData.put("playerName", nameAndId.getString(1));
						} else if (apiPlIdx == 1) {
							playerData.put("position", apiPlayer.getString(apiPlIdx));
						} else if (apiPlIdx == 2) {
							playerData.put("club", apiPlayer.getString(apiPlIdx));
						} else if (apiPlIdx == 3) {
							playerData.put("marktWert", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
						} else if (apiPlIdx == 4) {
							playerData.put("totalPoints", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
						} else if (apiPlIdx == 5) {
							playerData.put("pointsPerGame", ZahlenParser.parseJsonValueToInt(apiPlayer.get(apiPlIdx)));
						} else if (apiPlIdx == 6) {
							playerData.put("preisLeistung", apiPlayer.getString(apiPlIdx));
						} else if (apiPlIdx == 7) {
							playerData.put("historicalPointYield", apiPlayer.getString(apiPlIdx));
						}
					}
					if (playerId != null && !playerId.isBlank()) {
						finalMap.put(playerId, playerData);
						countAdd++;

					}
				}
				LOGGER.info(countAdd + " TopPlayer für Positikon " + pos + " wurden ausgelesen");
			}
			return finalMap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Fetches player detail data from com-analytics.de and returns it as
	 * JSONObject. Parsed fields: Name, Position, Verein, Marktwert, Prognose,
	 * Gesamtpunkte, Punktevolatilität, Historische Punkteausbeute, Punkte am
	 * letzten Spieltag, Empfehlung, Änderung (7 Tage), Team-Änderung (7 Tage)
	 * [getrennt als Wert + Prozent], Änderung (3 Monate), Bewertete Spiele,
	 * Verletzungsanfälligkeit.
	 *
	 * @param url The URL of the player detail page (z.B.
	 *            https://www.com-analytics.de/player/33800)
	 * @return JSONObject containing all parsed properties
	 * @throws IOException on network or parsing error
	 */
	public static JSONObject fetchPlayerData(String playerId) {
		// Fetch HTML

		JSONObject result = null;
		try {
			if (playerId != null && !playerId.isBlank()) {
				String url = "https://www.com-analytics.de/player/" + playerId;
				Document doc = Jsoup.connect(url).headers(HttpHeaderUtil.getRandomHeaders())//
						.ignoreContentType(true)//
						.timeout(25000)//
						.get();

				// Tabelle mit Spielerwerten suchen
				Element table = doc.selectFirst("table.table");
				if (table == null)
					throw new IOException("Spielerdaten-Tabelle nicht gefunden!");

				result = new JSONObject();

				// Alle Zeilen ablaufen und Key-Value extrahieren
				for (Element row : table.select("tr")) {
					Elements tds = row.select("td");
					if (tds.size() < 2)
						continue;
					String key = tds.get(0).text().trim();
					String value = tds.get(1).text().trim();

					switch (key) {
					case "Name":
						result.put("Name", value);
						break;
					case "Position":
						result.put("Position", value);
						break;
					case "Verein":
						result.put("Verein", value);
						break;
					case "Marktwert":
						result.put("Marktwert", value.replace(".", "").replace("€", "").trim());
						break;
					case "Prognose":
						result.put("Prognose", value.replace(".", "").replace("€", "").trim());
						break;
					case "Gesamtpunkte":
						result.put("Gesamtpunkte", value);
						break;
					case "Punktevolatilität":
						result.put("Punktevolatilität", value);
						break;
					case "Historische Punkteausbeute":
						result.put("Historische_Punkteausbeute", value);
						break;
					case "Punkte am letzten Spieltag":
						result.put("Punkte_letzter_Spieltag", value);
						break;
					case "Empfehlung":
						result.put("Empfehlung", value);
						break;
					case "Änderung (7 Tage)":
						result.put("Aenderung_7Tage", value);
						break;
					case "Team-Änderung (7 Tage)":
						// Split z.B. "+1.180.000 / +1,7 %"
						Pattern pattern = Pattern.compile("([+-]?[\\d\\.]+)\\s*/\\s*([+-]?[\\d\\,]+)\\s*%");
						Matcher matcher = pattern.matcher(value);
						if (matcher.find()) {
							String teamAenderungWert = matcher.group(1).replace(".", "").replace(",", "").trim();
							String teamAenderungProzent = matcher.group(2).replace(",", ".").trim();
							result.put("Team_Aenderung_7Tage_Wert", teamAenderungWert);
							result.put("Team_Aenderung_7Tage_Prozent", teamAenderungProzent);
						} else {
							result.put("Team_Aenderung_7Tage_Wert", JSONObject.NULL);
							result.put("Team_Aenderung_7Tage_Prozent", JSONObject.NULL);
						}
						break;
					case "Änderung (3 Monate)":
						result.put("Aenderung_3Monate", value);
						break;
					case "Bewertete Spiele":
						result.put("Bewertete_Spiele", value);
						break;
					case "Verletzungsanfälligkeit":
						result.put("Verletzungsanfaelligkeit", value);
						break;
					// ggf. weitere Felder...
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
