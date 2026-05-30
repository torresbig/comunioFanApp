package comunio.nas.dataScraper.comAnalystics;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;

public class ComAnalysticsTopFlop {

	private static final Logger LOGGER = LogManager.getLogger(ComAnalysticsTopFlop.class);

	/**
	 * Extrahiert die ComAnalystics Top/Flop Daten und fügt sie in das
	 * playerDBObject ein.
	 * 
	 * @param playerDBObject Das Spieler-Datenbank-JSONObject
	 */

	public static void getComAnalysticsTopFlopData(JSONObject playerDBObject, LastUpdates lastUpdates) {
		LOGGER.log(Level.INFO, "Starte ComAnalysticsTopFlop Datenextraktion...");
		int anzahl = 0;
		if (playerDBObject == null) {
			LOGGER.warning("playerDBObject ist null. Abbruch der Datenextraktion.");
			return;
		}

		JSONArray playerDb = playerDBObject.optJSONArray("playerDB");

		if (playerDb == null) {
			LOGGER.warning("playerDB ist null. Abbruch der Datenextraktion.");
			return;
		}

		try {
			// zuerst muss die PlayerDb durchgegangen werden und überall die
			// comAnalysticsTopFlop gelöscht werden
			for (int j = 0; j < playerDb.length(); j++) {
				JSONObject player = playerDb.getJSONObject(j);
				JSONObject playerData = player.optJSONObject("data");
				
				if (playerData != null && playerData.has("comAnalysticsTopFlop")) { // hier noch ändern
					playerData.remove("comAnalysticsTopFlop");
				}
				
				if (playerData != null && playerData.has("comAnalystics")) { // hier noch ändern
					JSONObject comAnalystics = playerData.optJSONObject("comAnalystics");
					comAnalystics.remove("topFlop");
				}
			}

			// Jetzt die neuen Daten holen
			JSONArray daten = fetchPlayerTrends();
			for (int i = 0; i < daten.length(); i++) {
				JSONObject trendObj = daten.getJSONObject(i);
				String spielerId = trendObj.getString("id");

				// Finde den Spieler in der playerDB
				for (int j = 0; j < playerDb.length(); j++) {
					JSONObject player = playerDb.getJSONObject(j);
					JSONObject playerData = player.optJSONObject("data");
					if (playerData != null) {
						JSONObject comAnalystics = playerData.optJSONObject("comAnalystics", new JSONObject());
						if (player.optString("id").equals(spielerId)) {
							// Spieler gefunden, füge die Trenddaten hinzu
							trendObj.remove("id"); // Entferne die SpielerId aus den Trenddaten
							trendObj.remove("name"); // Entferne den Spielernamen aus den Trenddaten
							trendObj.put("lastUpdate", new ComunioDate());// // LastUpdate-Zeitstempel
																			
							comAnalystics.put("topFlop", trendObj);
							comAnalystics.put("link", getLink(spielerId));
							playerData.put("comAnalystics", comAnalystics);
							break; // Spieler gefunden, Schleife beenden
						}
					}
				}
			}
			anzahl = daten.length();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler bei der ComAnalysticsTopFlop Datenextraktion: " + e.getMessage(), e);
		}
		lastUpdates.setComAnalystics(Instant.now());
		LOGGER.log(Level.INFO,
				"Ende ComAnalysticsTopFlop Datenextraktion - Extrahierte Gesamtanzahl Spieler: " + anzahl);
	}

	/**
	 * Lädt die HTML-Seite und parsed die Spielertrends ("Top" und "Flop").
	 * 
	 * @param url Ziel-URL mit Trendtabellen
	 * @return Alle Spieler als JSONArray
	 */
	public static JSONArray fetchPlayerTrends() {
		String url = "https://www.com-analytics.de/top_flop_predictions";
		JSONArray result = new JSONArray();
		try {
			LOGGER.info("Lade die Seite: " + url);
			Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0") // User Agent setzen, damit die Seite geladen
																		// wird
					.get();

			// Tabellen-Crawler, arbeitet für jede Kategorie
			parseTable(doc, "top_absolute", "top", result);
			parseTable(doc, "flop_absolute", "flop", result);

		} catch (IOException e) {
			LOGGER.severe("Fehler beim Laden oder Parsen der Seite: " + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Parst eine Trendtabelle ("Top" oder "Flop") und fügt die Daten in das
	 * JSONArray ein.
	 * 
	 * @param doc   Jsoup-Dokument
	 * @param tabId HTML-ID der Tabelle (z. B. "top_absolute")
	 * @param type  "top" oder "flop"
	 * @param arr   Ziel-Array
	 */
	private static void parseTable(Document doc, String tabId, String type, JSONArray arr) {
		Element table = doc.select("div#" + tabId + " table").first();
		if (table == null) {
			LOGGER.warning("Tabelle mit id " + tabId + " nicht gefunden!");
			return;
		}

		Elements rows = table.select("tbody tr");
		int platz = 1;
		for (Element row : rows) {
			try {
				Elements cells = row.select("td");
				// Spieler-Name + Link
				Element playerCell = cells.get(1).selectFirst("a");
				String spieler = playerCell.text();
				String href = playerCell.attr("href");
				// Extrahiere die Spieler-ID aus "/player/xxxxx"
				String spielerId = href.replaceAll(".*/player/(\\d+).*", "$1");

				String punkte = cells.get(2).text().trim();
				String marktwert = cells.get(3).text().trim();
				String zielwert = cells.get(4).text().trim();
				String aenderung = cells.get(5).text().trim();

				JSONObject obj = new JSONObject();
				obj.put("typ", type);
				obj.put("platz", platz++);
				obj.put("name", spieler);
				obj.put("id", spielerId);
				obj.put("punkte", punkte);
				obj.put("marktwert", marktwert);
				obj.put("zielwert", zielwert);
				obj.put("aenderung", aenderung);

				arr.put(obj);
			} catch (Exception ex) {
				LOGGER.warning("Fehler beim Parsen einer Zeile: " + ex.getMessage());
			}
		}
		LOGGER.info("Kategorie '" + type + "' (" + tabId + ") verarbeitet. Anzahl Datensätze: " + (platz - 1));
	}

	/**
	 * Beispiel-Methode zum Testen und Ausgabe des Ergebnisses.
	 */
	public static void main(String[] args) {
		String url = "https://www.com-analytics.de/top_flop_predictions";
		JSONArray daten = fetchPlayerTrends();
		// Ausgabe zum Debuggen
		System.out.println(daten.toString(2));
		LOGGER.info("Extrahierte Gesamtanzahl Spieler: " + daten.length());
	}

	private static String getLink(String id) {
		return "https://www.com-analytics.de/player/" + id;
	}

}
