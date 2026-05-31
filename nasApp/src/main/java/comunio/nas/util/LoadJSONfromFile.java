package comunio.nas.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import comunio.nas.dataVariable.Urls;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.player.PlayerHelper;

public class LoadJSONfromFile {
	
	private static final Logger LOGGER = LogManager.getLogger(LoadJSONfromFile.class);
	/**
	 * Lädt ein JSONArray von einer gegebenen URL (z.B. GitHub Raw-Link).
	 * 
	 * @param urlString Die URL zur JSON-Datei.
	 * @return JSONArray mit den geladenen Daten.
	 * @throws Exception Bei Netzwerk- oder Parsingfehlern.
	 */
	public static JSONArray loadJsonArrayFromUrl(String urlString) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		// Cache-Buster: Timestamp hinzufügen, damit GitHub Raw immer die aktuelle Version liefert
		String urlWithCacheBuster = urlString + (urlString.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
		
		HttpRequest request = HttpRequest.newBuilder()//
				.uri(URI.create(urlWithCacheBuster))//
				.header("User-Agent", "Mozilla/5.0")//
				.GET()//
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 404) {
			GitHubUploader.uploadToGitHub(Urls.getFilePathForGit(urlString), new JSONObject().toString(), Urls.getFilename(urlString) + " - datei nichr vorhanden! Wird erstellt!");
			LOGGER.info(Urls.getFilename(urlString) + " - nicht auf GITHUB vorhanden. wurde erstellt!");
			return new JSONArray();
		} else if (response.statusCode() != 200) {
			throw new IOException("HTTP error code: " + response.statusCode());
		}
		return new JSONArray(response.body());
	}

	/**
	 * Lädt ein JSONArray von einer gegebenen URL (z.B. GitHub Raw-Link).
	 * 
	 * @param urlString Die URL zur JSON-Datei.
	 * @return JSONArray mit den geladenen Daten.
	 * @throws Exception Bei Netzwerk- oder Parsingfehlern.
	 */
	public static JSONObject loadJsonObjectFromUrl(String urlString) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		// Cache-Buster: Timestamp hinzufügen, damit GitHub Raw immer die aktuelle Version liefert
		String urlWithCacheBuster = urlString + (urlString.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
		
		HttpRequest request = HttpRequest.newBuilder()//
				.uri(URI.create(urlWithCacheBuster))//
				.header("User-Agent", "Mozilla/5.0")//
				.GET()//
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 404) {
			GitHubUploader.uploadToGitHub(Urls.getFilePathForGit(urlString), new JSONObject().toString(), Urls.getFilename(urlString) + " - datei nichr vorhanden! Wird erstellt!");
			LOGGER.info(Urls.getFilename(urlString) + " - nicht auf GITHUB vorhanden. wurde erstellt!");
			return new JSONObject();
		} else if (response.statusCode() != 200) {
			throw new IOException("HTTP error code: " + response.statusCode());
		}
		
		// DEBUG: Log den Response-Body bevor er zu JSONObject konvertiert wird
		String responseBody = response.body();
		// if (responseBody.contains("LastUpdates") || urlString.contains("LastUpdates")) {
		// 	LOGGER.info("DEBUG LoadJSONfromFile: URL = " + urlWithCacheBuster);
		// 	LOGGER.info("DEBUG LoadJSONfromFile: Response-Body = " + responseBody);
		// }
		
		return new JSONObject(responseBody);
	}

	/**
	 * Lädt JSON-Daten von der angegebenen URL und gibt immer ein JSONObject zurück,
	 * das die Felder "lastUpdate", "lastBigUpdate" und "playerDB" enthält.
	 * <p>
	 * Die zurückgegebene Struktur ist dabei wie folgt:
	 * <ul>
	 * <li>Wenn das ursprüngliche JSON ein JSONObject ist, werden "lastUpdate" und
	 * "lastBigUpdate" übernommen (sonst als null gesetzt) und das JSONArray von
	 * "playerDB" (oder ein leeres Array).</li>
	 * <li>Wenn das ursprüngliche JSON direkt als JSONArray vorliegt, wird dieses
	 * als "playerDB" eingetragen, während "lastUpdate" und "lastBigUpdate" auf null
	 * gesetzt werden.</li>
	 * </ul>
	 * <p>
	 * Dadurch erhält man stets einheitliche Daten zum Weiterverarbeiten, ungeachtet
	 * des konkreten JSON-Formats.
	 * 
	 * @param urlString URL der JSON-Ressource als String
	 * @return JSONObject mit den Feldern "lastUpdate" (String oder null),
	 *         "lastBigUpdate" (String oder null), und "playerDB" (JSONArray)
	 * @throws IOException              Wenn die HTTP-Anfrage fehlschlägt oder der
	 *                                  Statuscode kein 200 ist
	 * @throws InterruptedException     Wenn die Anfrage unterbrochen wird
	 * @throws IllegalArgumentException Wenn das empfangene JSON weder JSONObject
	 *                                  noch JSONArray ist
	 */
	public static JSONObject loadJsonObjectWithPlayerArrayFromUrl(String urlString) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		// Cache-Buster: Timestamp hinzufügen, damit GitHub Raw immer die aktuelle Version liefert
		String urlWithCacheBuster = urlString + (urlString.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
		
		HttpRequest request = HttpRequest.newBuilder()//
				.uri(URI.create(urlWithCacheBuster))//
				.header("User-Agent", "Mozilla/5.0")//
				.GET()//
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("HTTP error code: " + response.statusCode());
		}

		Object json = new JSONTokener(response.body()).nextValue();

		JSONArray playerDB = null;
		JSONObject resultObj = (JSONObject) json;

		// playerDB als JSONArray oder neues leeres JSONArray
		playerDB = resultObj.optJSONArray("playerDB");
		if (playerDB == null) {
			playerDB = new JSONArray();
		}

		String ud = resultObj.optString("lastUpdate");
		if ((ud == null || ud.isBlank()) && playerDB != null) {
			ComunioDate date = PlayerHelper.getNewestDateByPath(playerDB, "data.lastUpdate");
			;
			resultObj.put("lastUpdate", date.toString());
		}

		ud = resultObj.optString("lastBigUpdate");
		if ((ud == null || ud.isBlank()) && playerDB != null) {
			ComunioDate date = PlayerHelper.getNewestDateByPath(playerDB, "data.lastBigUpdate");
			resultObj.put("lastBigUpdate", date.toString());
		}

		return resultObj;
	}

}
