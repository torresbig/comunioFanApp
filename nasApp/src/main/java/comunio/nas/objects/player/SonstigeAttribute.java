package comunio.nas.objects.player;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.orga.ComunioDate;

import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility-Klasse für das Setzen und Verwalten von sonstigen Spielerattributen
 * in einer JSON-basierten Spieler-Datenbank.
 * 
 * Diese Klasse analysiert alle Spieler und setzt verschiedene Attribute wie: -
 * Teuerster Spieler - Spieler mit den meisten Punkten - Spieler mit den
 * häufigsten Transfers - Beste/schlechteste Durchschnittsnoten
 */
public class SonstigeAttribute {

	private static final Logger LOGGER = Logger.getLogger(SonstigeAttribute.class.getName());

	/**
	 * Setzt Spielerattribute für Performance-bezogene Statistiken.
	 * 
	 * @param playerDBObject Das JSON-Objekt der Spielerdatenbank
	 */
	public static void setSpielerAttributePerformance(JSONObject playerDBObject, MatchdayInfo currentMatchdayInfo, NewsManager newsManager) {
		LOGGER.info("Starte Berechnung der Spielerattribute...");

		// Lösche alle bestehenden SonstigeInfos-Attribute
		deleteAttributeFromAllPlayers(playerDBObject, "SonstigeInfos");

		int spieltag = currentMatchdayInfo.getCurrentMatchday();

		try {
			JSONArray playerDB = playerDBObject.getJSONArray("playerDB");
			List<JSONObject> allPlayers = jsonArrayToList(playerDB);

			// Setze verschiedene Attribute basierend auf Maximalwerten
			findAndSetMaxAttribute(allPlayers, "wert", "teuerster Spieler", spieltag);
			findAndSetMaxAttribute(allPlayers, "realWert", "teuerster Spieler (Realwert)", spieltag);
			findAndSetMaxAttribute(allPlayers, "punkte", "meisten Punkte", spieltag);
			findAndSetMaxAttribute(allPlayers, "lastSeasonPoints", "meisten Punkte letzte Saison", spieltag);

			// Finde und setze Transfer-Attribute
			findMostTransfers(allPlayers, newsManager);

			LOGGER.info("Spielerattribute erfolgreich gesetzt.");

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Setzen der Spielerattribute", e);
		}
	}

	/**
	 * Löscht alle Attribute eines bestimmten Typs von allen Spielern.
	 * 
	 * @param playerDBObject Das JSON-Objekt der Spielerdatenbank
	 * @param attributeName  Name des zu löschenden Attributs
	 */
	public static void deleteAttributeFromAllPlayers(JSONObject playerDBObject, String attributeName) {
		try {
			JSONArray playerDB = playerDBObject.getJSONArray("playerDB");
			int deletedCount = 0;

			for (int i = 0; i < playerDB.length(); i++) {
				JSONObject player = playerDB.getJSONObject(i);

				String attributePath = "data.attribute." + attributeName;

				if (hasAttribute(player, attributePath)) {
					PlayerJSONHelper.deleteJsonObjectFromPlayer(player, attributePath);
					deletedCount++;
				}
			}

			LOGGER.info(String.format("Attribut '%s' von %d Spielern entfernt.", attributeName, deletedCount));

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Löschen der Attribute: " + attributeName, e);
		}
	}

