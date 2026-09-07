package comunio.nas.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClubMapper {
	public static Map<String, String> getClubMapDbIdToTmName(JSONArray clubsMapping) {
		// Vereine-Mapping aufbauen
		Map<String, String> dbIdToTmName = new HashMap<>();
		for (int i = 0; i < clubsMapping.length(); i++) {
			JSONObject club = clubsMapping.getJSONObject(i);
			String dbId = club.optString("id", "");
			JSONObject tm = club.optJSONObject("transfermarktDoDe");
			if (tm != null) {
				String tmName = tm.optString("name", "").trim();
				dbIdToTmName.put(dbId, tmName);
			}
		}
		return dbIdToTmName;
	}

	public static Map<String, String> getClubMapDbIdToTmId(JSONArray clubsMapping) {
		// Vereine-Mapping aufbauen
		Map<String, String> dbIdToTmName = new HashMap<>();
		for (int i = 0; i < clubsMapping.length(); i++) {
			JSONObject club = clubsMapping.getJSONObject(i);
			String dbId = club.optString("id", "");
			JSONObject tm = club.optJSONObject("transfermarktDoDe");
			if (tm != null) {
				String tmName = String.valueOf(tm.optInt("id", 0));
				dbIdToTmName.put(dbId, tmName);
			}
		}
		return dbIdToTmName;
	}

	public static String getClubnameFromComunioId(String id, JSONArray clubsMapping) {
		for (int i = 0; i < clubsMapping.length(); i++) {
			JSONObject club = clubsMapping.getJSONObject(i);
			String dbId = club.optString("id", "");
			if (id.equals(dbId)) {
				return club.getString("name");
			}
		}
		return "";
	}

	public static String getComunioIdFromName(String clubName, JSONArray clubsMapping) {
		String v1 = normalizeClubName(clubName);

		for (int i = 0; i < clubsMapping.length(); i++) {
			JSONObject club = clubsMapping.getJSONObject(i);
			String dbId = club.optString("id", "");
			String v2 = normalizeClubName(club.optString("name", ""));
			if (v1.equals(v2)) {
				return dbId;
			}
		}
		return "";
	}
	
	/**
	 * Verbesserte Vereinszuordnung mit hierarchischer Unterstützung für Zweitligavereine
	 * Versucht zuerst, den Spieler im Zweitverein zu finden, und fällt dann auf den Hauptverein zurück
	 * 
	 * @param clubName Der zu suchende Vereinsname (z.B. "FC Bayern München II")
	 * @param tmClubName Der Transfermarkt-Vereinsname (z.B. "FC Bayern München")
	 * @return true wenn übereinstimmend, sonst false
	 */
	public static boolean vergleichClubNames(String clubName, String tmClubName) {
	    if (clubName != null && !clubName.isEmpty() && tmClubName != null && !tmClubName.isEmpty()) {
	        String v1 = normalizeClubName(clubName);
	        String v2 = normalizeClubName(tmClubName);
	        
	        // Exakte Übereinstimmung zuerst
	        if (v1.equals(v2)) {
	            return true;
	        }
	        
	        // Zweitligavereine: "FC Bayern München II" → "FC Bayern München"
	        if (v1.contains(" ") && !v2.contains(" ") && v1.startsWith(v2)) {
	            return true;
	        }
	        
	        // Umgekehrter Fall: "FC Bayern München" → "FC Bayern München II"
	        if (v2.contains(" ") && !v1.contains(" ") && v2.startsWith(v1)) {
	            return true;
	        }
	        
	        // Teilweise Übereinstimmung (z.B. "1.FC Heidenheim" → "1.FC Heidenheim 1846")
	        if (v1.equals(v2) || v1.contains(v2) || v2.contains(v1)) {
	            return true;
	        }
	        
	        return false;
	    }
		return false; 
	}

	/**
	 * Verbesserte Namensüberprüfung mit hierarchischer Unterstützung
	 * 
	 * @param clubName1 Der erste Vereinsname
	 * @param clubName2 Der zweite Vereinsname
	 * @return true wenn übereinstimmend, sonst false
	 */
	public static boolean enthaeltClubNames(String clubName1, String clubName2) {
	    if (clubName1 != null && !clubName1.isEmpty() && clubName2 != null && !clubName2.isEmpty()) {
	        String v1 = normalizeClubName(clubName1);
	        String v2 = normalizeClubName(clubName2);
	        
	        // Exakte Übereinstimmung zuerst
	        if (v1.equals(v2)) {
	            return true;
	        }
	        
	        // Zweitligavereine: "FC Bayern München II" → "FC Bayern München"
	        if (v1.contains(" ") && !v2.contains(" ") && v1.startsWith(v2)) {
	            return true;
	        }
	        
	        // Umgekehrter Fall: "FC Bayern München" → "FC Bayern München II"
	        if (v2.contains(" ") && !v1.contains(" ") && v2.startsWith(v1)) {
	            return true;
	        }
	        
	        // Teilweise Übereinstimmung
	        if (v1.equals(v2) || v1.contains(v2) || v2.contains(v1)) {
	            return true;
	        }
	        
	        return false;
	    }
		return false; 
	}

	public static String normalizeClubName(String name) {
		if (name == null)
			return "";
		// Alles zu Kleinbuchstaben
		String cleaned = name.toLowerCase();
		// Entferne Zahlen, Bindestriche, Punkte, Leerzeichen
				cleaned = cleaned.replaceAll("[0-9\\-\\. ]+", "");
		// Entferne typische Präfixe am Anfang, egal ob mit Punkt, Leerzeichen, etc.
		cleaned = cleaned.replaceAll("^(1\\.?\\s*fc|fc|sv|tsg|borussia|sportclub|sport-club|sc|spvgg|vfl|bvb|1fsv|bayer|eintracht|hertha|fortuna|hannover|greuther|dynamo|erzgebirge|arminia|holstein|hansa|kfc|msv|viktoria|werder|schalke|1860)\\s*", "");
		
		return cleaned;
	}

}