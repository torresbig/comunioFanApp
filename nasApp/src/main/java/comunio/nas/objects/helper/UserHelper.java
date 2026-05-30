package comunio.nas.objects.helper;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserHelper {

	public static JSONObject findPlayerByComunioId(JSONArray userDB, String userId) {
		for (int i = 0; i < userDB.length(); i++) {
			JSONObject userData = userDB.getJSONObject(i);
			JSONObject user = userData.optJSONObject("user");
			if(user == null) {
				continue;
			}
			String dbId = convertIdToString(user.get("id"));
			if (dbId.equals(userId)) {
				return userData;
			}
		}
		return null;
	}
	
	/**
	 * Konvertiert ID-Objekte zu String, unabhängig vom Originaltyp.
	 * 
	 * @param idObj ID-Objekt (Integer, String, etc.)
	 * @return String-Repräsentation der ID
	 */
	public static String convertIdToString(Object idObj) {
		if (idObj instanceof Integer) {
			return String.valueOf(idObj);
		} else if (idObj instanceof String) {
			return (String) idObj;
		} else {
			return idObj.toString();
		}
	}
	
}
