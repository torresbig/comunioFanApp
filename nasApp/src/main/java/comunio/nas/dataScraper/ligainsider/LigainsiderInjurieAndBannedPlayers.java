package comunio.nas.dataScraper.ligainsider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.InjuryAndBlockedData;

public class LigainsiderInjurieAndBannedPlayers {
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderInjurieAndBannedPlayers.class);

	public static void main(String[] args) throws IOException {
		parseVerletzteUndGesperrte();
	}
	
	public static void updateAllInjuredBannedPlayer() {
		
	}

	public static Map<String, List<InjuryAndBlockedData>> parseVerletzteUndGesperrte() {
		Map<String, List<InjuryAndBlockedData>> result = new HashMap<>();
		String url = Urls.LIGAINSIDER_INJURY;
		LOGGER.info("Lade verletzte und gesperrte Spieler von: " + url);
		try {

			// ... innerhalb deiner Methode
			Document doc = Jsoup.connect(url).get();

			// Alle Tabellen-Container selektieren
			Elements tables = doc.select(".personal_table.personal_table_top");

			for (Element table : tables) {
				List<InjuryAndBlockedData> clubList = new ArrayList<>();

				// 1. NUR den Vereinsnamen holen (aus dem h2 innerhalb von .leg_table_title)
				// .text() auf dem ganzen Container würde die Spaltenüberschriften mitnehmen
				String clubName = table.select(".leg_table_title h2").text().trim();

				Elements rows = table.select(".small_table_row");

				for (Element row : rows) {
					InjuryAndBlockedData resultObject = new InjuryAndBlockedData();
					if (row.select(".small_table_column_noentry").isEmpty()) {
						String playerName = row.select(".left_title").text().trim();

						// 2. Das Symbol (Alt-Text des Bildes)
						String statusSymbol = row.select(".left_icon img").attr("alt").trim();

						if (row.select(".small_table_column_noentry").isEmpty()) {

							// 1. Spieler-Link Element finden
							Element playerLinkElem = row.select(".small_table_column1 > a").first();
							String playerLink = "";
							String playerId = "";

							if (playerLinkElem != null) {
								// Vollständige URL (z.B. https://www.ligainsider.de/emre-can_1812/)
								playerLink = playerLinkElem.absUrl("href");

								// Relativer Pfad für die ID-Extraktion (z.B. /emre-can_1812/)
								String relativePath = playerLinkElem.attr("href");

								// 2. ID extrahieren (alles zwischen dem letzten Unterstrich '_' und dem letzten
								// Slash '/')
								if (relativePath.contains("_")) {
									// Extrahiert "1812" aus "/emre-can_1812/"
									playerId = relativePath.substring(relativePath.lastIndexOf("_") + 1, relativePath.lastIndexOf("/"));
								}
							}

							resultObject.setClub(clubName);
							resultObject.setPlayerName(playerName);
							resultObject.setPlayerId(playerId);
							resultObject.setPlayerLink(playerLink);
						}

						String grund = row.select(".small_table_column2").text().trim();

						// 3. News Text UND Link extrahieren
						Element newsElement = row.select(".small_table_column3 a").first();
						String newsText = "";
						String newsLink = "";
						if (newsElement != null) {
							newsText = newsElement.text().trim();
							// .absUrl("href") gibt den kompletten Link inkl. https://... zurück
							newsLink = newsElement.absUrl("href");
						}

						String dauer = row.select(".small_table_column4").text().trim();
						resultObject.setSinceString(dauer);
						resultObject.setReason(grund);
						resultObject.setStatus(statusSymbol);
						if (!newsLink.isEmpty()) {
							resultObject.setLastNewsLink(newsLink);
						}
						if (!newsText.isEmpty()) {
							resultObject.setLastNewsText(newsText);
						}
						clubList.add(resultObject);
					}

				}
				if (!clubList.isEmpty()) {
					result.put(clubName, clubList);

				}
			}
		} catch (Exception e) {
			LOGGER.warning("Fehler beim abfragen der Ligainsiderwebsite - VERLETZE & GESPERRTE Spieler -->" + url + " << FEHLER: " + e.getMessage());
		}
		return result;
	}

}
