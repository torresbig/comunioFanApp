package comunio.nas.util.player;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;

public class PlayerHelper {

	private static final Logger LOGGER = LogManager.getLogger(PlayerHelper.class);

	/**
	 * Sucht einen Spieler anhand der ID in einem JSONArray.
	 * 
	 * @param playerDB    JSONArray mit Spielern
	 * @param playerId    Die zu suchende Spieler-ID
	 * @param notInLigaDB die nicht in Liga Spieler DB (kann null sein!)
	 * @return JSONObject des Spielers oder null, falls nicht gefunden
	 */
	public static JSONObject findPlayerByComunioId(JSONArray playerDB, String playerId, JSONObject notInLigaDBObj) {

		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject playerObj = playerDB.getJSONObject(i);
			String dbId = convertIdToString(playerObj.get("id"));
			if (dbId.equals(playerId)) {
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
		JSONObject player = db.optJSONObject(playerId);
		if (player == null) {
			return null;
		}
		LOGGER.info("Player aus der NOT_IN_LIGA_DB geholt!");
		// Spieler gefunden, jetzt aus der DB entfernen, damit er nicht nochmal verwendet wird
		db.remove(playerId); // Einmalige Nutzung, danach entfernen
		
		return player;
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

	/**
	 * Durchsucht das JSONArray nach dem angegebenen Pfad (z.B.
	 * "data.attribute.ligainsiderRanking.lastUpdate") und gibt den ersten
	 * gefundenen Wert als String zurück. Gibt null zurück, wenn der Pfad nicht
	 * gefunden wurde.
	 */
	public static ComunioDate getFirstDateByPath(JSONArray jsonArray, String path) {
		if (jsonArray == null || path == null || path.isEmpty()) {
			return null;
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject eintrag = jsonArray.optJSONObject(i);
			ComunioDate result = getDateByPath(eintrag, path);
			if (result != null) { // Wenn ein gültiger Wert gefunden wurde, direkt zurückgeben
				return result;
			}
		}

		return null; // Kein gültiger Eintrag gefunden
	}

	/**
	 * Durchsucht das JSONArray nach dem angegebenen Pfad (z.B.
	 * "data.attribute.ligainsiderRanking.lastUpdate") und gibt den neusten
	 * gefundenen Wert als String zurück. Gibt null zurück, wenn der Pfad nicht
	 * gefunden wurde.
	 */
	public static ComunioDate getNewestDateByPath(JSONArray jsonArray, String path) {
		if (jsonArray == null || path == null || path.isEmpty()) {
			return null;
		}

		ComunioDate newestDate = null;
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject eintrag = jsonArray.optJSONObject(i);
			ComunioDate result = getDateByPath(eintrag, path);
			if (result != null) { // Wenn ein gültiger Wert gefunden wurde, direkt zurückgeben
				if (newestDate == null) {
					newestDate = result;
				} else if (result.after(newestDate)) {
					newestDate = result;
				}
			}
		}

		return newestDate; // Kein gültiger Eintrag gefunden
	}

	public static ComunioDate getDateByPath(JSONObject jsonObject, String path) {
		if (jsonObject == null || path == null || path.isEmpty()) {
			return null;
		}

		String[] keys = path.split("\\.");

		Object current = jsonObject;
		for (String key : keys) {
			if (!(current instanceof JSONObject)) {
				return null;
			}
			current = ((JSONObject) current).opt(key);
			if (current == null) {
				return null;
			}
		}

		try {
			return new ComunioDate(current.toString());
		} catch (Exception e) {
			// z.B. ParseException oder ähnliches, falls ComunioDate einen wirft
			return null;
		}
	}

	public static JSONObject createNewEmptyPlayerJSON(String playerID, String playerName) {
		JSONObject player = new JSONObject();
		player.put("id", playerID);
		player.put("name", playerName);

		// Data-Objekt mit Standardwerten
		JSONObject data = new JSONObject();
		data.put("position", "UNBEKANNT");
		data.put("verein", "UNBEKANNT");
		data.put("punkte", 0);
		data.put("spieltagspunkte", new JSONArray());
		data.put("lastPoints", 0);

		JSONObject spielerdaten = new JSONObject();
		spielerdaten.put("nebenpositionen", new JSONArray());
		spielerdaten.put("nationalspieler", "");
		spielerdaten.put("trikotNummer", 0);
		spielerdaten.put("nationalitaet", "");
		spielerdaten.put("laenderspielTore", 0);
		spielerdaten.put("hauptposition", "");
		spielerdaten.put("fuss", "");
		spielerdaten.put("groesse", "");
		spielerdaten.put("geburtstag", "");
		spielerdaten.put("laenderspiele", 0);
		spielerdaten.put("lastUpdate", JSONObject.NULL);
		data.put("spielerDaten", spielerdaten);

		data.put("isNotName", new JSONArray());
		data.put("punktekorrektur", new JSONArray());
		data.put("wert", 0);
		data.put("realWert", 0);
		data.put("withinSquad", false);
		data.put("onMarket", false);
		data.put("stats", new JSONObject().put("notenDurchschnitt", "0").put("gelbekarten", 0).put("totalPenalties", 0).put("ratedGames", 0).put("lastUpdate", new ComunioDate().toString()).put("playedGames", 0).put("totalGoals", 0).put("punkteDurchschnitt", "0").put("gelbrotekarten", 0).put("rotekarten", 0).put("manOfTheMatchAmount", 0));
		data.put("attribute", new JSONArray());
		data.put("possibleNames", new JSONArray());
		data.put("comunioStatus", new JSONObject().put("grund", "unbekannt").put("lastUpdate", new ComunioDate().toString()).put("seit", "unbekannt").put("bis", "unbekannt").put("details", "").put("historie", new JSONArray()).put("status", "AKTIV"));
		data.put("status", new JSONObject().put("grund", "unbekannt").put("lastUpdate", new ComunioDate().toString()).put("seit", "unbekannt").put("details", "").put("historie", new JSONArray()).put("status", "AKTIV"));

		player.put("data", data);
		return player;
	}

	/**
	 * Normalisiert Namen für Vergleiche:
	 * - Entfernt diakritische Zeichen
	 * - Konvertiert zu Kleinbuchstaben
	 * - Entfernt Sonderzeichen außer Leerzeichen
	 */
	public static String normalizeName(String name) {
	    if (name == null) return "";
	    try {
	        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
	                           .replaceAll("\\p{M}", "") // diakritische Zeichen entfernen
	                           .toLowerCase(Locale.ROOT)
	                           .replaceAll("[^a-z0-9\\s]", "");
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
	    if (searchName == null || fullName == null) return false;
	    
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
	 * Erweiterte Name-Übereinstimmung mit mehreren Strategien
	 */
	public static boolean namesMatchWithInitial(String name1, String name2) {
	    if (name1 == null || name2 == null) return false;
	    
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
	    if (maxLen == 0) return false;
	    
	    int lev = new LevenshteinDistance().apply(normName1, normName2);
	    double similarity = 1.0 - ((double) lev / maxLen);
	    
	    return similarity > 0.85; // Etwas lockerere Schwelle
	}

	/**
	 * Einfache Levenshtein-Distanz-Implementierung
	 */
	private static class LevenshteinDistance {
	    public int apply(String s, String t) {
	        if (s.length() == 0) return t.length();
	        if (t.length() == 0) return s.length();
	        
	        int[][] dp = new int[s.length() + 1][t.length() + 1];
	        
	        for (int i = 0; i <= s.length(); i++) dp[i][0] = i;
	        for (int j = 0; j <= t.length(); j++) dp[0][j] = j;
	        
	        for (int i = 1; i <= s.length(); i++) {
	            for (int j = 1; j <= t.length(); j++) {
	                int cost = (s.charAt(i-1) == t.charAt(j-1)) ? 0 : 1;
	                dp[i][j] = Math.min(Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), 
	                                  dp[i-1][j-1] + cost);
	            }
	        }
	        
	        return dp[s.length()][t.length()];
	    }
	}


	/**
	 * Hilfsmethode, um Namen in das Format "Initial Nachname" zu bringen. Entfernt
	 * Sonderzeichen und gibt Kleinbuchstaben zurück.
	 * Behandelt auch Initial-Präfixe: "A. Sambi Lokonga" wird wie "Sambi Lokonga" normalisiert.
	 *
	 * @param name Name, z. B. "Thomas Sborn" oder "A. Sambi Lokonga"
	 * @return Normalisierte Zeichenkette z. B. "t sborn" oder "s lokonga"
	 */
	private static String normalizeSimple(String name) {
		if (name == null || name.isBlank())
			return "";
		String[] parts = name.trim().split("\\s+");
		if (parts.length < 2) {
			return name.toLowerCase().replaceAll("[^a-z]", "");
		}
		// Wenn der erste Teil nur ein Buchstabe ist (z.B. "A." bei "A. Sambi Lokonga"),
		// ignoriere dieses Initial und nehm den zweiten Teil als Vornamen
		boolean isJustInitial = parts[0].replaceAll("[^a-z]", "").length() <= 1;
		
		String firstInitial;
		if (isJustInitial && parts.length >= 3) {
			// "A. Sambi Lokonga" -> "s lokonga" (zweites Teil als First-Name-Initial)
			firstInitial = parts[1].substring(0, 1).toLowerCase();
		} else if (isJustInitial && parts.length == 2) {
			// "A. Lokonga" -> nur ein Name nach Initial, nutze ihn als Ganzes
			firstInitial = parts[1].substring(0, 1).toLowerCase();
		} else {
			// Normal: "Thomas Sborn" -> "t sborn"
			firstInitial = parts[0].substring(0, 1).toLowerCase();
		}
		String lastName = parts[parts.length - 1].toLowerCase().replaceAll("[^a-z]", "");
		return firstInitial + " " + lastName;
	}
	
	

	/**
	 * Ermittelt den Besitzstand eines Spielers zu einem bestimmten Zeitpunkt
	 * (normalerweise: Spieltag-Start).
	 *
	 * @param playerId     Die playerId des Spielers.
	 * @param matchdayInfo Das MatchdayInfo-Objekt für den Spieltag.
	 * @param newsArray    JSONArray mit allen News- und Transfer-Events (sortiert
	 *                     nach Datum).
	 * @param playerToUser Aktuelle Map<PlayerID, UserID>.
	 * @return UserID des Besitzers am entsprechenden Spieltag, oder null wenn
	 *         unbekannt.
	 */
	public static String getOwnerAtMatchday(String playerId, MatchdayInfo matchdayInfo, JSONArray newsArray, Map<String, String> playerToUser) {
		// Referenz-Zeitpunkt (idealerweise: frühester oder spätester Kickoff des
		// Spieltags)
		ZonedDateTime matchdayStart = matchdayInfo.getEarliestKickoff();
		if (matchdayStart == null) {
			matchdayStart = matchdayInfo.getLatestKickoff();
		}
		if (matchdayStart == null) {
			// Fallback: aktuelles Datum
			matchdayStart = ZonedDateTime.now();
		}

		// DateTime Formatter, falls News-Datum als String (z.B. "17.06.2025")
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		String currentOwnerId = null;

		// Chronologisch alle Transfers zum Spieler bis zum Matchday-Datum durchgehen
		for (int i = 0; i < newsArray.length(); i++) {
			JSONObject newsDay = newsArray.getJSONObject(i);
			if (!newsDay.has("news"))
				continue;
			JSONArray items = newsDay.getJSONArray("news");
			for (int j = 0; j < items.length(); j++) {
				JSONObject event = items.getJSONObject(j);

				// Nur Transfers relevant!
				if (event.has("art") && "TRANSFER".equals(event.getString("art"))) {
					// Ist es der richtige Spieler?
					if (playerId.equals(event.optString("playerId"))) {
						// Datum des Transfers (Vergleichspunkt)
						String newsDateStr = event.optString("date");
						ZonedDateTime newsDate;
						try {
							// Format "17.06.2025" verarbeiten
							LocalDateTime localDate = LocalDateTime.parse(newsDateStr, formatter);
							// Default: Berlin-Zeit
							newsDate = localDate.atZone(matchdayStart.getZone());
						} catch (Exception e) {
							// Fallback: ignoriere fehlerhafte Datumsangaben
							continue;
						}

						if (!newsDate.isAfter(matchdayStart)) {
							// Besitzerwechsel vor/nach dem Spieltag -> setze neuen Besitzer
							JSONObject transferDetails;
							try {
								transferDetails = new JSONObject(event.optString("text"));
							} catch (Exception e) {
								continue; // Fehlerhaftes Transfer-Details-Format
							}
							currentOwnerId = transferDetails.optString("buyerId", null);
						}
					}
				}
			}
		}

		// Wenn Transfers gefunden wurden, wurde Besitzer angepasst
		if (currentOwnerId != null) {
			return currentOwnerId;
		}

		// Falls kein Transfer, den aktuellen Besitzstand nutzen
		return playerToUser.getOrDefault(playerId, null);
	}

}
