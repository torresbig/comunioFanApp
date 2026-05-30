package comunio.nas.dataScraper.comunio;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import comunio.nas.objects.community.Community;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;

public class Login {
	public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
	private static final Logger LOGGER = LogManager.getLogger(Login.class);

	// Konstanten
	private static final String CONTENT_TYPE = "application/json;charset=UTF-8";
	private static final String BEARER = "Bearer ";
	private static final String LOGIN_URL = "https://www.comunio.de/api/login";
	private static final String REFRESH_URL = "https://www.comunio.de/api/refresh";
	private static final String API_URL = "https://www.comunio.de/api";

	private static String token = null;
	private static String refreshToken = null;
	private static Calendar tokenExpired = null;

	public static void login(String username, String password, Community community, User user) {
		LOGGER.info("Starte Login-Prozess...");
		if (shouldGetToken()) {
			try {
				setToken(getTokenFromServer(username, password));
				tokenExpired = Calendar.getInstance();
				LOGGER.info("Token erfolgreich geholt und gesetzt. Token läuft ab: " + tokenExpired.getTime());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Fehler beim Abrufen des Tokens: " + e.getMessage(), e);
			}
		} else {
			LOGGER.info("Token ist noch gültig, kein neuer Login nötig.");
		}

		try {
				updateSettingsFromServer(community, user);
		
			
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Einstellungen: " + e.getMessage(), e);
		}
	}



	public static void ensureValidToken(String username, String password) {
		LOGGER.fine("Überprüfe Token-Gültigkeit...");
		if (shouldGetToken()) {
			LOGGER.info("Token ungültig oder abgelaufen, führe Login erneut aus.");
			login(username, password, null, null);
		} else {
			LOGGER.fine("Token ist gültig.");
		}
	}

	private static boolean shouldGetToken() {
		if (getToken() == null) {
			LOGGER.fine("Kein Token vorhanden, muss neu geholt werden.");
			return true;
		}
		Calendar now = Calendar.getInstance();
		long diffInSeconds = (now.getTimeInMillis() - tokenExpired.getTimeInMillis()) / 1000;
		if (diffInSeconds >= 1800) {
			LOGGER.fine("Token abgelaufen (" + diffInSeconds + " Sekunden alt).");
			return true;
		}
		return false;
	}

	private static String getTokenFromServer(String username, String password) throws IOException {
		if (refreshToken != null) {
			try {
				LOGGER.info("Versuche Token mit Refresh-Token zu erneuern...");
				String newToken = refreshAccessToken();
				LOGGER.info("Refresh-Token erfolgreich benutzt.");
				return newToken;
			} catch (IOException e) {
				LOGGER.warning("Refresh-Token fehlgeschlagen, hole neuen Token mit Login.");
				refreshToken = null;
			}
		}

		LOGGER.info("Starte Login-Request an Comunio-API...");
		try {
			Response response = Jsoup.connect(LOGIN_URL)//
					.method(Connection.Method.POST)//
					.userAgent(USER_AGENT)//
					.header("Content-Type", CONTENT_TYPE)//
					.requestBody("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"tzoffset\":\"2\"}")//
					.ignoreContentType(true)//
					.execute();

			JSONObject jsonResponse = new JSONObject(response.body());
			refreshToken = jsonResponse.getString("refresh_token");
			LOGGER.info("Login erfolgreich. Access-Token und Refresh-Token erhalten.");
			return jsonResponse.getString("access_token");
		} catch (IOException e) {
			LOGGER.severe("Login fehlgeschlagen: " + e.getMessage());
			
			throw e;
		}
	}

	private static String refreshAccessToken() throws IOException {
		LOGGER.info("Sende Refresh-Token an API...");
		Response response = Jsoup.connect(REFRESH_URL)//
				.method(Connection.Method.POST)//
				.userAgent(USER_AGENT)//
				.header("Content-Type", CONTENT_TYPE)//
				.requestBody("{\"refresh_token\":\"" + refreshToken + "\"}")//
				.ignoreContentType(true)//
				.execute();

		JSONObject jsonResponse = new JSONObject(response.body());
		LOGGER.info("Neues Access-Token durch Refresh erhalten.");
		return jsonResponse.getString("access_token");
	}

	public static void updateSettingsFromServer(Community community, User user) throws IOException {
		try {
			LOGGER.info("Rufe aktuelle Einstellungen vom Server ab...");
			Document doc = Jsoup.connect(API_URL)//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", BEARER + getToken())//
					.ignoreContentType(true)//
					.get();

			JSONObject jsonO = new JSONObject(doc.body().text());
			JSONObject communityJson = jsonO.optJSONObject("community");
			if (communityJson != null && community != null) {
				community.setName(communityJson.getString("name"));
				community.setId(communityJson.getString("id"));
				community.setType(communityJson.getString("type"));
				JSONObject rules = communityJson.optJSONObject("rules");
				if (rules != null) {
					community.setSettings(rules.optJSONObject("items", new JSONObject()));
				}
			}
			JSONObject userJson = jsonO.optJSONObject("user");
			if (userJson != null && user != null) {
				user.updateFromJson(userJson);
			}

			LOGGER.fine("Einstellungen vom Server geladen: " + jsonO.keySet());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Einstellungen: " + e.getMessage(), e);
		}
	}

	public static String getToken() {
		return token;
	}

	public static void setToken(String token) {
		Login.token = token;
		tokenExpired = Calendar.getInstance();
		LOGGER.fine("Token gesetzt: " + (token != null ? token.substring(0, 10) + "..." : "null"));
	}
}
