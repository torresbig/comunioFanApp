package comunio.nas.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import comunio.nas.dataVariable.Urls;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.player.PlayerHelper;

/**
 * Utility-Klasse zum Laden, Validieren und Verarbeiten von JSON-Ressourcen
 * über HTTP-Verbindungen (z. B. GitHub Raw User-Content).
 * <p>
 * Bietet automatische Fehlertoleranz durch Retry-Mechanismen bei temporären 
 * Netzwerkausfällen oder GitHub Rate-Limits sowie aussagekräftiges Logging 
 * bei unerwarteten Serverantworten (z. B. HTML-Fehlerseiten).
 */
public class LoadJSONfromFile {
	
	private static final Logger LOGGER = LogManager.getLogger(LoadJSONfromFile.class);
	
	/** Max. Anzahl an Versuchen bei Netzwerk- oder Server-Fehlern. */
	private static final int MAX_RETRIES = 3;
	
	/** Wartezeit in Millisekunden zwischen zwei Retry-Versuchen. */
	private static final long RETRY_DELAY_MS = 2000;

	/**
	 * Sendet eine HTTP-GET-Anfrage an die angegebene URL mit Retry-Logik und Timeouts.
	 * <p>
	 * Fügt automatisch einen Cache-Buster-Parameter an die URL an, um das Caching von 
	 * GitHub Raw Content zu umgehen. Bei temporären Serverfehlern (z. B. HTTP 429 Rate Limit)
	 * oder Verbindungsabbrüchen wird die Anfrage bis zu {@link #MAX_RETRIES}-mal wiederholt.
	 *
	 * @param urlString Die Ziel-URL der HTTP-Anfrage.
	 * @return Das empfangene {@link HttpResponse}-Objekt mit dem Body als String.
	 * @throws Exception Wenn alle Verbindungsversuche fehlschlagen.
	 */
	private static HttpResponse<String> sendWithRetry(String urlString) throws Exception {
		// HTTP-Client mit globalem Verbindungs-Timeout initialisieren
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();

		// Cache-Buster (Zeitstempel) anfügen, um stets die aktuellste GitHub-Version zu erzwingen
		String urlWithCacheBuster = urlString + (urlString.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(urlWithCacheBuster))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

		HttpResponse<String> response = null;
		Exception lastException = null;

		// Retry-Schleife für Netzwerkanfragen
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofString());
				int status = response.statusCode();

				// Erfolgreiche Antworten (200 OK) oder gezieltes 404 (Datei nicht vorhanden) sofort zurückgeben
				if (status == 200 || status == 404) {
					return response;
				}

