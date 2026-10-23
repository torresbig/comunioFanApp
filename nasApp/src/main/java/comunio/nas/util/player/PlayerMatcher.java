package comunio.nas.util.player;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.ClubMapper;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Logger;

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

	private static final Logger LOGGER = LogManager.getLogger(PlayerMatcher.class);

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
	public static JSONObject findPlayerByNameAndClub(JSONArray playerDB, Map<String, ClubObject> clubDb, String clubName, Collection<String> possibleNames) {
		if (playerDB == null || playerDB.length() == 0 || possibleNames == null || possibleNames.isEmpty()) {
			return null;
		}
		if (clubName == null || clubName.isBlank() || clubDb == null || clubDb.isEmpty()) {
			LOGGER.warning("fehler bei den Inputdaten für das Clubmapping");
			return null;
		}

		String clubId = ClubMapper.getComunioIdFromName(clubName, clubDb);
		if (clubId == null || clubId.isBlank()) {
			return null;
		}

		List<ScoredPlayer> scoredCandidates = new ArrayList<>();
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");
			if (data == null || !clubId.equals(data.optString("verein", ""))) {
				continue;
			}

			int score = scorePlayerNameMatch(player, possibleNames);
			if (score > 0) {
				scoredCandidates.add(new ScoredPlayer(player, score));
			}
		}

		if (scoredCandidates.isEmpty()) {
			return fallbackMatchByClubNameDatabase(playerDB, clubId, possibleNames);
		}

		scoredCandidates.sort((a, b) -> Integer.compare(b.score, a.score));
		ScoredPlayer best = scoredCandidates.get(0);
		if (best.score >= 90) {
			return best.player;
		}
		if (best.score >= 60) {
			String bestSurname = extractLastName(normalizeForCompare(best.player.optString("name", "")));
			if (!bestSurname.isEmpty() && countPlayersWithSurnameInClub(playerDB, clubId, bestSurname) <= 1) {
				return best.player;
			}
		}

		JSONObject fallback = fallbackMatchByClubNameDatabase(playerDB, clubId, possibleNames);
		if (fallback != null) {
			return fallback;
		}
		return null;
	}

	private static final class ScoredPlayer {
		private final JSONObject player;
		private final int score;

		private ScoredPlayer(JSONObject player, int score) {
			this.player = player;
			this.score = score;
		}
	}

	private static int countPlayersWithSurnameInClub(JSONArray playerDB, String clubId, String surname) {
		if (playerDB == null || surname == null || surname.isBlank()) {
			return 0;
		}
		String normalizedSurname = normalizeForCompare(surname);
		if (normalizedSurname.isEmpty()) {
			return 0;
		}

		int count = 0;
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");
			if (data == null || !clubId.equals(data.optString("verein", ""))) {
				continue;
			}
			String normalizedPlayer = normalizeForCompare(player.optString("name", ""));
			if (normalizedPlayer.isEmpty()) {
				continue;
			}
			if (normalizedSurname.equals(extractLastName(normalizedPlayer))) {
				count++;
			}
		}
		return count;
	}

	private static int scorePlayerNameMatch(JSONObject player, Collection<String> possibleNames) {
		Set<String> dbVariants = new HashSet<>();
		addNameVariants(dbVariants, player.optString("name", ""));

		JSONObject data = player.optJSONObject("data");
		if (data != null) {
			JSONArray dbPossibleNames = data.optJSONArray("possibleNames");
			if (dbPossibleNames != null) {
				for (int i = 0; i < dbPossibleNames.length(); i++) {
					addNameVariants(dbVariants, dbPossibleNames.optString(i, ""));
				}
			}
		}

		int bestScore = 0;
		for (String externalName : possibleNames) {
			String extNorm = normalizeForCompare(externalName);
			if (extNorm.isEmpty()) {
				continue;
			}
			for (String dbVar : dbVariants) {
				String dbNorm = normalizeForCompare(dbVar);
				if (dbNorm.isEmpty()) {
					continue;
				}
				int score = scoreNamePair(extNorm, dbNorm);
				if (score > bestScore) {
					bestScore = score;
				}
			}
		}
		return bestScore;
	}

	public static JSONObject findPlayerByNameAndClub(JSONArray playerDB, Map<String, ClubObject> clubDb, String clubName, String name) {
		Collection<String> possibleNames = generatePossibleNames(name);
		return findPlayerByNameAndClub(playerDB, clubDb, clubName, possibleNames);
	}

	private static int scoreNamePair(String left, String right) {
		if (left == null || right == null || left.isBlank() || right.isBlank()) {
			return 0;
		}
		if (left.equals(right)) {
			return 100;
		}
		String leftLast = extractLastName(left);
		String rightLast = extractLastName(right);
		String leftFirst = extractFirstName(left);
		String rightFirst = extractFirstName(right);

		if (!leftLast.isEmpty() && !rightLast.isEmpty() && leftLast.equals(rightLast)) {
			if (!leftFirst.isEmpty() && !rightFirst.isEmpty() && leftFirst.equals(rightFirst)) {
				return 96;
			}
			if (!leftFirst.isEmpty() && !rightFirst.isEmpty() && leftFirst.charAt(0) == rightFirst.charAt(0)) {
				return 85;
			}
			if (isLastNameOnly(left) || isLastNameOnly(right)) {
				return 60;
			}
			return 70;
		}

		if (left.contains(right) || right.contains(left)) {
			if (isLastNameOnly(left) || isLastNameOnly(right)) {
				return 60;
			}
			return 90;
		}

		if (left.length() < 3 || right.length() < 3) {
			return 0;
		}
		int lev = org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance().apply(left, right);
		double similarity = 1.0 - ((double) lev / Math.max(left.length(), right.length()));
		if (similarity >= 0.92) {
			return 75;
		}
		if (similarity >= 0.82) {
			return 65;
		}
		return 0;
	}

	private static boolean isLastNameOnly(String value) {
		String[] parts = value.trim().split("\\s+");
		return parts.length == 1 && !parts[0].isEmpty();
	}

	private static String extractFirstName(String name) {
		String[] parts = name.trim().split("\\s+");
		if (parts.length == 0) {
			return "";
		}
		String first = parts[0];
		if (first.endsWith(".")) {
			return first.substring(0, first.length() - 1);
		}
		return first;
	}

	private static String extractLastName(String name) {
		String[] parts = name.trim().split("\\s+");
		if (parts.length == 0) {
			return "";
		}
		String last = parts[parts.length - 1];
		return last.replace(".", "");
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
		String cleaned = fullName.trim().replace("-", " ").replace("–", " ").replace("—", " ").replace(".", " ");
		cleaned = cleaned.replaceAll("\\s+", " ").trim();
		names.add(normalizeName(cleaned));
		names.add(cleaned.toLowerCase(Locale.ROOT));
		String[] parts = cleaned.split("\\s+");
		if (parts.length >= 2) {
			String first = parts[0];
			String lastName = parts[parts.length - 1];
			names.add(normalizeForCompare(first + " " + lastName));
			names.add(normalizeForCompare(first.substring(0, 1) + " " + lastName));
			names.add(normalizeForCompare(first.substring(0, 1) + ". " + lastName));
			names.add(normalizeForCompare(lastName));
		}
		return names;
	}

	private static void addNameVariants(Set<String> nameVariants, String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		String cleaned = name.trim().replace("-", " ").replace("–", " ").replace("—", " ").replace(".", " ").replaceAll("\\s+", " ").trim();
		if (cleaned.isEmpty()) {
			return;
		}
		nameVariants.add(normalizeForCompare(cleaned));
		nameVariants.add(cleaned.toLowerCase(Locale.ROOT));
		String[] parts = cleaned.split("\\s+");
		if (parts.length >= 2) {
			String first = parts[0];
			String lastName = parts[parts.length - 1];
			nameVariants.add(normalizeForCompare(first + " " + lastName));
			nameVariants.add(normalizeForCompare(first.substring(0, 1) + " " + lastName));
			nameVariants.add(normalizeForCompare(first.substring(0, 1) + ". " + lastName));
			nameVariants.add(normalizeForCompare(lastName));
		}
	}

	private static String normalizeForCompare(String value) {
		if (value == null) {
			return "";
		}
		String normalized = normalizeName(value);
		if (!normalized.isEmpty()) {
			return normalized;
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Normalisiert Namen für Vergleiche: - Entfernt diakritische Zeichen -
	 * Konvertiert zu Kleinbuchstaben - Entfernt Sonderzeichen außer Leerzeichen
	 */
	public static String normalizeName(String name) {
		if (name == null)
			return "";
		try {
			String normalized = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
					.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", "");
			return normalized.trim();
		} catch (Exception e) {
			return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", "").trim();
		}
	}

	/**
	 * Prüft ob ein Suchname als Teil des vollständigen Namens vorkommt
	 * Berücksichtigt auch diakritische Zeichen
	 */
	public static boolean containsSubstringMatch(String searchName, String fullName) {
		if (searchName == null || fullName == null)
			return false;

		// Direkter Teil-String-Vergleich
		if (fullName.contains(searchName)) {
			return true;
		}

		// Umgekehrt prüfen (selten nötig, aber sicherheitshalber)
		if (searchName.contains(fullName)) {
			return true;
		}

		return false;
	}

	/**
	 * Erweiterte Name-Übereinstimmung mit mehreren Strategien Verbessert für: -
	 * Namen mit Initialen (z.B. "R. Adam" → "Raif Adam") -
	 * Transliterationsunterschiede (z.B. "İnanoğlu" → "Inanoglu") - Flexiblere
	 * Nachnamensübereinstimmung für Zweitligavereine
	 */
	public static boolean namesMatchWithInitial(String name1, String name2) {
		if (name1 == null || name2 == null)
			return false;

		String normName1 = normalizeName(name1);
		String normName2 = normalizeName(name2);

		// Direkte Übereinstimmung
		if (normName1.equals(normName2)) {
			return true;
		}

		// Teil-String-Übereinstimmung
		if (containsSubstringMatch(normName1, normName2)) {
			return true;
		}

		// Initial-basierter Vergleich
		String[] parts1 = normName1.split("\\s+");
		String[] parts2 = normName2.split("\\s+");

		if (parts1.length >= 2 && parts2.length >= 2) {
			// Beide Namen haben mindestens Vornamen + Nachnamen
			String lastName1 = parts1[parts1.length - 1];
			String lastName2 = parts2[parts2.length - 1];

			String firstInitial1 = parts1[0].substring(0, 1);
			String firstInitial2 = parts2[0].substring(0, 1);

			// Gleicher Nachname + gleiche erste Initial
			if (lastName1.equals(lastName2) && firstInitial1.equals(firstInitial2)) {
				return true;
			}

			// Nur gleicher Nachname (letzter Ausweg)
			if (lastName1.equals(lastName2)) {
				return true; // Wird später durch Vereins-Check weiter eingeschränkt
			}
		}

		// Levenshtein-Distanz für ähnliche Namen
		int maxLen = Math.max(normName1.length(), normName2.length());
		if (maxLen == 0)
			return false;

		int lev = new LevenshteinDistance().apply(normName1, normName2);
		double similarity = 1.0 - ((double) lev / maxLen);

		// Etwas lockerere Schwelle für bessere Trefferquote
		return similarity > 0.85;
	}

	/**
	 * Einfache Levenshtein-Distanz-Implementierung
	 */
	private static class LevenshteinDistance {
		public int apply(String s, String t) {
			if (s.length() == 0)
				return t.length();
			if (t.length() == 0)
				return s.length();

			int[][] dp = new int[s.length() + 1][t.length() + 1];

			for (int i = 0; i <= s.length(); i++)
				dp[i][0] = i;
			for (int j = 0; j <= t.length(); j++)
				dp[0][j] = j;

			for (int i = 1; i <= s.length(); i++) {
				for (int j = 1; j <= t.length(); j++) {
					int cost = (s.charAt(i - 1) == t.charAt(j - 1)) ? 0 : 1;
					dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
				}
			}

			return dp[s.length()][t.length()];
		}
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
		} else if (idObj instanceof Double) {
			return String.valueOf(idObj);
		} else if (idObj == null) {
			return "";
		} else {
			return idObj.toString();
		}
	}
	
	/**
	 * Sucht einen Spieler anhand der ID in einem JSONArray.
	 * 
	 * @param playerDB    JSONArray mit Spielern
	 * @param playerId    Die zu suchende Spieler-ID
	 * @param notInLigaDB die nicht in Liga Spieler DB (kann null sein!)
	 * @return JSONObject des Spielers oder null, falls nicht gefunden
	 */
	public static JSONObject findPlayerByComunioId(JSONArray playerDB, Object playerId, JSONObject notInLigaDBObj) {
		String id = convertIdToString(playerId);

		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject playerObj = playerDB.getJSONObject(i);
			String dbId = convertIdToString(playerObj.get("id"));

			if (dbId.equals(id)) {
				return playerObj;
			}
		}

		if (notInLigaDBObj == null) {
			return null;
		}

		JSONObject db = notInLigaDBObj.optJSONObject("db");
		if (db == null) {
			return null;
		}
		JSONObject player = db.optJSONObject(id);
		if (player == null) {
			return null;
		}

		JSONObject data = player.getJSONObject("data");
		data.put("retired", false);

		LOGGER.info("Player aus der NOT_IN_LIGA_DB geholt!");
		db.remove(id);

		return player;
	}

	private static JSONObject fallbackMatchByClubNameDatabase(JSONArray playerDB, String clubId, Collection<String> possibleNames) {
		if (playerDB == null || playerDB.length() == 0 || possibleNames == null || possibleNames.isEmpty()) {
			return null;
		}
		Set<String> externalNames = new HashSet<>();
		for (String name : possibleNames) {
			if (name == null || name.isBlank()) {
				continue;
			}
			externalNames.add(normalizeForCompare(name));
			externalNames.addAll(generatePossibleNames(name));
		}
		if (externalNames.isEmpty()) {
			return null;
		}

		Map<String, List<JSONObject>> surnameCandidates = new HashMap<>();
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");
			if (data == null || !clubId.equals(data.optString("verein", ""))) {
				continue;
			}
			String candidateName = player.optString("name", "");
			String surname = extractLastName(normalizeForCompare(candidateName));
			if (!surname.isBlank()) {
				surnameCandidates.computeIfAbsent(surname, k -> new ArrayList<>()).add(player);
			}
		}

		for (String externalName : possibleNames) {
			if (externalName == null || externalName.isBlank()) {
				continue;
			}
			String normalizedExternal = normalizeForCompare(externalName);
			String externalSurname = extractLastName(normalizedExternal);
			if (!externalSurname.isBlank()) {
				List<JSONObject> bySurname = surnameCandidates.get(externalSurname);
				if (bySurname != null && bySurname.size() == 1) {
					JSONObject onlyMatch = bySurname.get(0);
					if (scorePlayerNameMatch(onlyMatch, possibleNames) > 0) {
						return onlyMatch;
					}
				}
			}
			for (int i = 0; i < playerDB.length(); i++) {
				JSONObject player = playerDB.getJSONObject(i);
				JSONObject data = player.optJSONObject("data");
				if (data == null || !clubId.equals(data.optString("verein", ""))) {
					continue;
				}
				if (scorePlayerNameMatch(player, Set.of(externalName)) > 0) {
					return player;
				}
			}
		}
		return null;
	}

}