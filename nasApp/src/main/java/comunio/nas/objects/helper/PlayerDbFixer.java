package comunio.nas.objects.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.cheats.KontostandBerechner;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataScraper.comunio.NewsAnalyzerComunio;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.TransferNewsEntry;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.user.User;
import comunio.nas.util.player.PlayerHelper;

public class PlayerDbFixer {

	private static final Logger LOGGER = LogManager.getLogger(PlayerDbFixer.class);

	/**
	 * Verschiebt fälschlich auf Root-Level gespeicherte Felder („realWert“,
	 * „possibleNames“) zurück in das data-Objekt des jeweiligen Spielers.
	 *
	 * @param playerDb Die JSONArray mit allen Spielern
	 */
	public static void fixFalseDataSpielerstats(JSONArray playerDB) {
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");

			if (data == null) {
				LOGGER.warning("Kein 'data'-Objekt vorhanden für: " + player.toString());
				continue;
			}

			// ----- realWert korrigieren -----
			if (player.has("lastBigUpdate")) {
				String stats = player.getString("lastBigUpdate");
				data.put("lastBigUpdate", stats);
				player.remove("lastBigUpdate");

				LOGGER.info("lastBigUpdate von Root-Ebene in data verschoben für Spieler: " + player.optString("name"));
			}
		}
	}

	public static void fixTmDeMarktwerteAndRealWert(JSONArray playerDB) {
		int counter = 0;
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");
			if (data == null) {
				LOGGER.warning("Kein 'data'-Objekt vorhanden für: " + player.toString());
				continue;
			}

			if (data.optLong("realWert") == 0) {
				JSONObject tmDe = data.optJSONObject("transfermarktDoDe");
				if (tmDe != null) {
					long wert = tmDe.optLong("marktwert");
					if (wert > 0) {
						data.put("realWert", wert);
						counter++;
						LOGGER.warning("Marktwert korrigiert für: " + player.getString("name"));
					}
				} else {

					continue;
				}
			}
		}
		LOGGER.warning("Marktwerte für " + counter + " korrigiert.");
	}

	/**
	 * Wandelt für jeden Spieler im Array das Attribut-JSONArray in ein JSONObject
	 * um. Dabei wird angenommen, dass keine Schlüssel mehrfach vorkommen.
	 */
	public static void fixAttributeArrayInPlayers(JSONArray playerDB) {
		StringBuilder sb = new StringBuilder();
		sb.append("DB Fix: fixAttributeArrayInPlayers - ändern von JSONArray in JSONObject").append(System.lineSeparator());
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			if (!player.has("data")) {
				sb.append("Spieler " + player.getString("name") + " hat kein data-Object.").append(System.lineSeparator());
				LOGGER.warning(sb.toString());
				continue;
			}

			JSONObject data = player.getJSONObject("data");
			if (!data.has("attribute")) {
				sb.append("Spieler " + player.getString("name") + " hat kein attribute-Object.").append(System.lineSeparator());
				LOGGER.warning(sb.toString());
				data.put("attribute", new JSONObject());
			}

			Object attrObj = data.get("attribute");

			if (attrObj instanceof JSONArray) {

				sb.append("attribute vorhanden als JSONArray...").append(System.lineSeparator());

				JSONArray attrArray = (JSONArray) attrObj;
				data.remove("attribute");
				sb.append("vorhandene attribute zwischengespeichert und aus DB gelöscht").append(System.lineSeparator());
				if (attrArray.isEmpty()) {
					data.put("attribute", new JSONObject());
					sb.append("Spieler " + player.getString("name") + " hat kein Attribute (Liste ist leer)");
					LOGGER.warning(sb.toString());
					continue;
				}
				JSONObject attrObject = new JSONObject();
				boolean rankingDone = false;
				for (int j = 0; j < attrArray.length(); j++) {
					JSONObject attrEntry = attrArray.getJSONObject(j);
					for (String key : attrEntry.keySet()) {
						if (rankingDone) {
							break;
						}
						Object value = attrEntry.get(key);
						// Einfach einfügen, ohne List für Mehrfachkeys
						attrObject.put(key, value);
						if (key.equals("ligainsiderRanking")) {
							rankingDone = true;
							break;
						}
					}
				}

				data.put("attribute", attrObject);
				sb.append("Attribute als JSONObject neu in data gepackt: " + data.toString());
				LOGGER.warning(sb.toString());
			}
		}
	}

	public static void cleanSpielerDatenFromData(JSONObject spielerDBJson) {
		JSONArray spielerDB = spielerDBJson.optJSONArray("playerDB", new JSONArray());
		StringBuilder sb = new StringBuilder();
		sb.append("DB Fix: cleanSpielerDatenFromData - ändern spielerDaten! verschieben in eigene json satt direkt in data").append(System.lineSeparator());
		int zaehler = 0;
		for (Object obj : spielerDB) {
			JSONObject player = (JSONObject) obj;
			JSONObject data = player.optJSONObject("data");
			if (data == null) {
				sb.append("Spielerobjekt hat garkein Data: Spieler: " + player.optString("name") + " (ID: " + player.optString("id") + ")");
				LOGGER.warning(sb.toString());

				continue;
			}

			if (data.has("nebenpositionen")) {
				zaehler++;
				data.remove("nebenpositionen");
				data.remove("nationalspieler");
				data.remove("trikotNummer");
				data.remove("nationalitaet");
				data.remove("laenderspielTore");
				data.remove("hauptposition");
				data.remove("fuss");
				data.remove("groesse");
				data.remove("geburtstag");
				data.remove("laenderspiele");
				data.remove("lastUpdate");
				if (!data.has("spielerDaten")) {
					sb.append("BAD: Spieler: " + player.optString("name") + " (ID: " + player.optString("id") + ")" + " --> hat noch kein spielerdaten Objekt. ").append(System.lineSeparator());
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
				} else {
					sb.append("TOP: Spieler: " + player.optString("name") + " (ID: " + player.optString("id") + ")" + " --> hat ein spielerdaten Objekt. ").append(System.lineSeparator());
				}
			}
		}
		sb.append("--> " + zaehler + "Spielerobjekt mti falschen Spielerdaten bereinigt");
		LOGGER.warning(sb.toString());
	}

	public static void removeWrongOwnerLastPorzessedMatchday(JSONObject playerDBobj) {
		JSONArray spielerDB = playerDBobj.optJSONArray("playerDB", new JSONArray());
		StringBuilder sb = new StringBuilder();
		sb.append("DB Fix: removeWrongOwnerLastPorzessedMatchday - ändern der falschen daten! ").append(System.lineSeparator());
		int zaehler = 0;
		for (Object obj : spielerDB) {
			JSONObject player = (JSONObject) obj;
			JSONObject data = player.optJSONObject("data");
			if (data == null) {
				sb.append("Spielerobjekt hat garkein Data: Spieler: " + player.optString("name") + " (ID: " + player.optString("id") + ")");
				LOGGER.warning(sb.toString());

				continue;
			}
			zaehler++;
			player.remove("owner");
			data.remove("lastProcessedMatchday");
			data.remove("stats");
			data.remove("attribute");
			// player.put("lastProcessedMatchday", 1);

			JSONObject status = data.optJSONObject("status");
			if (status != null) {
				if (status.getString("status").equals("NICHT_IN_LIGA") && !data.getString("verein").equals("0")) {
					data.put("verein", "0");
				}
				JSONArray hist = status.getJSONArray("historie");
				JSONArray newHist = new JSONArray();
				for (Object objHist : hist) {
					JSONObject jsonObj = (JSONObject) objHist;
					if (!jsonObj.getString("status").equals("AKTIV") && !jsonObj.getString("status").equals("NICHT_IN_LIGA")) {
						newHist.put(jsonObj);
					}
				}
			}

		}
		sb.append("--> " + zaehler + "Spielerobjekt mti falschen daten bereinigt");
		LOGGER.warning(sb.toString());
	}

	public static void addNewsToUser(JSONArray userDB, NewsManager newsManager) {
		for (Object obj : userDB) {
			JSONObject jsonObj = (JSONObject) obj;
			JSONObject user = jsonObj.optJSONObject("user");
			if (user != null && !user.getString("id").equals("1")) {

				newsManager.addNews(News.getUserpoints(user.getString("name"), user.getString("id"), jsonObj.getInt("lastPoints"), jsonObj.getInt("punkte")), true);
			}

		}

	}

	public static void fixSpieltagDoppelInterpoliert(JSONObject pointsDB) {
		JSONArray pointsDBArray = pointsDB.optJSONArray("pointsDB", new JSONArray());
		for (Object obj : pointsDBArray) {
			JSONObject jsonObj = (JSONObject) obj;
			if (jsonObj.has("spieltag")) {
				int spieltag = jsonObj.getInt("spieltag");
				if (spieltag > 34) {
					jsonObj.put("spieltag", 34);
				}
			}
		}
	}

	/**
	 * Füllt für jeden Spieler fehlende Spieltage bis currentSpieltag auf.
	 * 
	 * @param pointsDB        JSON-Datenbank (Key = Spieler-ID, Value = JSONArray
	 *                        mit Spieltagen)
	 * @param currentSpieltag letzter (maximaler) bekannter Spieltag
	 */
	public static void fixFehlendeSpieltagspunkte(JSONObject pointsDB, int currentSpieltag) {

		pointsDB.keySet().forEach(playerId -> {
			JSONArray array = pointsDB.getJSONArray(playerId);

			// Map zur leichteren Verarbeitung
			Map<Integer, JSONObject> map = new HashMap<>();
			for (Object o : array) {
				JSONObject e = (JSONObject) o;
				if (e.has("key")) {
					map.put(e.getInt("key"), e);
				}
			}

			List<Integer> keys = new ArrayList<>(map.keySet());
			Collections.sort(keys);

			// --- Spezialfall: erster Spieltag fehlt ---
			if (!map.containsKey(1) && !keys.isEmpty()) {
				int firstKey = keys.get(0);
				JSONObject first = map.get(firstKey);
				int nextTotal = first.optInt("totalPoints", 0);
				int nextValue = first.optInt("value", 0);
				int inferredValue = Math.max((nextTotal - nextValue) / 1, 0);
				JSONObject sp1 = new JSONObject();
				sp1.put("key", 1);
				sp1.put("value", inferredValue);
				sp1.put("totalPoints", inferredValue);
				sp1.put("info", "Interpoliert (Startspieltag aus 0 berechnet)");
				map.put(1, sp1);
				keys.add(1);
				Collections.sort(keys);
			}

			// --- Hauptinterpolation ---
			for (int i = 0; i < keys.size() - 1; i++) {
				int currentKey = keys.get(i);
				int nextKey = keys.get(i + 1);

				JSONObject current = map.get(currentKey);
				JSONObject next = map.get(nextKey);
				int gap = nextKey - currentKey - 1;

				if (gap > 0) {
					// nicht interpolieren, wenn der Nachfolger gar keinen totalPoints hat
					if (next.isNull("totalPoints"))
						continue;

					int prevTotal = current.optInt("totalPoints", 0);
					int nextTotal = next.optInt("totalPoints", prevTotal);
					int nextValue = next.optInt("value", 0);

					int diffTotal = (nextTotal - prevTotal) - nextValue;

					if (gap == 1) {
						// einfacher Fall: ein Spieltag fehlt
						int interpolValue = diffTotal;
						int interpolTotal = prevTotal + interpolValue;
						JSONObject e = new JSONObject();
						e.put("key", currentKey + 1);
						e.put("value", interpolValue);
						e.put("totalPoints", interpolTotal);
						e.put("info", "Interpoliert (einzelner fehlender Spieltag)");
						map.put(currentKey + 1, e);
					} else {
						// mehrere fehlen → gleichmäßig verteilen
						int avgValue = (gap > 0) ? Math.max(diffTotal / gap, 0) : 0;
						int runningTotal = prevTotal;
						for (int j = 1; j <= gap; j++) {
							runningTotal += avgValue;
							JSONObject e = new JSONObject();
							e.put("key", currentKey + j);
							e.put("value", avgValue);
							e.put("totalPoints", runningTotal);
							e.put("info", "Interpoliert (mehrere fehlende Spieltage)");
							map.put(currentKey + j, e);
						}
					}
				}
			}

			// --- kein künstlicher Schluss-Spieltag! ---
			JSONArray out = new JSONArray();
			List<Integer> allKeys = new ArrayList<>(map.keySet());
			Collections.sort(allKeys);
			for (int k : allKeys) {
				if (k <= currentSpieltag)
					out.put(map.get(k));
			}

			pointsDB.put(playerId, out);
		});
	}

	public static void removeAllStatusFromPlayerObject(JSONObject playerDbObject) {
		JSONArray spielerDB = playerDbObject.optJSONArray("playerDB", new JSONArray());

		for (int i = 0; i < spielerDB.length(); i++) {
			JSONObject spielerObj = spielerDB.getJSONObject(i);
			JSONObject data = spielerObj.optJSONObject("data");
			data.remove("status"); // remove status from player object
			data.remove("comunioStatus"); // remove status from player object
			spielerObj.put("data", data);
		}
	}

	public static void removeFromPlayerToUserMapIfNotInLiga(JSONObject notInLigaDb, Map<String, String> playerToUserMap) {
		JSONObject db = notInLigaDb.optJSONObject("db", new JSONObject());
		for (String key : db.keySet()) {
			playerToUserMap.remove(key);
		}
	}

	public static void fixTagRetiredOnAllDb(JSONObject playerDbObject, JSONObject notInLigaDb) {
		JSONArray spielerDB = playerDbObject.optJSONArray("playerDB", new JSONArray());

		for (int i = 0; i < spielerDB.length(); i++) {
			JSONObject spielerObj = spielerDB.getJSONObject(i);
			JSONObject data = spielerObj.optJSONObject("data");
			data.remove("status");
			data.remove("comunioStatus");
			data.remove("spieltagspunkte");
			if (data.has("transfermarktDoDe")) {
				JSONObject tmde = data.getJSONObject("transfermarktDoDe");
				if (tmde.has("name") && (tmde.getString("name").contains("Mio.") || tmde.getString("name").contains("Tsd."))) {
					tmde.put("name", cleanName(tmde.getString("name")));
				}
			}
			if (data.has("retired")) {
				continue;
			}
			data.put("retired", false);
			spielerObj.put("data", data);
		}

		JSONObject db = notInLigaDb.optJSONObject("db", new JSONObject());
		for (String key : db.keySet()) {
			JSONObject nilObj = db.getJSONObject(key);
			JSONObject nilData = nilObj.optJSONObject("data");
			nilData.remove("status");
			nilData.remove("comunioStatus");
			nilData.remove("spieltagspunkte");
			if (nilData.has("transfermarktDoDe")) {
				JSONObject tmde = nilData.getJSONObject("transfermarktDoDe");
				if (tmde.has("name") && (tmde.getString("name").contains("Mio.") || tmde.getString("name").contains("Tsd."))) {
					tmde.put("name", cleanName(tmde.getString("name")));
				}
			}

			if (nilData.has("retired")) {
				continue;
			}

			nilData.put("retired", true);

			nilObj.put("data", nilData);
			db.put(key, nilObj);
		}
	}

	public static String cleanName(String raw) {
		return raw.replaceAll("\\s*\\d+[,.]\\d+\\s*Mio\\.\\s*€", "").trim();
	}

	// Methode zum Entfernen eines Schlüssels aus verschachtelten JSON-Arrays mit
	// 3-Optionen-Bestätigung
	public static void removeKeyFromArray(JSONObject root, String keyPath) {
		String[] pathParts = keyPath.split("\\."); // z.B. "data.clubID"

		// Navigation zum Zielobjekt (root -> playerDB -> [i] -> pathParts[0] -> ...)
		JSONArray playerDB = root.getJSONArray("playerDB");
		List<JSONObject> parentContainers = new ArrayList<>();

		// Sammle alle Container-Objekte, die den Zielschlüssel enthalten
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject item = playerDB.getJSONObject(i);
			JSONObject container = navigateToContainer(item, pathParts);
			if (container != null && container.has(pathParts[pathParts.length - 1])) {
				parentContainers.add(container);
			}
		}

		if (parentContainers.isEmpty()) {
			System.out.println("Keine Objekte mit Pfad '" + keyPath + "' gefunden");
			return;
		}

		// Bestätigungsdialog mit 3 Optionen: Einzeln löschen, Alle löschen, Abbrechen
		String message = "Schlüssel '" + keyPath + "' in " + parentContainers.size() + " Objekten gefunden.\nWie möchten Sie fortfahren?";
		int result = JOptionPane.showOptionDialog(null, message, "Löschen bestätigen", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "Einzeln löschen", "Alle löschen", "Abbrechen" }, "Alle löschen");

		switch (result) {
		case 0: // Einzeln löschen
			// Zeige Dialog für jedes Objekt einzeln an
			for (JSONObject container : parentContainers) {
				int singleResult = JOptionPane.showConfirmDialog(null, "Schlüssel '" + keyPath + "' in diesem Objekt entfernen?", "Bestätigung", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (singleResult == JOptionPane.YES_OPTION) {
					container.remove(pathParts[pathParts.length - 1]);
					System.out.println("Schlüssel entfernt aus einem Objekt");
				}
			}
			break;

		case 1: // Alle löschen
			for (JSONObject container : parentContainers) {
				container.remove(pathParts[pathParts.length - 1]);
			}
			System.out.println("Schlüssel in allen " + parentContainers.size() + " Objekten entfernt");
			break;

		case 2: // Abbrechen
			System.out.println("Löschen abgebrochen");
			break;
		}
	}

	// Hilfsmethode zur Navigation zu dem Objekt, das den Zielschlüssel enthält
	private static JSONObject navigateToContainer(JSONObject root, String[] pathParts) {
		JSONObject current = root;
		// Navigiere durch alle Teile des Pfads bis auf das letzte (das ist der
		// Zielcontainer)
		for (int i = 0; i < pathParts.length - 1; i++) {
			if (current.has(pathParts[i])) {
				Object next = current.get(pathParts[i]);
				if (next instanceof JSONObject) {
					current = (JSONObject) next;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		return current;
	}

	/**
	 * Vergleicht alle Transfers einer API-Abfrage im angegebenen Zeitraum mit den
	 * Transfers, die bereits im NewsManager bekannt sind. Fehlende Transfers werden
	 * mit dem API-Datum eingetragen, der Kontostand-Berechner wird erneut
	 * ausgeführt (damit der Käufer das Geld erhält), und alle fehlenden Transfers
	 * werden in einer Liste zurückgegeben.
	 *
	 * @param newsManager     NewsManager mit allen bekannten Transfers
	 * @param startDateStr    Start-Datum (Format: "2026-08-22" oder "22.08.2026")
	 * @param endDateStr      End-Datum (Format: "2026-08-22" oder "22.08.2026")
	 * @param user            aktueller Comunio-Benutzer (für API-Zugriff)
	 * @param matchdayInfo    Matchday-Info-Objekt
	 * @param lastUpdates     LastUpdates-Objekt
	 * @param playerDBObject  JSONObject mit Player-Datenbank
	 * @param playerToUserMap Mapping Spieler → User
	 * @param notInLigaDbObj  JSONObject für nicht-gefundene Spieler
	 * @param userMap         Map der User (für Kontostandsberechnung)
	 * @return Liste der fehlenden Transfers (als News-Objekte)
	 */
	public static List<News> findAndInsertMissingTransfers(String startDateStr, String endDateStr, NewsManager newsManager, User user, MatchdayInfo matchdayInfo, LastUpdates lastUpdates, JSONObject playerDBObject, Map<String, String> playerToUserMap, JSONObject notInLigaDbObj, Map<String, User> userMap) {
		if (playerDBObject == null || !playerDBObject.has("playerDB")) {
			return new ArrayList<>();
		}

		JSONArray playerDb = playerDBObject.getJSONArray("playerDB");

		// Datums-Parsing mit Fallback für beide Formate
		ComunioDate startDate = null;
		ComunioDate endDate = null;
		try {
			startDate = new ComunioDate(startDateStr);
			endDate = new ComunioDate(endDateStr);
		} catch (Exception e) {
			LOGGER.warning("Fehler beim Parsen der Datumsangaben: " + e.getMessage());
			return Collections.emptyList();
		}

		// Alle Transfers vom API im Zeitraum holen
		List<TransferNewsEntry> apiTransfers = new ArrayList<>();
		// Temporäre Liste für fehlende Transfers
		List<News> missingTransfers = new ArrayList<>();

		try {
			// Paginierte API-Abfrage wie in analyzeNews, aber mit festem Start- und
			// Enddatum
			int page = 0;
			int limit = 20;
			boolean hasMore = true;
			boolean stopBecauseOfOldDate = false;

			while (hasMore) {
				JSONObject newsRoot = NewsAnalyzerComunio.fetchNewsFromApi(user, page, limit);
				if (newsRoot == null || !newsRoot.has("newsList")) {
					break;
				}

				JSONObject newsList = newsRoot.getJSONObject("newsList");
				JSONObject groups = newsList.optJSONObject("groups");
				if (groups == null) {
					break;
				}

				for (String dateKey : groups.keySet()) {
				    ComunioDate groupDate = new ComunioDate(dateKey);

				    if (groupDate.before(startDate)) {
				        stopBecauseOfOldDate = true;
				        continue;
				    }

				    if (groupDate.after(endDate)) {
				        continue;
				    }

					JSONArray entries = groups.getJSONObject(dateKey).getJSONArray("entries");
					for (int i = 0; i < entries.length(); i++) {
						JSONObject entry = entries.getJSONObject(i);
						if ("TRANSACTION_TRANSFER".equals(entry.optString("type"))) {
							processTransferForCheck(entry, groupDate, playerDb, notInLigaDbObj, apiTransfers, playerToUserMap, newsManager, missingTransfers);
						}
					}
				}
				
				if (stopBecauseOfOldDate) {
				    break;
				}


				hasMore = newsList.optBoolean("hasMore", false);
				page += limit;
			}

			// Fehlende Transfers im NewsManager eintragen
			for (News missingNews : missingTransfers) {
				if (!newsManager.contains(missingNews)) {
					newsManager.addNews(missingNews, true);
				}
			}

			// Kontostände neu berechnen, damit Käufer das Geld erhält
			if (!missingTransfers.isEmpty()) {
				KontostandBerechner calc = new KontostandBerechner();
				calc.calculateKontostaende(userMap, newsManager);
			}

		} catch (Exception e) {
			LOGGER.severe("Fehler beim Abgleich der Transfers: " + e.getMessage());
		}

		return missingTransfers;
	}

	/**
	 * Verarbeitet einen einzelnen API-Transfer-Eintrag und prüft, ob er bereits im
	 * NewsManager bekannt ist. Falls nicht, wird er zur Liste der fehlenden
	 * Transfers hinzugefügt.
	 *
	 * @param entry            JSONObject des API-News-Eintrags vom Typ
	 *                         "TRANSACTION_TRANSFER"
	 * @param newsDate         ComunioDate des zugehörigen Datumsblocks
	 * @param playerDB         JSONArray mit der Spieler-Datenbank
	 * @param notInLigaDbObj   JSONObject für nicht-gefundene Spieler
	 * @param apiTransfers     Liste, in die gefundene TransferNewsEntry-Objekte
	 *                         eingefügt werden (wird ggf. später verwendt)
	 * @param playerToUserMap  Mapping Spieler-ID -> User-ID
	 * @param newsManager      NewsManager zur Prüfung, ob News bereits bekannt ist
	 * @param missingTransfers Ergebnis-Liste: hier werden fehlende Transfers (als
	 *                         News-Objekte) eingetragen
	 */
	private static boolean processTransferForCheck(JSONObject entry, ComunioDate newsDate, JSONArray playerDB, JSONObject notInLigaDbObj, List<TransferNewsEntry> apiTransfers, Map<String, String> playerToUserMap, NewsManager newsManager, List<News> missingTransfers) {
		String transactionId = String.valueOf(entry.optLong("id"));
		JSONObject message = entry.getJSONObject("message");
		String[] transferTypes = { "FROM_COMPUTER", "TO_COMPUTER", "BETWEEN_USERS" };

		boolean foundOldDate = false;

		for (String transferType : transferTypes) {
			if (!message.has(transferType)) {
				continue;
			}

			JSONArray transfers = message.getJSONArray(transferType);
			for (int j = 0; j < transfers.length(); j++) {
				try {
					JSONObject transfer = transfers.getJSONObject(j);
					String playerId = String.valueOf(transfer.getJSONObject("tradable").getInt("id"));
					JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, playerId, notInLigaDbObj);

					String sellerId = String.valueOf(transfer.getJSONObject("from").getInt("id"));
					String seller = transfer.getJSONObject("from").getString("name");
					String buyerId = String.valueOf(transfer.getJSONObject("to").getInt("id"));
					String buyer = transfer.getJSONObject("to").getString("name");

					int price = transfer.getInt("price");
					int value = player != null ? player.getJSONObject("data").optInt("wert", 0) : 0;
					String playerName = transfer.getJSONObject("tradable").getString("name");

					News news = News.getTransfer(sellerId, seller, buyerId, buyer, price, value, playerId, playerName, newsDate, transactionId);

					if (!newsManager.contains(news)) {
						missingTransfers.add(news);
					}

					boolean alreadyInList = apiTransfers.stream().anyMatch(t -> t.getNews().equals(news));

					if (!alreadyInList) {
						apiTransfers.add(new TransferNewsEntry(newsDate, playerId, buyerId, news));
					}

				} catch (Exception e) {
					LOGGER.info("Fehler bei Transfer-Prüfung: " + e.getMessage());
				}
			}
		}

		return foundOldDate;
	}

}
