package comunio.nas.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;

public class ClubMapper {
	
	private static final Logger LOGGER = LogManager.getLogger(ClubMapper.class);

	
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

	public static String getClubnameFromComunioId(String id,  Map<String, ClubObject> clubDb) {
		for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
			ClubObject clubo = entry.getValue();
			String dbId = clubo.getId();
			if (id.equals(dbId)) {
				return clubo.getName();
			}
		}
		return "";
	}

//	public static String getComunioIdFromName(String clubName, JSONArray clubsMapping) {
//		JSONObject club = getComunioClubJSONFromName(clubName, clubsMapping);
//		if (club != null && !club.isEmpty()) {
//			return club.getString("id");
//		}
//		return null;
//	}

	public static String getComunioIdFromName(String clubName, Map<String, ClubObject> clubDb) {
		if(clubName == null || clubName.isBlank() || clubDb == null || clubDb.isEmpty()) {
			LOGGER.warning("fehler bei den Inputdaten für das Clubmapping");
			return null;
		}
		
		String v1 = normalizeClubName(clubName);

		for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
			ClubObject clubo = entry.getValue();
			String v2 = normalizeClubName(clubo.getName());
			if (v1.equals(v2)) {
				return clubo.getId();
			}
		}
		return null;
	}
	
	public static ClubObject getComunioClubObjectFromName(String clubName, Map<String, ClubObject> clubDb) {
		if(clubName == null || clubName.isBlank() || clubDb == null || clubDb.isEmpty()) {
			LOGGER.warning("fehler bei den Inputdaten für das Clubmapping");
			return null;
		}
		
		String v1 = normalizeClubName(clubName);

		for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
			ClubObject clubo = entry.getValue();
			String v2 = normalizeClubName(clubo.getName());
			if (v1.equals(v2)) {
				return clubo;
			}
		}
		return null;
	}

	/**
	 * Verbesserte Vereinszuordnung mit hierarchischer Unterstützung für
	 * Zweitligavereine Versucht zuerst, den Spieler im Zweitverein zu finden, und
	 * fällt dann auf den Hauptverein zurück
	 * 
	 * @param clubName   Der zu suchende Vereinsname (z.B. "FC Bayern München II")
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
		// Alles zu Kleinbuchstaben und Whitespace normalisieren
		String cleaned = name.toLowerCase().trim().replaceAll("\\s+", " ");
		// In Wörter aufteilen
		String[] words = cleaned.split(" ");
		// Bekannte Präfixe (normalisiert durch Entfernen von Nicht-Buchstaben)
		Set<String> prefixes = new HashSet<>(Arrays.asList(
				"1", "fc", "sv", "tsg", "borussia", "sportclub", "sport-club", "sc", "spvgg", "vfl", "vfb", "bvb",
				"1fsv", "fsv", "bayer", "eintracht", "hertha", "fortuna", "hannover", "greuther",
				"dynamo", "erzgebirge", "arminia", "holstein", "hansa", "kfc", "msv", "viktoria",
				"werder", "1860", "rb", "tsv",
				// Compound prefixes (e.g. "1.FC" as single word)
				"1fc", "1fcv", "1fsv", "1sc", "1sv", "1vfl", "1bvb", "1tsg"));
		// Entferne Präfixe vom Anfang
		int startIndex = 0;
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			// Normalisiere Wort für Präfixabgleich (Behalte Buchstaben UND Ziffern)
			String normalizedWord = word.replaceAll("[^\\p{L}\\p{N}]", "");
			if (prefixes.contains(normalizedWord)) {
				startIndex = i + 1;
			} else {
				break;
			}
		}
		// Baue verbleibende Zeichenkette durch Zusammensetzen der Wörter
		StringBuilder sb = new StringBuilder();
		for (int i = startIndex; i < words.length; i++) {
			sb.append(words[i]);
		}
		// Entferne jegliche nicht-buchstaben Zeichen (Ziffern, Satzzeichen, etc.)
		String result = sb.toString().replaceAll("[^\\p{L}]+", "");
		return result;
	}

}