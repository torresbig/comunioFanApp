package comunio.nas.dataScraper.comunio;

import java.util.logging.Logger;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;

public class UserDataLoader {

	// https://www.comunio.de/api/users/10966300
	private static final Logger LOGGER = LogManager.getLogger(UserDataLoader.class);

	public static void main(String[] args) {
		JSONObject obj = new JSONObject();
		obj.put("id", "10966300");
		fetchDataForUserJson(obj);
		obj.clear();
		obj.put("id", "5981249");
		fetchDataForUserJson(obj);

	}

	/**
	 * Holt Detail-Daten zu einem User von der Comunio-API und fügt sie in das bestehende userObject ein.
	 * Token-Handling bleibt wie in der bestehenden Infrastruktur.
	 *
	 * @param userObject Das JSONObject eines Users aus der userDB, das mindestens ein Feld "id" enthält.
	 * @return JSONObject mit den vom Server geholten Detaildaten oder null bei Fehler/nicht gefunden.
	 */
	public static JSONObject fetchDataForUserJson(JSONObject userObject) {
	    try {
	    	JSONObject user = userObject.optJSONObject("user", new JSONObject());
	    	 if (user == null) {
		            LOGGER.warning("fetchDataForUserJson: Kein 'Userobject' vorhanden!");
		            return null;
		        }
	    	
	    	
	        String id = user.optString("id", "");
	        if (id.isBlank()) {
	            LOGGER.warning("fetchDataForUserJson: Kein 'id'-Feld im userObject vorhanden!");
	            return null;
	        }

	        String url = Urls.COM_USERDATA + id;

	        // API-Request mit Login/Token
	        Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
	        String jsonResponse = Jsoup.connect(url)
	                .userAgent(HttpHeaderUtil.getRandomUserAgent())
	                .header("Accept", "application/json, text/plain, */*")
	                .header("Authorization", "Bearer " + Login.getToken())
	                .header("Accept-Encoding", "gzip, deflate, br, zstd")
	                .header("Accept-Language", "de-DE,en-EN;q=0.9")
	                .header("x-timezone", "Europe/Berlin")
	                .ignoreContentType(true)
	                .execute()
	                .body();

	        JSONObject userData = new JSONObject(jsonResponse);

	        // =====================
	        // FELDER INS USER-OBJEKT EINTRAGEN
	        // =====================
	        // teamValue & points
	        int teamValue = userData.optInt("teamValue", 0);
	        if (teamValue > 0) {
	            userObject.put("teamValue", teamValue);
	        }
	        userObject.put("points", userData.optInt("points", 0));
	        userObject.put("type", userData.optString("type", "BASIC"));
	        // Stammdaten
	        putIfPresent(user, "firstName", userData);
	        putIfPresent(user, "lastName", userData);
	        putIfPresent(user, "login", userData);
	        putIfPresent(user, "registered", userData);
	        putIfPresent(user, "newsCount", userData);

	        LOGGER.info("fetchDataForUserJson: Userdaten für ID=" + id + " erfolgreich aktualisiert.");
	        return userData;

	    } catch (Exception e) {
	        LOGGER.warning("fetchDataForUserJson: Fehler beim Abrufen der Userdaten -> " + e.getMessage());
	        return null;
	    }
	}

	/**
	 * Hilfsfunktion: Kopiert ein Feld aus source in target, wenn vorhanden.
	 */
	private static void putIfPresent(JSONObject target, String key, JSONObject source) {
	    if (source.has(key)) {
	        target.put(key, source.opt(key));
	    }
	}


}
