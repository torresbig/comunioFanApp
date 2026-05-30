package comunio.nas.dataScraper.ligainsider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.LigainsiderRankingEntry;
import comunio.nas.objects.helper.LogManager;

/**
 * Parst das Ligainsider-Ranking von der Webseite.
 */
class LigainsiderRankingParser {
	
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderRankingParser.class);

	/**
	 * Parst die Ligainsider-Ranking-Tabelle und gibt eine Liste der Einträge
	 * zurück.
	 *
	 * @return Liste der LigainsiderRankingEntry-Objekte
	 * @throws IOException bei Verbindungs- oder Parsing-Fehlern
	 */
	public static List<LigainsiderRankingEntry> parseRanking() throws IOException {
		List<LigainsiderRankingEntry> result = new ArrayList<>();
		LOGGER.info("Lade Ligainsider-Ranking von: " + Urls.LIGAINSIDER_URL);

		// HTML-Dokument laden
		Document doc = Jsoup.connect(Urls.LIGAINSIDER_URL)//
				.userAgent("Mozilla/5.0")//
				.get();

		// Tabellenzeilen selektieren
		Elements rows = doc.select("table#DataTable > tbody > tr");
		int rangCount = 1;
		for (Element row : rows) {
			Elements tds = row.select("td");
			// Sicherheitscheck: Nur vollständige Zeilen verarbeiten
			if (tds.size() < 10) {
				LOGGER.fine("Überspringe Zeile mit zu wenigen Spalten: " + row.text());
				continue;
			}

			// Rang fortlaufend setzen:
			int rang = rangCount;
			rangCount++;

			// Spielername extrahieren (Index 2, enthält <a>)
			String name = tds.get(2).text().trim();

			// Verein extrahieren (Index 3, enthält <a>)
			String verein = tds.get(3).text().trim();

			// Durchschnittsnote (Index 4)
			double durchschnittsnote = parseDoubleSafe(tds.get(4).text());

			// Durchschnittspunkte (Index 6)
			double durchschnittspunkte = parseDoubleSafe(tds.get(6).text());

			// Punkte gesamt (Index 7)
			int punkte = parseIntSafe(tds.get(7).text());

			// Einsätze (bewertet) (Index 8, Format: "17 (17)")
			int einsaetzeBewertet = parseEinsaetze(tds.get(8).text());

			// Durchschnittsminuten (Index 9)
			int durchschnittsminuten = parseIntSafe(tds.get(9).text());

			// Eintrag erzeugen und der Ergebnisliste hinzufügen
			LigainsiderRankingEntry entry = new LigainsiderRankingEntry(name, rang, verein, durchschnittsnote, durchschnittspunkte, punkte, einsaetzeBewertet, durchschnittsminuten);
			result.add(entry);
		}

		LOGGER.info("Ligainsider-Ranking: " + result.size() + " Einträge gefunden.");
		return result;
	}

	private static Integer parseIntSafe(String s) {
		s = s.trim();
		if (s.isEmpty())
			return null;
		try {
			return Integer.parseInt(s.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return null;
		}
	}

	private static double parseDoubleSafe(String s) {
		s = s.trim().replace(",", ".");
		if (s.isEmpty())
			return 0.0;
		try {
			return Double.parseDouble(s.replaceAll("[^0-9\\.]", ""));
		} catch (Exception e) {
			return 0.0;
		}
	}

	private static int parseEinsaetze(String einsaetze) {
		int idx1 = einsaetze.indexOf('(');
		int idx2 = einsaetze.indexOf(')');
		if (idx1 >= 0 && idx2 > idx1) {
			try {
				return Integer.parseInt(einsaetze.substring(idx1 + 1, idx2));
			} catch (Exception e) {
				// fallback
			}
		}
		return parseIntSafe(einsaetze) != null ? parseIntSafe(einsaetze) : 0;
	}

}