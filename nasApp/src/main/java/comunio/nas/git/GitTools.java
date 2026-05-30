package comunio.nas.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;

public class GitTools {
	
	private static final Logger LOGGER = LogManager.getLogger(GitTools.class);
	
	/**
	 * Wartet darauf, dass *alle* durch eine Upload‑Sequenz ausgelösten GitHub Actions
	 * Builds vollständig abgeschlossen sind. Diese Methode ist speziell für Szenarien
	 * gedacht, in denen mehrere Dateien nacheinander hochgeladen werden und dadurch
	 * mehrere Workflow-Runs entstehen, die sich gegenseitig abbrechen oder überholen.
	 *
	 * Funktionsweise:
	 * ---------------
	 * GitHub Actions startet bei jedem Upload einen neuen Workflow-Run. Läuft bereits
	 * ein älterer Run, wird dieser häufig automatisch abgebrochen ("cancelled").
	 * Dadurch entsteht eine Kette von Runs, die zeitlich dicht hintereinander liegen.
	 *
	 * Diese Methode stellt sicher, dass:
	 *
	 *   1. **Nur Runs berücksichtigt werden, die NACH dem Upload-Startzeitpunkt
	 *      (`uploadStartTime`) erstellt wurden.**
	 *      → Alte, bereits erfolgreiche Builds werden ignoriert.
	 *
	 *   2. **Immer der NEUESTE relevante Run überwacht wird.**
	 *      Die GitHub-API liefert Runs absteigend sortiert (neueste zuerst).
	 *      Es wird immer der erste Run verwendet, dessen `created_at` nach
	 *      `uploadStartTime` liegt.
	 *
	 *   3. **Erst dann zurückgegeben wird, wenn der neueste Run abgeschlossen ist
	 *      UND danach kein neuerer Run mehr gestartet wurde.**
	 *      → Dadurch wird garantiert, dass die gesamte Upload-Serie vollständig
	 *        verarbeitet wurde.
	 *
	 *   4. **Abgebrochene Runs korrekt behandelt werden.**
	 *      Ein "cancelled" Run bedeutet nicht zwingend einen Fehler – häufig wurde
	 *      einfach ein neuerer Run gestartet. Die Methode wartet dann automatisch
	 *      weiter auf den nächsten Run.
	 *
	 * Rückgabewerte:
	 * --------------
	 *   - `true`  → Alle relevanten Builds wurden erfolgreich abgeschlossen.
	 *   - `false` → Ein Build ist fehlgeschlagen ODER das Timeout wurde erreicht.
	 *
	 * Parameter:
	 * ----------
	 * @param commitSha
	 *        SHA des Commits, dient ausschließlich zu Logging-Zwecken.
	 *
	 * @param timeout
	 *        Maximale Wartezeit für den gesamten Prozess. Wird diese überschritten,
	 *        bricht die Methode ab und gibt `false` zurück.
	 *
	 * @param uploadStartTime
	 *        Zeitpunkt, an dem die Upload-Sequenz begonnen hat. Nur Runs, deren
	 *        `created_at` NACH diesem Zeitpunkt liegen, werden berücksichtigt.
	 *
	 * Ablaufübersicht:
	 * ----------------
	 *   - GitHub Actions Runs abfragen
	 *   - Neuesten Run nach `uploadStartTime` bestimmen
	 *   - Wenn Run noch läuft → warten
	 *   - Wenn Run abgeschlossen:
	 *         - bei Erfolg → prüfen, ob inzwischen ein neuerer Run gestartet wurde
	 *         - bei Fehler → sofort abbrechen
	 *   - Wenn derselbe Run zweimal erfolgreich abgeschlossen wurde → fertig
	 *
	 * Besonderheiten:
	 * ---------------
	 *   - Die Methode ist robust gegenüber:
	 *         • abgebrochenen Runs
	 *         • parallelen Runs
	 *         • Runs, die spät starten
	 *         • GitHub API Verzögerungen
	 *
	 *   - Sie eignet sich ideal für Upload-Skripte, die viele Dateien nacheinander
	 *     pushen und sicherstellen müssen, dass GitHub Pages oder CI/CD erst dann
	 *     fertig ist, wenn wirklich der letzte Build durchgelaufen ist.
	 *
	 * Beispiel:
	 * ---------
	 *   Instant start = Instant.now();
	 *   uploadFiles();
	 *   boolean ok = waitForAllGitHubBuilds(commitSha, Duration.ofMinutes(10), start);
	 *
	 *   if (ok) {
	 *       System.out.println("Alle Builds erfolgreich abgeschlossen.");
	 *   } else {
	 *       System.err.println("Builds fehlgeschlagen oder Timeout.");
	 *   }
	 */

