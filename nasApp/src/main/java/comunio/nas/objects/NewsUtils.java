package comunio.nas.objects;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.NewsArt;
import comunio.nas.util.ClubMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsUtils {

	/**
	 * Lädt die News.json von GitHub und gibt das aktuellste Datum als LocalDate
	 * zurück. Gibt null zurück, wenn keine News vorhanden sind.
	 */
	public static LocalDate getLastNewsDateFromGitHub() {
		try {
			
			URL url = new URL(Urls.NEWS_DB_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "ComunioFanApp");

			if (conn.getResponseCode() == 200) {
				StringBuilder response = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						response.append(line);
					}
				}
				JSONArray newsArray = new JSONArray(response.toString());
				List<LocalDate> dates = new ArrayList<>();
				for (int i = 0; i < newsArray.length(); i++) {
					JSONObject newsObj = newsArray.getJSONObject(i);
					if (newsObj.has("date")) {
						dates.add(LocalDate.parse(newsObj.getString("date")));
					}
				}
				if (!dates.isEmpty()) {
					return dates.stream().max(LocalDate::compareTo).orElse(null);
				}
			}
		} catch (Exception e) {
			// Logging, falls gewünscht
		}
		return null;
	}

	/**
	 * Durchsucht alle News im gegebenen NewsManager und korrigiert alte
	 * Vereinswechsel-Einträge.
	 * 
	 * Alte Vereinswechsel-News haben im Feld 'text' einen Klartext-String, z.B.
	 * "Vereinswechsel: Tabaković (Verein 62 → Verein 3)". Diese Methode wandelt
	 * diese Texte in ein strukturiertes JSON-Format um, z.B.
	 * {"playerName":"Tabaković","oldClub":62,"newClub":3}.
	 * 
	 * Dabei werden die Vereinsnummern als Integer gespeichert (ohne das Wort
	 * „Verein“).
	 * 
	 * @param newsManager NewsManager mit den zu verarbeitenden News
	 * @return Anzahl der News, die erfolgreich konvertiert wurden
	 */
	public static int convertOldClubChangeTextToJson(NewsManager newsManager, JSONArray clubDB) {
		int convertedCount = 0;

		// Regex, um aus dem Klartext den Spielernamen, alte und neue Vereinsnummer zu
		// extrahieren
		// Beispieltext: "Vereinswechsel: Tabaković (Verein 62 → Verein 3)"
		Pattern pattern = Pattern.compile("Vereinswechsel:\\s*(.+?)\\s*\\(Verein\\s*(\\d+)\\s*→\\s*(?:Verein\\s*)?(\\d+)\\)");

		// Zugriff auf alle News-Listen gruppiert nach Datum
		for (List<News> newsList : newsManager.getNewsByDate().values()) {
			for (News news : newsList) {
				// Nur Vereinswechsel-News prüfen
				if (news.getArt() == NewsArt.VEREINSWECHSEL) {
					String text = news.getText();

					// Prüfen, ob 'text' schon ein JSON-Objekt ist, dann keine Umwandlung nötig
					if (!isJson(text)) {
						// Versuchen, Klartext mit Regex zu parsen
						Matcher matcher = pattern.matcher(text);
						if (matcher.find()) {
							// Spielername aus dem ersten Erfassungsblock (Gruppe 1)
							String playerName = matcher.group(1).trim();

							// Alte Vereinsnummer als Integer aus Gruppe 2
							int oldClubId = Integer.parseInt(matcher.group(2).trim());

							// Neue Vereinsnummer als Integer aus Gruppe 3
							int newClubId = Integer.parseInt(matcher.group(3).trim());

							// Strukturierte JSON-Daten erzeugen
							JSONObject json = new JSONObject();
							json.put("playerName", playerName);
							json.put("oldClubId", oldClubId);
							json.put("newClubId", newClubId);
							json.put("oldClub", ClubMapper.getClubnameFromComunioId(String.valueOf(oldClubId), clubDB));
							json.put("newClub", ClubMapper.getClubnameFromComunioId(String.valueOf(newClubId), clubDB));

							// das News-Objekt aktualisieren mit dem neuen JSON-String im Feld 'text'
							news.setText(json.toString());

							convertedCount++; // Zähler erhöhen
						}
					}
				}
			}
		}
		// Anzahl der konvertierten News zurückgeben
		return convertedCount;
	}

	/**
	 * Hilfsmethode: Prüft, ob ein String ein gültiges JSON-Objekt ist.
	 * 
	 * @param text Der zu prüfende String
	 * @return true, wenn der String ein JSON-Objekt repräsentiert, ansonsten false
	 */
	private static boolean isJson(String text) {
		if (text == null)
			return false;
		try {
			new JSONObject(text);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
