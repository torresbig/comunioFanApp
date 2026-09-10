package comunio.nas.dataScraper.espn;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import comunio.nas.objects.helper.LogManager;

/**
 * Schlanker HTTP-Client für die öffentliche (inoffizielle) ESPN-Soccer-API.
 * <p>
 * Basis-URL: {@code https://site.api.espn.com/apis/site/v2/sports/soccer/ger.1}
 * <br>
 * Kein API-Key nötig. Es werden Browser-ähnliche Header gesetzt und eine kurze
 * Pause zwischen Requests eingehalten (Rate-Limit-freundlich).
 * <p>
 * Referenz: https://github.com/pseudo-r/Public-ESPN-API/blob/main/docs/sports/soccer.md
 */
public class EspnApiClient {

	private static final Logger LOGGER = LogManager.getLogger(EspnApiClient.class);

	/** Slug der 1. deutschen Bundesliga laut Public-ESPN-API-Doku */
	public static final String BUNDESLIGA = "ger.1";

	/** Basis-URL für alle Soccer-Endpunkte einer Liga */
	private static final String BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer/" + BUNDESLIGA;

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/**
	 * WICHTIG: Die ESPN-API (Akamai-WAF) blockiert Browser-ähnliche User-Agents
	 * (Chrome/Firefox) mit HTTP 403. Ein neutraler, nicht-browser User-Agent
	 * (wie der curl-Default) wird dagegen akzeptiert. Deshalb hier KEIN
	 * Browser-UA verwenden!
	 */
	private static final String USER_AGENT = "curl/8.5.0";

	/** Wartezeit in ms zwischen zwei Requests, um Rate-Limits zu vermeiden. */
	private static final long REQUEST_DELAY_MS = 800;

	/** Konstruktor privat – statische Nutzung. */
	private EspnApiClient() {
	}

	// =========================================================================
	// ÖFFENTLICHE ENDPUNKTE
	// =========================================================================

	/**
	 * Scoreboard: aktuelle/geplante/vergangene Spiele der Liga.
	 * 
	 * @return JSONObject mit "events"-Array (jedes Event = ein Spiel)
	 */
	public static JSONObject getScoreboard() throws IOException, InterruptedException {
		return getJson(BASE_URL + "/scoreboard");
	}

	/**
	 * Scoreboard für einen Datumsbereich (z. B. "20260910-20260917").
	 * 
	 * @param dates z. B. "20260910-20260917" (Format YYYYMMDD-YYYYMMDD)
	 */
	public static JSONObject getScoreboard(String dates) throws IOException, InterruptedException {
		return getJson(BASE_URL + "/scoreboard?dates=" + dates);
	}

	/**
	 * Tabelle der Liga (Standings).
	 */
	public static JSONObject getStandings() throws IOException, InterruptedException {
		return getJson(BASE_URL + "/standings");
	}

	/**
	 * Liste aller Teams der Liga.
	 */
	public static JSONObject getTeams() throws IOException, InterruptedException {
		return getJson(BASE_URL + "/teams");
	}

	/**
	 * Kader (Roster) eines Teams inkl. Saison-Statistiken der Spieler.
	 * 
	 * @param teamId ESPN-Team-ID (aus {@link #getTeams()})
	 */
	public static JSONObject getTeamRoster(String teamId) throws IOException, InterruptedException {
		return getJson(BASE_URL + "/teams/" + teamId + "/roster");
	}

	/**
	 * Detailliertes Spielerprofil eines Athleten.
	 * 
	 * @param athleteId ESPN-Athlete-ID (aus Roster)
	 */
	public static JSONObject getAthlete(String athleteId) throws IOException, InterruptedException {
		return getJson(BASE_URL + "/athletes/" + athleteId);
	}

	/**
	 * Detail-Summary zu einem einzelnen Spiel (Aufstellungen, Ereignisse, Karten).
	 * 
	 * @param eventId ESPN-Event-ID (aus Scoreboard)
	 */
	public static JSONObject getEventSummary(String eventId) throws IOException, InterruptedException {
		return getJson(BASE_URL + "/summary?event=" + eventId);
	}

	// =========================================================================
	// INTERN
	// =========================================================================

	/**
	 * Führt einen GET-Request aus und parst das Ergebnis als {@link JSONObject}.
	 * Wiederholt die Anfrage einmal, falls der Server vorübergehend fehlerhaft
	 * antwortet (z. B. 5xx).
	 */
	private static JSONObject getJson(String url) throws IOException, InterruptedException {
		LOGGER.info("ESPN-Request: " + url);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("User-Agent", USER_AGENT)
				.header("Accept", "application/json")
				.header("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
				.timeout(Duration.ofSeconds(20))
				.GET()
				.build();

		HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		int status = response.statusCode();
		String body = response.body();

		// Kurzer Retry bei vorübergehenden Serverfehlern
		if (status >= 500 && status < 600) {
			LOGGER.warning("ESPN HTTP " + status + " bei " + url + " – einmaliger Retry...");
			Thread.sleep(1500);
			response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			status = response.statusCode();
			body = response.body();
		}

		if (status != 200) {
			throw new IOException("ESPN HTTP " + status + " bei " + url + " - Body: " + truncate(body));
		}

		// Sanft schlafen, um das ESPN-Rate-Limit nicht zu reizen
		Thread.sleep(REQUEST_DELAY_MS);

		Object parsed = new JSONTokener(body).nextValue();
		if (parsed instanceof JSONObject) {
			return (JSONObject) parsed;
		}
		throw new IOException("ESPN lieferte kein JSONObject bei " + url + " - Body: " + truncate(body));
	}

	private static String truncate(String s) {
		if (s == null) {
			return "null";
		}
		return s.length() > 300 ? s.substring(0, 300) + "..." : s;
	}
}