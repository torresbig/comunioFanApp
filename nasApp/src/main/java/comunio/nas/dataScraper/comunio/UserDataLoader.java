package comunio.nas.dataScraper.comunio;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataVariable.Urls;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.user.User;
import comunio.nas.objects.user.UserInfo;
import comunio.nas.util.HttpHeaderUtil;

public class UserDataLoader {

	// https://www.comunio.de/api/users/10966300
	private static final Logger LOGGER = LogManager.getLogger(UserDataLoader.class);

	public static void main(String[] args) {
		User obj = new User();
		obj.setId("10966300");
		fetchDataForUserJson(obj);
		obj = new User();
		obj.setId("5981249");
		fetchDataForUserJson(obj);

	}
	
	public static void fetchDataForAllUsers(Map<String, User> userMap) {
		if (userMap == null || userMap.isEmpty()) {
			LOGGER.warning("fetchDataForAllUsers: userDB ist leer oder null!");
			return;
		}
		int i = 0;
		for (Entry<String, User> entry : userMap.entrySet()) {
			i++;
			User userObject = entry.getValue();
			if (userObject == null) {
				LOGGER.warning("fetchDataForAllUsers: userObject an Index " + i + " ist null!");
				continue;
			}
			if(userObject.getUserInfo() == null)  {
				LOGGER.warning("fetchDataForAllUsers: userObject an Index " + i + " hat kein 'user'-Feld!");
				continue;
			}
			UserInfo userInfo = userObject.getUserInfo();
			if(userInfo.getId().equals("1")) {
				LOGGER.info("fetchDataForAllUsers: User mit ID=1 wird übersprungen (COMPUTER).");
				continue;
			}
			JSONObject userData = fetchDataForUserJson(userObject);
			if (userData == null) {
				LOGGER.warning("fetchDataForAllUsers: Fehler beim Abrufen der Userdaten für Index " + i);
			}
		}	
	}

	/**
	 * Holt Detail-Daten zu einem User von der Comunio-API und fügt sie in das bestehende userObject ein.
	 * Token-Handling bleibt wie in der bestehenden Infrastruktur.
	 *
	 * @param userObject Das JSONObject eines Users aus der userDB, das mindestens ein Feld "id" enthält.
	 * @return JSONObject mit den vom Server geholten Detaildaten oder null bei Fehler/nicht gefunden.
	 */
	public static JSONObject fetchDataForUserJson(User user) {
	    try {
	    	
	    	 if (user == null) {
		            LOGGER.warning("fetchDataForUserJson: Kein 'Userobject' vorhanden!");
		            return null;
		        }
	    	
	    	
	        String id = user.getId();
	        if (id.isBlank()) {
	            LOGGER.warning("fetchDataForUserJson: Kein 'id'-Feld im userObject vorhanden!");
	            return null;
	        }
	        if(id.equals("1")) {
	        	LOGGER.info("fetchDataForUserJson: User mit ID=1 wird übersprungen (COMPUTER).");
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
	            user.setTeamValue(teamValue);
	        }
	        
	        user.setPoints(userData.optInt("points", 0));
	        user.getUserInfo().setType(userData.optString("type", "BASIC"));
	        // Stammdaten

	        user.setFirstName(userData.optString("firstName", ""));
	        user.setLastName(userData.optString("lastName", ""));
	        user.getUserInfo().setLoginName(userData.optString("login", ""));
	        user.setRegistered(userData.optString("registered", ""));
//	        user.getUserInfo().setNewsCount(userData.optInt("newsCount", 0));
	        
	        LOGGER.info("fetchDataForUserJson: Userdaten für ID=" + id + " erfolgreich aktualisiert.");
	        return userData;

	    } catch (Exception e) {
	        LOGGER.warning("fetchDataForUserJson: Fehler beim Abrufen der Userdaten -> " + e.getMessage());
	        return null;
	    }
	}

	


}
