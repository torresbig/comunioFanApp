package comunio.nas.dataScraper.fussballdatenDe;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.DateUtils;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.player.PlayerMatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Klasse zum Scrapen von Bundesliga-Spielerstatistiken für jeden Spieltag von
 * fussballdaten.de.
 * <p>
 * Lädt Seiten mit paginierten Spielerlisten und bereitet strukturierte
 * JSON-Ausgaben auf.
 */
public class FussballdatenDeDataScraper {

	/**
	 * Logger-Instanz für diese Klasse zur Protokollierung.
	 */
	private static final Logger LOGGER = LogManager.getLogger(FussballdatenDeDataScraper.class);

	/**
	 * Lädt alle Seiten mit Spielerstatistiken für einen bestimmten Spieltag.
	 * Entnimmt sämtliche Spieler und aggregiert sie in einem JSONArray.
	 *
	 * @param playerDB JSONArray mit Spieler-Basisdaten (z.B. aus einer eigenen DB)
	 * @param clubDB   JSONArray mit Vereins-Basisdaten
	 * @param spieltag Spieltag der Saison 2026 (1-basiert)
	 * @return JSONArray mit allen Spielerstatistiken für diesen Spieltag
	 * @throws IOException bei Netzwerkfehlern oder Parsingproblemen
	 */
	private static JSONArray ladeAlleSpielerFuerSpieltag(int spieltag) throws IOException {
		JSONArray alleSpieler = new JSONArray();
		int aktuelleSeite = 1;

		// Pagination-Schleife: solange weitere Spieler-Seiten vorhanden sind
		while (true) {
			String url = "https://www.fussballdaten.de/bundesliga/2026/" + spieltag + "/einsaetze/?page="
					+ aktuelleSeite;
			Document doc = null;
			try {
				doc = loadDocWithRetry(url, 3, 2500);
			} catch (HttpStatusException | InterruptedException e) {
				LOGGER.warning("Fehler beim Laden der Seite: " + url + " - " + e.getMessage());
				e.printStackTrace();
			}
			if (doc == null) {
				LOGGER.warning("Kein Dokument geladen auf Seite " + aktuelleSeite + " für Spieltag " + spieltag);
				break;
			}

			Element tabelle = doc.selectFirst("table.table-statistik.full");
			if (tabelle == null) {
				LOGGER.info("Keine Statistik-Tabelle auf Seite " + aktuelleSeite + " für Spieltag " + spieltag);
				break;
			}

			Elements zeilen = tabelle.select("tbody tr.tr-item");
			if (zeilen.isEmpty()) {
				LOGGER.info("Keine weiteren Spieler auf Seite " + aktuelleSeite + " für Spieltag " + spieltag);
				break;
			}

			// Extrahiert die Daten jedes einzelnen Spieler-Eintrags
			for (Element zeile : zeilen) {
				JSONObject spielerJson = new JSONObject();

				Element spielerLink = zeile.selectFirst("td.vam a.table-link");
				String spielerName = spielerLink.text();
				String spielerUrl = "https://www.fussballdaten.de" + spielerLink.attr("href");

				String tore = zeile.select("td.text-center.hidden-xs").get(0).text();
				String einsatzzeit = zeile.select("td.text-center").last().text();

				Element flagge = zeile.selectFirst("span.flag-icon");
				String nation = flagge != null ? flagge.attr("title") : "";

				Element vereinLink = zeile.select("td.vam div.dib a.table-link.fwn.fs10").first();
				String verein = vereinLink != null ? vereinLink.text() : "";

				spielerJson.put("name", spielerName);
				spielerJson.put("url", spielerUrl);
				spielerJson.put("tore", Integer.valueOf(tore));
				spielerJson.put("einsatzzeit", Integer.valueOf(einsatzzeit));
				spielerJson.put("nationalitaet", nation);
				spielerJson.put("club", verein);
				alleSpieler.put(spielerJson);
			}

			// Prüfen, ob es noch weitere Seiten gibt (Pagination-Logik)
			Element nextPage = doc.selectFirst("ul.pagination li.next:not(.disabled) a");
			if (nextPage == null) {
				// Keine weitere Seite mehr verfügbar
				break;
			} else {
				aktuelleSeite++;
			}
		}

		return alleSpieler;
	}

	/**
	 * Lädt die HTML-Seite robust mit zufälligen HTTP-Headern. Führt automatisches
	 * Retry bei HTTP 502 Gateway-Fehlern durch (bis maxRetries).
	 *
	 * @param url        Die abzurufende URL
	 * @param maxRetries Maximale Anzahl an Retry-Versuchen bei 502-Fehlern
	 * @param delayMs    Wartezeit zwischen Retries in Millisekunden
	 * @return JSoup-Document mit dem geladenen HTML (oder Exception im Fehlerfall)
	 * @throws HttpStatusException  bei irreparablen HTTP-Fehlern
	 * @throws InterruptedException falls Sleep unterbrochen wird
	 */
	private static Document loadDocWithRetry(String url, int maxRetries, int delayMs)
			throws InterruptedException, HttpStatusException {
		int attempt = 0;
		while (true) {
			try {
				// Erzeugt zufällige Browser-ähnliche HTTP-Header
				Map<String, String> headers = HttpHeaderUtil.getRandomHeaders();

				// Jsoup mit Headern und Timeout verwenden
				return Jsoup.connect(url).headers(headers).timeout(10_000).get();
			} catch (HttpStatusException e) {
				if (e.getStatusCode() == 502 && attempt < maxRetries) {
					LOGGER.warning("HTTP 502 beim Laden von " + url + ", erneuter Versuch nach " + delayMs
							+ " ms (Versuch " + (attempt + 1) + " von " + maxRetries + ")");
					Thread.sleep(delayMs);
					attempt++;
				} else {
					LOGGER.severe("HTTP-Fehler " + e.getStatusCode() + " bei URL: " + url + " - " + e.getMessage());
					throw e;
				}
			} catch (IOException e) {
				LOGGER.severe("Netzwerkfehler beim Abrufen von: " + url + " - " + e.getMessage());
				throw new RuntimeException("Netzwerkfehler beim Abruf von: " + url, e);
			}
		}
	}

}
