package comunio.nas.dataScraper.ligainsider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.LigainsiderClubObject;

public class LigainsiderClubParser {

	private static final Logger LOGGER = LogManager.getLogger(LigainsiderClubParser.class);

	private static final Pattern LIGAINSIDER_ID_PATTERN = Pattern.compile("/([^/]+)/(\\d+)/?");

	/**
	 * Parst die LigaInsider HTML-Tabelle, extrahiert die Clubdaten und reichert das
	 * bestehende clubDb-JSONArray um das 'ligainsider'-Objekt an.
	 *
	 * @param htmlContent HTML Snippet der LigaInsider Tabelle
	 * @param clubDbJson  Das originale Club-DB JSON Array als String
	 * @return Aktualisiertes JSONArray
	 */
	public static Map<String, LigainsiderClubObject> parseAndMapLigaInsider() {
		Map<String, LigainsiderClubObject> extractedClubs = new HashMap<>();
		// HTML-Dokument laden
		Document doc;
		try {
			doc = Jsoup.connect(Urls.LIGAINSIDER_CLUBS)//
					.userAgent("Mozilla/5.0")//
					.get();

			Elements rows = doc.select("tr.table_row");

			// Map zum schnellen Nachschlagen der gecrawlten LigaInsider-Daten

			for (Element row : rows) {
				Element linkElement = row.selectFirst("td.title_column2 span a");
				if (linkElement == null)
					continue;

				String clubName = linkElement.text().trim();
				String relativeUrl = linkElement.attr("href").trim();
				String fullLink = "https://www.ligainsider.de" + relativeUrl;

				// Extract Club-ID aus der URL (z.B. "/sc-freiburg/18/" -> 18)
				String clubId = "";
				Matcher matcher = LIGAINSIDER_ID_PATTERN.matcher(relativeUrl);
				if (matcher.find()) {
					clubId = matcher.group(2);
				}

				LigainsiderClubObject liga = new LigainsiderClubObject(clubName, clubId, fullLink);

				// Speichern unter normalisiertem Namen für flexibleres Matching
				extractedClubs.put(clubId, liga);
			}

			// 2. ClubDB Array einlesen und mit den extrahierten Daten anreichern
		} catch (IOException e) {
			LOGGER.warning("Failed to fetch LigaInsider data: " + e.getMessage());
		}
		return extractedClubs;
	}

}
