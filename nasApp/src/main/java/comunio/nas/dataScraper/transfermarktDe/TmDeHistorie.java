package comunio.nas.dataScraper.transfermarktDe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TmDeHistorie {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	/**
	 * Parst die Verletzungshistorie eines Spielers von der Transfermarkt-Seite und
	 * gibt sie als JSON-Array aus. Jedes Element im Array enthält die Saison, die
	 * Verletzungsart, die Start- und Enddaten der Verletzung, die Anzahl der Tage
	 * und die Anzahl der verpassten Spiele. Die Methode erwartet den HTML-String
	 * der Verletzungshistorie-Seite eines Spielers, extrahiert die relevanten
	 * Informationen aus der Tabelle und gibt sie in einem strukturierten
	 * JSON-Format aus.
	 * 
	 * Beispielaufruf: String playerString = "<html>...</html>"; // HTML der
	 * Verletzungshistorie-Seite getVerletzungshistorie(playerString);
	 * 
	 * @param playerString HTML-String der Verletzungshistorie-Seite eines Spielers
	 * 
	 */
	public static void getVerletzungshistorie(String playerString) {

		// HTML laden (oder Jsoup.connect(URL).get())
		Document doc = Jsoup.parse(playerString);

		JSONArray result = new JSONArray();

		// Tabelle finden
		Elements rows = doc.select("table.items tbody tr");

		for (Element row : rows) {

			String saison = row.select("td").get(0).text().trim();
			String verletzung = row.select("td").get(1).text().trim();
			String vonStr = row.select("td").get(2).text().trim();
			String bisStr = row.select("td").get(3).text().trim();
			String verpassteSpieleStr = row.select("td").get(5).select("span").text().trim();

			// Datum "von"
			LocalDate von = LocalDate.parse(vonStr, FORMAT);

			// Datum "bis" (kann leer sein)
			LocalDate bis = bisStr.isEmpty() ? LocalDate.now() : LocalDate.parse(bisStr, FORMAT);

			long tage = ChronoUnit.DAYS.between(von, bis);

			int verpassteSpiele = verpassteSpieleStr.isEmpty() ? 0 : Integer.parseInt(verpassteSpieleStr);

			JSONObject obj = new JSONObject();
			obj.put("saison", saison);
			obj.put("verletzung", verletzung);
			obj.put("von", vonStr);
			obj.put("bis", bisStr.isEmpty() ? null : bisStr);
			obj.put("tage", tage);
			obj.put("verpassteSpiele", verpassteSpiele);

			result.put(obj);
		}

		System.out.println(result.toString(2));
	}

	public static List<SuspensionEntry> parseSuspensions(Document doc) {

		List<SuspensionEntry> list = new ArrayList<>();
		DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		Elements rows = doc.select("table.items tbody tr");

		for (Element row : rows) {

			String saison = row.select("td").get(0).text().trim();
			String art = row.select("td").get(1).text().trim();

			// Wettbewerb steht im title-Attribut des <img>
			Element img = row.select("td").get(2).selectFirst("img");
			String wettbewerb = img != null ? img.attr("title") : "";

			String vonStr = row.select("td").get(3).text().trim();
			String bisStr = row.select("td").get(4).text().trim();

			// verpasste Spiele: steht VOR dem <a>, also direkt im Text
			String verpassteSpieleStr = row.select("td").get(6).ownText().trim();

			LocalDate von = LocalDate.parse(vonStr, FORMAT);
			LocalDate bis = bisStr.isEmpty() ? LocalDate.now() : LocalDate.parse(bisStr, FORMAT);

			long tage = ChronoUnit.DAYS.between(von, bis);
			int verpassteSpiele = verpassteSpieleStr.isEmpty() ? 0 : Integer.parseInt(verpassteSpieleStr);

			list.add(new SuspensionEntry(saison, art, wettbewerb, von, bisStr.isEmpty() ? null : bis, tage, verpassteSpiele));
		}

		return list;
	}

}