				LOGGER.warning("HTTP Status " + status + " bei URL: " + urlString + " (Versuch " + attempt + "/" + MAX_RETRIES + ")");
			} catch (Exception e) {
				lastException = e;
				LOGGER.warning("Netzwerkfehler bei Versuch " + attempt + "/" + MAX_RETRIES + ": " + e.getMessage());
			}

			// Kurze Pause vor dem nächsten Versuch einlegen
			if (attempt < MAX_RETRIES) {
				Thread.sleep(RETRY_DELAY_MS);
			}
		}

		// Falls eine Antwort vorliegt (z. B. Status 500), diese zurückgeben, sonst die geworfene Exception
		if (response != null) {
			return response;
		}
		throw lastException != null ? lastException : new IOException("Fetch fehlgeschlagen nach " + MAX_RETRIES + " Versuchen");
	}

	/**
	 * Lädt ein {@link JSONArray} von einer angegebenen URL (z. B. GitHub Raw-Link).
	 * <p>
	 * Prüft vor dem Parsen explizit, ob die Antwort mit einer eckigen Klammer {@code '['} beginnt. 
	 * Sollte GitHub eine HTML-Fehlerseite zurückgeben, wird der Inhalt im Log erfasst und 
	 * eine klare {@link JSONException} geworfen.
	 *
	 * @param urlString Die URL zur JSON-Datei.
	 * @return Das geladene {@link JSONArray} mit den Daten.
	 * @throws Exception Bei HTTP-Fehlern, ungültigem Format oder Parsing-Problemen.
	 */
	public static JSONArray loadJsonArrayFromUrl(String urlString) throws Exception {
		HttpResponse<String> response = sendWithRetry(urlString);
		int statusCode = response.statusCode();

		// Sonderfall 404: Datei existiert auf GitHub noch nicht -> neu anlegen
		if (statusCode == 404) {
			GitHubUploader.uploadToGitHub(Urls.getFilePathForGit(urlString), new JSONObject().toString(), Urls.getFilename(urlString) + " - datei nicht vorhanden! Wird erstellt!");
			LOGGER.info(Urls.getFilename(urlString) + " - nicht auf GITHUB vorhanden. wurde erstellt!");
			return new JSONArray();
		} else if (statusCode != 200) {
			throw new IOException("HTTP error code: " + statusCode + " - Body: " + truncateBody(response.body()));
		}

		String body = response.body() != null ? response.body().trim() : "";
		
		// Syntax-Check: Muss zwingend mit '[' beginnen
		if (!body.startsWith("[")) {
			LOGGER.severe("Fehler beim Parsen von JSONArray aus URL: " + urlString);
			LOGGER.severe("Empfangener Content (kein '[' am Anfang): " + truncateBody(body));
			throw new JSONException("A JSONArray text must start with '['. Empfangen: " + truncateBody(body));
		}

		return new JSONArray(body);
	}

	/**
	 * Lädt ein {@link JSONObject} von einer angegebenen URL (z. B. GitHub Raw-Link).
	 * <p>
	 * Prüft vor dem Parsen explizit, ob die Antwort mit einer geschweiften Klammer {@code '{'} beginnt.
	 *
	 * @param urlString Die URL zur JSON-Datei.
	 * @return Das geladene {@link JSONObject}.
	 * @throws Exception Bei HTTP-Fehlern, ungültigem Format oder Parsing-Problemen.
	 */
	public static JSONObject loadJsonObjectFromUrl(String urlString) throws Exception {
		HttpResponse<String> response = sendWithRetry(urlString);
		int statusCode = response.statusCode();

		// Sonderfall 404: Datei existiert auf GitHub noch nicht -> neu anlegen
		if (statusCode == 404) {
			GitHubUploader.uploadToGitHub(Urls.getFilePathForGit(urlString), new JSONObject().toString(), Urls.getFilename(urlString) + " - datei nicht vorhanden! Wird erstellt!");
			LOGGER.info(Urls.getFilename(urlString) + " - nicht auf GITHUB vorhanden. wurde erstellt!");
			return new JSONObject();
		} else if (statusCode != 200) {
			throw new IOException("HTTP error code: " + statusCode + " - Body: " + truncateBody(response.body()));
		}
		
		String body = response.body() != null ? response.body().trim() : "";
		
		// Syntax-Check: Muss zwingend mit '{' beginnen
		if (!body.startsWith("{")) {
			LOGGER.severe("Fehler beim Parsen von JSONObject aus URL: " + urlString);
			LOGGER.severe("Empfangener Content (kein '{' am Anfang): " + truncateBody(body));
			throw new JSONException("A JSONObject text must start with '{'. Empfangen: " + truncateBody(body));
		}

		return new JSONObject(body);
	}

	/**
	 * Lädt JSON-Daten von der angegebenen URL und stellt sicher, dass stets ein 
	 * {@link JSONObject} mit den Feldern {@code "lastUpdate"}, {@code "lastBigUpdate"} 
	 * und {@code "playerDB"} zurückgegeben wird.
	 * <p>
	 * <b>Format-Flexibilität:</b>
	 * <ul>
	 *   <li>Ist die Quelle ein {@link JSONObject}, werden vorhandene Metadaten übernommen 
	 *       und {@code "playerDB"} bereitgestellt.</li>
	 *   <li>Ist die Quelle direkt ein {@link JSONArray}, wird dieses als {@code "playerDB"} 
	 *       eingebettet und die Datumsfelder automatisch anhand der Spieler-Einträge ermittelt.</li>
	 * </ul>
	 *
	 * @param urlString Die URL zur JSON-Ressource.
	 * @return Ein vereinheitlichtes {@link JSONObject}.
	 * @throws Exception Wenn die Anfrage fehlschlägt oder der Inhalt kein valides JSON darstellt.
	 */
	public static JSONObject loadJsonObjectWithPlayerArrayFromUrl(String urlString) throws Exception {
		HttpResponse<String> response = sendWithRetry(urlString);

		if (response.statusCode() != 200) {
			throw new IOException("HTTP error code: " + response.statusCode() + " - Body: " + truncateBody(response.body()));
		}

		String body = response.body() != null ? response.body().trim() : "";
		
		// Validierung: Muss entweder Object '{' oder Array '[' sein
		if (!body.startsWith("{") && !body.startsWith("[")) {
			LOGGER.severe("Empfangene Daten sind weder Object noch Array: " + truncateBody(body));
			throw new IllegalArgumentException("Empfangene Daten von URL " + urlString + " sind kein gültiges JSON.");
		}

		Object json = new JSONTokener(body).nextValue();
		JSONArray playerDB = null;
		JSONObject resultObj;

		// Fallunterscheidung nach Struktur der Rohdaten
		if (json instanceof JSONArray) {
			playerDB = (JSONArray) json;
			resultObj = new JSONObject();
			resultObj.put("playerDB", playerDB);
		} else {
			resultObj = (JSONObject) json;
			playerDB = resultObj.optJSONArray("playerDB");
			if (playerDB == null) {
				playerDB = new JSONArray();
				resultObj.put("playerDB", playerDB);
			}
		}

		// Fehlende Datumsstempel anhand der enthaltenen Spieler-Daten nachberechnen
		String ud = resultObj.optString("lastUpdate");
		if ((ud == null || ud.isBlank()) && playerDB.length() > 0) {
			ComunioDate date = PlayerHelper.getNewestDateByPath(playerDB, "data.lastUpdate");
			if (date != null) {
				resultObj.put("lastUpdate", date.toString());
			}
		}

		ud = resultObj.optString("lastBigUpdate");
		if ((ud == null || ud.isBlank()) && playerDB.length() > 0) {
			ComunioDate date = PlayerHelper.getNewestDateByPath(playerDB, "data.lastBigUpdate");
			if (date != null) {
				resultObj.put("lastBigUpdate", date.toString());
			}
		}

		return resultObj;
	}

	/**
	 * Hilfsmethode zum Einkürzen langer Response-Strings für Log-Ausgaben.
	 * Verhindert unleserlich große Log-Dateien, wenn HTML-Seiten empfangen werden.
	 *
	 * @param body Der ursprüngliche Server-Antworttext.
	 * @return Ein auf max. 200 Zeichen gekürzter String mit Auslassungspunkten.
	 */
	private static String truncateBody(String body) {
		if (body == null) return "null";
		return body.length() > 200 ? body.substring(0, 200) + "..." : body;
	}
}