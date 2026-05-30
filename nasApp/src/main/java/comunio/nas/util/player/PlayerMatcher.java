package comunio.nas.util.player;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.util.ClubMapper;

import java.util.*;

/**
 * Findet einen Spieler in der Datenbank anhand von Namensvarianten und
 * Vereinsnamen mit robuster Fuzzy-Logik, angelehnt an die bewährte
 * Transfermarkt-Matching-Strategie.
 *
 * Diese Methode ist besonders tolerant gegenüber unterschiedlichen
 * Schreibweisen und prüft zuerst exakte Namens- UND Vereinsübereinstimmung,
 * dann fuzzy auf Name und Verein.
 */
public class PlayerMatcher {

	/**
	 * Findet einen Spieler in der Datenbank anhand von Namensvarianten und
	 * Vereinsnamen, indem die Vereins-ID aus der Datenbank in den Klartextnamen
	 * umgewandelt wird. Das Matching erfolgt robust: Zuerst wird der Verein
	 * verglichen (nach Normalisierung), dann werden alle Namensvarianten (exakt und
	 * fuzzy) verglichen.
	 * 
	 * @param playerDB      Die Spielerdatenbank als JSONArray
	 * @param playerName    Der Zielspieler-Name (z.B. aus Ligainsider)
	 * @param clubName      Vereinsname aus Ligainsider (Klartext)
	 * @param possibleNames Namensvarianten des Zielspielers (Set oder Liste)
	 * @return JSONObject des gefundenen Spielers oder null, falls kein Match
	 */
	public static JSONObject findPlayerByNameAndClub(JSONArray playerDB, JSONArray clubDB, String playerName, String clubName, Collection<String> possibleNames) {
		JSONObject match = null;
		boolean matchFound = false;
		JSONObject fuzzyMatch = null;

		// Normalisiere den Vereinsnamen aus der externen Quelle (z.B. Ligainsider)
		String normalizedRankingClub = normalizeClubName(clubName);

		for (int j = 0; j < playerDB.length() && !matchFound; j++) {
			JSONObject player = playerDB.getJSONObject(j);

			// Namensvarianten aus der Datenbank generieren
			Set<String> dbNameVariants = new HashSet<>();
			String dbName = player.optString("name", "");
			dbNameVariants.add(dbName.trim().toLowerCase());

			JSONObject data = player.optJSONObject("data");
			if (data != null) {
				JSONArray dbPossibleNames = data.optJSONArray("possibleNames");
				if (dbPossibleNames != null) {
					for (int k = 0; k < dbPossibleNames.length(); k++) {
						dbNameVariants.add(dbPossibleNames.getString(k).trim().toLowerCase());
					}
				}
			}

			// Vereins-ID in Klartextnamen umwandeln
			String dbClubId = data != null ? data.optString("verein", "") : "";
			String dbClubName = ClubMapper.getClubnameFromComunioId(dbClubId, clubDB);
			String normalizedDbClub = normalizeClubName(dbClubName);

			// Debug-Ausgaben für gezielte Analyse
//	        System.out.println("[DEBUG] Ligainsider-Verein: " + clubName + " -> " + normalizedRankingClub);
//	        System.out.println("[DEBUG] DB-Verein-ID: " + dbClubId + " -> " + dbClubName + " -> " + normalizedDbClub);

			// Vereinsvergleich: Nur fortfahren, wenn die Vereine übereinstimmen
			if (!normalizedDbClub.equals(normalizedRankingClub)) {
				continue;
			}

			// Namensvarianten vergleichen (exakt & fuzzy)
			outer: for (String pn : possibleNames) {
				for (String dbVar : dbNameVariants) {
					String pnNorm = pn.trim().toLowerCase();
					String dbNorm = dbVar.trim().toLowerCase();

					// Exakter Namensvergleich
					if (!pnNorm.isEmpty() && !dbNorm.isEmpty() && pnNorm.equals(dbNorm)) {
						match = player;
						matchFound = true;
						break outer;
					}
					// Fuzzy-Matching mit Levenshtein-Distanz
					int lev = org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance().apply(pnNorm, dbNorm);
					double similarity = 1.0 - ((double) lev / Math.max(pnNorm.length(), dbNorm.length()));
					if (similarity > 0.8 && fuzzyMatch == null) {
						fuzzyMatch = player;
					} else if (similarity > 0.8 && fuzzyMatch != null) {
						// Mehrdeutiger Fuzzy-Treffer: Kein eindeutiges Match
						fuzzyMatch = null;
						matchFound = true;
						break outer;
					}
				}
			}
		}
		// Falls kein exakter Treffer, aber ein eindeutiger Fuzzy-Treffer gefunden wurde
		if (match == null && fuzzyMatch != null) {
			match = fuzzyMatch;
		}
		return match;
	}
	
	public static JSONObject findPlayerByNameAndClub(JSONArray playerDB, JSONArray clubDB, String playerName, String clubName) {
		Set<String> possibleNames = generatePossibleNames(playerName);
		return findPlayerByNameAndClub(playerDB, clubDB, playerName, clubName, possibleNames);
	}

	/**
	 * Normalisiert Vereinsnamen für einen robusten Vergleich. Entfernt typische
	 * Präfixe/Suffixe und wandelt in Kleinbuchstaben um.
	 *
	 * @param name Vereinsname im Klartext
	 * @return Normalisierter Vereinsname
	 */
	private static String normalizeClubName(String name) {
		if (name == null)
			return "";
		String cleaned = name.toLowerCase();
		cleaned = cleaned.replaceAll("^(1\\.?\\s*fc|fc|sv|tsg|borussia|sportclub|sc|spvgg|vfl|bvb|1fsv|bayer|eintracht|hertha|fortuna|hannover|greuther|dynamo|erzgebirge|arminia|holstein|hansa|kfc|msv|viktoria|werder|schalke|1860)\\s*", "");
		cleaned = cleaned.replaceAll("[0-9\\-\\. ]+", "");
		return cleaned;
	}

	/**
	 * Erzeugt verschiedene Namensvarianten für ein robustes Matching. (Initial +
	 * Nachname, Nachname allein, Originalname in Kleinbuchstaben)
	 */
	public static Set<String> generatePossibleNames(String fullName) {
		Set<String> names = new HashSet<>();
		if (fullName == null || fullName.isBlank()) {
			return names;
		}
		fullName = fullName.trim();
		names.add(fullName.toLowerCase());
		String[] parts = fullName.split("\\s+");
		if (parts.length >= 2) {
			String initial = parts[0].substring(0, 1).toUpperCase() + ".";
			String lastName = parts[parts.length - 1];
			names.add((initial + " " + lastName).toLowerCase());
			names.add(lastName.toLowerCase());
		}
		return names;
	}

}
