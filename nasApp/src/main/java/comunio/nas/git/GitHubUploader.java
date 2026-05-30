package comunio.nas.git;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.ComunioDataUpdater;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.LoadJSONfromFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GitHubUploader {
	private static final Logger LOGGER = LogManager.getLogger(GitHubUploader.class);

	// --- Datenbank-Uploads ---
	public static void uploadPlayerDatabase(JSONObject playerDBObject) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.PLAYER_DB_URL), playerDBObject.toString(2), "Update player database");
	}

	public static void uploadPlayerPoints(JSONObject pointsDB) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.POINTS_DB_URL), pointsDB.toString(2), "Update player points ");
	}

	public static void uploadMarketValueDatabase(JSONArray marketValueDB) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.MARKET_VALUE_DB_URL), marketValueDB.toString(2), "Update market values");
	}

	public static void uploadClubsDatabase(JSONArray clubDb) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.CLUB_DB_URL), clubDb.toString(2), "Update market values");
	}

	public static void uploadNews(JSONObject news) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.NEWS_DB_URL), news.toString(2), "Update news feed");
	}

	public static void uploadUserDatabase(JSONArray userMapToJsonArray) {
		if (ComunioDataUpdater.community != null && !ComunioDataUpdater.community.getId().isEmpty()) {
			uploadToGitHub(Urls.getFilePathForGit(Urls.USER_DB_URL), userMapToJsonArray.toString(2), "Update userDB feed");
		}else {
			LOGGER.info("userMapToJsonArray auf GitHub NICHT aktualisiert. Keine gültige Comunio-Community vorhanden.");
		}
	}

	public static void uploadTransfermarktListe(JSONArray transfermarktArray) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.TRANSFERMARKT_LIST), transfermarktArray.toString(2), "Update Transfermarkt-Liste feed");

	}

	public static void uploadMatchdayInfoListe(JSONObject matchdayInfoList) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.MATCHDAYDATA_LIST), matchdayInfoList.toString(2), "Update matchdayInfoList-Liste feed");

	}

	public static void uploadLastUpdateListe(JSONObject lastUpdates) {
		uploadToGitHub(Urls.getFilePathForGit(Urls.LASTUPDATES_LIST), lastUpdates.toString(2), "Update lastUpdates-Liste feed");

	}

	public static boolean mappingChanged = false;

	public static void uploadPlayerToUserMap(Map<String, String> map) {
		if (mappingChanged) {
			JSONArray arr = new JSONArray();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				JSONObject obj = new JSONObject();
				obj.put(entry.getKey(), entry.getValue());
				arr.put(obj);
			}
			if (ComunioDataUpdater.community != null && !ComunioDataUpdater.community.getId().isEmpty()) {
				uploadToGitHub(Urls.getFilePathForGit(Urls.USER_TO_PLAYER_URL), arr.toString(2), "Update PlayerToUserMap");
				LOGGER.info("PlayerToUserMap auf GitHub aktualisiert.");
			} else {
				LOGGER.info("PlayerToUserMap auf GitHub NICHT aktualisiert. Keine gültige Comunio-Community vorhanden.");
			}
			
		} else {
			LOGGER.info("PlayerToUserMap auf GitHub NICHT aktualisiert. keine Änderungen vorhanden");
		}
	}

	public static Map<String, String> downloadPlayerToUserMap(String url) {
		JSONArray arr = new JSONArray();
		try {
			arr = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.USER_TO_PLAYER_URL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			for (String playerId : obj.keySet()) {
				map.put(playerId, obj.getString(playerId));
			}
		}
		return map;
	}

	// --- GitHub-API-Upload ---
	public static void uploadToGitHub(String filePath, String content, String commitMessage) {
		try {
			String encoded = Base64.getEncoder().encodeToString(content.getBytes());
			String sha = getExistingFileSha(filePath);

			if (sha != null) {
				updateGitHubFile(filePath, encoded, sha, commitMessage);
				LOGGER.info("Updated GitHub file: " + filePath);
			} else {
				createGitHubFile(filePath, encoded, commitMessage);
				LOGGER.info("Created new GitHub file: " + filePath);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "GitHub upload failed: " + e.getMessage(), e);
		}
	}

	public static void uploadToGitHub(String urlString, JSONObject content) {
		uploadToGitHub(Urls.getFilePathForGit(urlString), content.toString(2), Urls.getFilename(urlString) + " updatet");

	}

	private static String getExistingFileSha(String filePath) throws Exception {
		String urlString = String.format("https://api.github.com/repos/%s/%s/contents/%s", GitData.REPO_OWNER, GitData.REPO_NAME, filePath);

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
				// DEBUG: SHA-Response anzeigen
//	            System.out.println("[DEBUG] SHA-Request-Response: " + response.toString());
				JSONObject json = new JSONObject(response.toString());
				return json.getString("sha");
			}
		} else if (responseCode == 404) {
			return null; // File doesn't exist yet
		}
		throw new IOException("Unexpected response code: " + responseCode);
	}

	private static void createGitHubFile(String filePath, String content, String commitMessage) throws Exception {
		updateOrCreateFile(filePath, content, null, commitMessage);
	}

	private static void updateGitHubFile(String filePath, String content, String sha, String commitMessage) throws Exception {
		updateOrCreateFile(filePath, content, sha, commitMessage);
	}

	private static void updateOrCreateFile(String filePath, String content, String sha, String commitMessage) throws Exception {
		String urlString = String.format("https://api.github.com/repos/%s/%s/contents/%s", GitData.REPO_OWNER, GitData.REPO_NAME, filePath);

		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("PUT");
		conn.setRequestProperty("Authorization", "token " + GitData.GITHUB_TOKEN);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
		conn.setRequestProperty("User-Agent", "ComunioFanApp");
		conn.setDoOutput(true);

		JSONObject jsonBody = new JSONObject();
		jsonBody.put("message", commitMessage);
		jsonBody.put("content", content);
		if (sha != null) {
			jsonBody.put("sha", sha);
		}

		try (OutputStream os = conn.getOutputStream()) {
			os.write(jsonBody.toString().getBytes());
		}

		int responseCode = conn.getResponseCode();
		InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
		}

		if (responseCode < 200 || responseCode > 299) {
			throw new IOException("GitHub API error: " + responseCode + " - " + response);
		}
	}

}