	/**
	 * Findet Spieler mit dem maximalen Wert für ein bestimmtes Feld und setzt
	 * entsprechende Attribute.
	 * 
	 * @param allPlayers    Liste aller Spieler
	 * @param fieldPath     Pfad zum zu vergleichenden Feld (z.B. "wert", "punkte")
	 * @param attributeName Name des zu setzenden Attributs
	 * @param spieltag      Aktueller Spieltag
	 */
	private static void findAndSetMaxAttribute(List<JSONObject> allPlayers, String fieldPath, String attributeName, int spieltag) {
		try {
			// Finde maximalen Wert
			OptionalInt maxValue = allPlayers.stream().mapToInt(player -> PlayerJSONHelper.getPlayerFieldValue(player, fieldPath)).filter(value -> value > 0) // Ignoriere 0-Werte
					.max();

			if (!maxValue.isPresent()) {
				LOGGER.warning(() -> String.format("Kein maximaler Wert für Feld '%s' gefunden.", fieldPath));
				return;
			}

			int max = maxValue.getAsInt();

			// Finde alle Spieler mit diesem Maximalwert
			List<JSONObject> maxPlayers = allPlayers.stream().filter(player -> PlayerJSONHelper.getPlayerFieldValue(player, fieldPath) == max).collect(Collectors.toList());

			// Setze Attribut für alle gefundenen Spieler
			for (JSONObject player : maxPlayers) {
				String attributeValue = getValueForAttribute(player, fieldPath, attributeName, max, spieltag);
				setPlayerAttribute(player, "SonstigeInfos", attributeValue);

				LOGGER.fine(() -> String.format("Attribut '%s' für Spieler %s gesetzt: %s", attributeName, getPlayerName(player), attributeValue));
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Setzen des Max-Attributs: " + attributeName, e);
		}
	}

	/**
	 * Findet die Spieler mit der höchsten Anzahl an Transfers anhand der Transfer-News 
	 * und setzt für diese Spieler ein entsprechendes Attribut.
	 * 
	 * Ablauf:
	 * 1. Ermittelt alle Transfer-Events gruppiert nach Datum durch den NewsManager.
	 * 2. Zählt für jeden Spieler die Anzahl seiner Transfers.
	 * 3. Filtert Spieler heraus, die aktuell keinem Verein zugeordnet sind (Verein = "0").
	 * 4. Findet die Spieler mit der maximalen Anzahl an Transfers.
	 * 5. Setzt für jeden dieser Top-Spieler ein Attribut "SonstigeInfos" mit der Info über die Transferanzahl.
	 *
	 * @param allPlayers Liste aller Spieler als JSONObject-Instanzen mit Spielerinformationen.
	 *                   Erwartet wird, dass die Spielerobjekte eine "data"-Struktur mit Vereinsinformationen enthalten.
	 * @param newsManager Instanz des NewsManagers, die Zugriff auf Transfer-News bietet.
	 *
	 * @throws Exception Wird abgefangen und als Fehler geloggt, falls bei der Verarbeitung Probleme auftreten.
	 */
	private static void findMostTransfers(List<JSONObject> allPlayers, NewsManager newsManager) {
	    try {
	        Map<ComunioDate, List<News>> transfers = newsManager.getTransferNewsByDate();
	        Map<String, Integer> playerCountMap = new HashMap<>();

	        for (List<News> newsList : transfers.values()) {
	            for (News news : newsList) {
	                String playerId = news.getPlayerId();
	                playerCountMap.put(playerId, playerCountMap.getOrDefault(playerId, 0) + 1);
	            }
	        }

	        // Hilfemap: Spieler ID → Vereinsstatus ("verein" als String)
	        Map<String, String> playerClubMap = new HashMap<>();
	        for (JSONObject player : allPlayers) {
	            playerClubMap.put(getPlayerId(player), player.optJSONObject("data") != null ? player.getJSONObject("data").optString("verein", "") : "");
	        }

	        List<String> topPlayerIDs = new ArrayList<>();
	        int maxTransferCount = 0;

	        for (Map.Entry<String, Integer> entry : playerCountMap.entrySet()) {
	            String playerId = entry.getKey();
	            int count = entry.getValue();

	            // Spieler vom Verein "0" überspringen
	            if ("0".equals(playerClubMap.get(playerId))) {
	                continue;
	            }

	            if (count > maxTransferCount) {
	                topPlayerIDs.clear();
	                topPlayerIDs.add(playerId);
	                maxTransferCount = count;
	            } else if (count == maxTransferCount) {
	                topPlayerIDs.add(playerId);
	            }
	        }

	        if (topPlayerIDs.isEmpty()) {
	            LOGGER.info("Keine Transfers gefunden.");
	            return;
	        }

	        for (JSONObject player : allPlayers) {
	            String playerId = getPlayerId(player);
	            if (topPlayerIDs.contains(playerId)) {
	                String attributeValue = String.format("häufigsten Transfers (%d)", maxTransferCount);
	                setPlayerAttribute(player, "SonstigeInfos", attributeValue);
	            }
	        }
	    } catch (Exception e) {
	        LOGGER.log(Level.SEVERE, "Fehler beim Ermitteln der häufigsten Transfers", e);
	    }
	}


	/**
	 * Setzt ein Attribut für einen Spieler.
	 * 
	 * @param player         Das Spieler-JSONObject
	 * @param attributeType  Typ des Attributs (z.B. "SonstigeInfos")
	 * @param attributeValue Wert des Attributs
	 */
	private static void setPlayerAttribute(JSONObject player, String attributeType, String attributeValue) {
		try {
			JSONObject data = player.getJSONObject("data");
			JSONObject attribute = data.optJSONObject("attribute");

			if (attribute == null) {
				attribute = new JSONObject();
				data.put("attribute", attribute);
			}

			JSONArray attributeArray = attribute.optJSONArray(attributeType);
			if (attributeArray == null) {
				attributeArray = new JSONArray();
				attribute.put(attributeType, attributeArray);
			}

			// Füge neuen Wert hinzu, wenn er noch nicht existiert
			if (!arrayContains(attributeArray, attributeValue)) {
				attributeArray.put(attributeValue);
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Setzen des Spielerattributs", e);
		}
	}

	/**
	 * Hilfsmethode: Konvertiert JSONArray zu List<JSONObject>.
	 */
	private static List<JSONObject> jsonArrayToList(JSONArray jsonArray) {
		List<JSONObject> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.getJSONObject(i));
		}
		return list;
	}