	public static boolean waitForAllGitHubBuilds(String commitSha, Duration timeout, Instant uploadStartTime) {
	    String owner = GitData.REPO_OWNER;
	    String repo  = GitData.REPO_NAME;
	    String token = GitData.GITHUB_TOKEN;

	    String url = String.format("https://api.github.com/repos/%s/%s/actions/runs?per_page=20", owner, repo);
	    HttpClient client = HttpClient.newHttpClient();
	    Instant deadline = Instant.now().plus(timeout);

	    LOGGER.info("Warte auf *alle* GitHub Actions Builds nach Upload-Start: " + uploadStartTime);

	    JSONObject lastSeenRun = null;

	    while (Instant.now().isBefore(deadline)) {
	        try {
	            HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(url))
	                    .header("Authorization", "Bearer " + token)
	                    .header("Accept", "application/vnd.github.v3+json")
	                    .GET()
	                    .build();

	            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

	            if (response.statusCode() != 200) {
	                LOGGER.warning("GitHub API Fehler: " + response.statusCode());
	                TimeUnit.SECONDS.sleep(10);
	                continue;
	            }

	            JSONObject json = new JSONObject(response.body());
	            JSONArray runs = json.optJSONArray("workflow_runs");

	            if (runs == null || runs.length() == 0) {
	                LOGGER.info("Keine Runs gefunden. Warte...");
	                TimeUnit.SECONDS.sleep(10);
	                continue;
	            }

	            // NEUESTEN Run nach uploadStartTime finden
	            JSONObject newestRelevantRun = null;
	            for (int i = 0; i < runs.length(); i++) {
	                JSONObject run = runs.getJSONObject(i);
	                Instant created = Instant.parse(run.getString("created_at"));
	                if (created.isAfter(uploadStartTime)) {
	                    newestRelevantRun = run;
	                    break; // API liefert absteigend sortiert
	                }
	            }

	            if (newestRelevantRun == null) {
	                LOGGER.info("Noch kein Run nach Upload-Start. Warte...");
	                TimeUnit.SECONDS.sleep(10);
	                continue;
	            }

	            String status = newestRelevantRun.getString("status");
	            String conclusion = newestRelevantRun.optString("conclusion", "pending");
	            long runId = newestRelevantRun.getLong("id");

	            LOGGER.info("Prüfe Run " + runId + " | Status: " + status + " | Conclusion: " + conclusion);

	            // Wenn der Run noch läuft → warten
	            if (!"completed".equals(status)) {
	                TimeUnit.SECONDS.sleep(10);
	                continue;
	            }

	            // Wenn der Run fertig ist → prüfen, ob ein neuerer Run gestartet wurde
	            if ("success".equals(conclusion) || "neutral".equals(conclusion)) {

	                // Wenn wir diesen Run schon einmal gesehen haben → es kam KEIN neuerer Run mehr
	                if (lastSeenRun != null && lastSeenRun.getLong("id") == runId) {
	                    LOGGER.info("✓ Alle Builds abgeschlossen. Keine neuen Runs mehr.");
	                    return true;
	                }

	                // Diesen Run als "gesehen" markieren und erneut prüfen
	                lastSeenRun = newestRelevantRun;
	                LOGGER.info("Run erfolgreich, prüfe ob ein neuerer Run gestartet wurde...");
	                TimeUnit.SECONDS.sleep(10);
	                continue;
	            }

	            if ("failure".equals(conclusion)) {
	                LOGGER.severe("✗ Build fehlgeschlagen.");
	                return false;
	            }

	        } catch (Exception e) {
	            LOGGER.log(Level.WARNING, "Fehler beim Abfragen der GitHub Actions", e);
	            try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ignored) {}
	        }
	    }

	    LOGGER.severe("Timeout beim Warten auf alle Builds.");
	    return false;
	}

	
	/**
	 * Ruft den SHA des letzten Commits im Repository ab.
	 *
	 * @return SHA des letzten Commits oder null, wenn kein Commit existiert
	 * @throws Exception bei Netzwerkfehlern
	 */
	public static String getLatestCommitSha() throws Exception {
	    String urlString = String.format("https://api.github.com/repos/%s/%s/commits?per_page=1", GitData.REPO_OWNER, GitData.REPO_NAME);
	    
	    URL url = new URL(urlString);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("GET");
	    conn.setRequestProperty("Authorization", "token " + GitData.GITHUB_TOKEN);
	    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
	    conn.setRequestProperty("User-Agent", "ComunioFanApp");
	    
	    int responseCode = conn.getResponseCode();
	    if (responseCode == 200) {
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
	            StringBuilder response = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                response.append(line);
	            }
	            JSONArray commits = new JSONArray(response.toString());
	            if (commits.length() > 0) {
	                return commits.getJSONObject(0).getString("sha");
	            }
	        }
	    }
	    throw new IOException("Unexpected response code: " + responseCode + " beim Abrufen des letzten Commits");
	}

	
	
	/**
	 * Stellt eine einzelne Datei aus einem früheren Commit wieder her, indem der
	 * Inhalt aus dem angegebenen Commit-SHA geladen und anschließend über den
	 * GitHubUploader erneut hochgeladen wird.
	 *
	 * @param filePath   Pfad der Datei im Repository (z. B. "data/Spielerdatenbank.json")
	 * @param commitSha  Commit-SHA, aus dem die Datei wiederhergestellt werden soll
	 */
	public static void restoreFileFromCommit(String filePath, String commitSha) throws Exception {
	    String owner = GitData.REPO_OWNER;
	    String repo = GitData.REPO_NAME;
	    String token = GitData.GITHUB_TOKEN;

	    String url = String.format(
	        "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
	        owner, repo, filePath, commitSha
	    );

	    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
	    conn.setRequestMethod("GET");
	    conn.setRequestProperty("Authorization", "token " + token);
	    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
	    conn.setRequestProperty("User-Agent", "ComunioFanApp");

	    if (conn.getResponseCode() != 200) {
	        throw new IOException("Fehler beim Abrufen der Datei: HTTP " + conn.getResponseCode());
	    }

	    StringBuilder response = new StringBuilder();
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            response.append(line);
	        }
	    }

	    JSONObject json = new JSONObject(response.toString());
	    String encodedContent = json.getString("content");

	    byte[] decodedBytes = Base64.getMimeDecoder().decode(encodedContent);
	    String content = new String(decodedBytes);

	    // Wieder hochladen über deinen bestehenden Uploader
	    GitHubUploader.uploadToGitHub(filePath, content, "Restore " + filePath + " from commit " + commitSha);

	    System.out.println("Restored: " + filePath);
	}


	/**
	 * Stellt alle relevanten Datenbankdateien aus einem früheren Commit wieder her.
	 * Nutzt die in Urls.java definierten Git-Dateipfade und lädt jede Datei aus
	 * dem angegebenen Commit-SHA erneut hoch.
	 *
	 * @param commitSha Commit-SHA, aus dem alle Dateien wiederhergestellt werden sollen
	 */
	public static void restoreAllFilesFromCommit(String commitSha) throws Exception {
	    List<String> files = List.of(
	        Urls.PLAYER_DB_URL,
	        Urls.CLUB_DB_URL,
	        Urls.MARKET_VALUE_DB_URL,
	        Urls.NEWS_DB_URL,
	        Urls.USER_TO_PLAYER_URL,
	        Urls.USER_DB_URL,
	        Urls.NOTINLIGA_DB_URL,
	        Urls.TRANSFERMARKT_LIST,
	        Urls.MATCHDAYDATA_LIST,
	        Urls.LASTUPDATES_LIST,
	        Urls.INJURIES_DB_URL,
	        Urls.POINTS_DB_URL
	    );

	    for (String fileUrl : files) {
	        String filePath = Urls.getFilePathForGit(fileUrl);
	        restoreFileFromCommit(filePath, commitSha);
	    }
	}



}