	/**
	 * Hilfsmethode: Holt die Spieler-ID.
	 */
	private static String getPlayerId(JSONObject player) {
		return player.optString("id", "unknown");
	}

	/**
	 * Hilfsmethode: Holt den Spielernamen.
	 */
	private static String getPlayerName(JSONObject player) {
		return player.optString("name", "Unknown");
	}

	/**
	 * Hilfsmethode: Prüft, ob ein Spieler ein bestimmtes Attribut hat.
	 */
	private static boolean hasAttribute(JSONObject player, String attributePath) {
		String[] pathParts = attributePath.split("\\.");
		Object current = player;

		for (String part : pathParts) {
			if (current instanceof JSONObject obj) {
				if (!obj.has(part)) {
					return false;
				}
				current = obj.get(part);
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Hilfsmethode: Prüft, ob ein JSONArray einen bestimmten String enthält.
	 */
	private static boolean arrayContains(JSONArray array, String value) {
		for (int i = 0; i < array.length(); i++) {
			if (value.equals(array.optString(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Hilfsmethode: Generiert den Attribut-Wert basierend auf dem Feld und Wert.
	 */
	private static String getValueForAttribute(JSONObject player, String fieldPath, String attributeName, int value, int spieltag) {
		NumberFormat numberFormat = NumberFormat.getInstance();

		return switch (attributeName) {
		case "teuerster Spieler" -> "teuerster Spieler (" + numberFormat.format(value) + " €)";
		case "teuerster Spieler (Realwert)" -> "teuerster Spieler (Realwert) (" + numberFormat.format(value) + " €)";
		case "meisten Punkte" -> "meisten Punkte (" + value + ")";
		case "meisten Punkte letzte Saison" -> "meisten Punkte letzte Saison (" + value + ")";
		default -> attributeName + " (" + value + ")";
		};
	}

	/**
	 * Hilfsmethode für Testzwecke: Gibt alle Attribute aller Spieler aus.
	 */
	public List<String> getAllAttribute(JSONObject playerDBObject) {
		List<String> result = new ArrayList<>();

		try {
			JSONArray playerDB = playerDBObject.getJSONArray("playerDB");

			for (int i = 0; i < playerDB.length(); i++) {
				JSONObject player = playerDB.getJSONObject(i);
				JSONObject attributes = player.getJSONObject("data").optJSONObject("attribute");

				if (attributes != null && attributes.length() > 0) {
					String playerName = getPlayerName(player);
					String playerId = getPlayerId(player);

					for (String key : attributes.keySet()) {
						Object value = attributes.get(key);
						result.add(playerName + " (" + playerId + "), " + key + ": " + value);
					}
				}
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Abrufen aller Attribute", e);
		}

		return result;
	}
}