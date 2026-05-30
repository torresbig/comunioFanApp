package comunio.nas.objects.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.enu.NewsArt;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.orga.ComunioDate;

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

	/**
	 * Prüft, ob ein String bereits in einem JSONArray enthalten ist
	 * (case-insensitiv).
	 *
	 * @param array das JSON-Array mit Strings
	 * @param value der zu prüfende String
	 * @return true, wenn enthalten
	 */
	private static boolean containsValue(JSONArray array, String value) {
		for (int i = 0; i < array.length(); i++) {
			if (array.optString(i).equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	public static void getAllPlayersNotUpdatet(JSONArray playerDB, NewsManager newsManager) {
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			JSONObject data = player.optJSONObject("data");

			if (data.has("lastUpdate")) {
				ComunioDate lastUpdate = new ComunioDate(data.getString("lastUpdate"));
				ComunioDate now = new ComunioDate();

				if (lastUpdate.before(now)) {
					try {
						JSONObject status = data.optJSONObject("status");
						if (status == null) {
							status = new JSONObject();
							data.put("status", status);
						}
						// nächstes Object, wenn schon sder status nicht in liga ist.
						if (status.getString("status").equals("NICHT_IN_LIGA")) {
							continue;
						}
						JSONObject comunioStatus = data.optJSONObject("comunioStatus");
						if (comunioStatus == null) {
							comunioStatus = new JSONObject();
							data.put("comunioStatus", comunioStatus);
						}
						// nächstes Object, wenn schon sder status nicht in liga ist.
						if (comunioStatus.getString("status").equals("NICHT_IN_LIGA")) {
							JSONObject stat = new JSONObject();
							stat.put("status", "NICHT_IN_LIGA");
							stat.put("grund", "");
							stat.put("seit", "");
							stat.put("bis", JSONObject.NULL);
							stat.put("details", "");
							stat.put("lastUpdate", lastUpdate);
							status = stat;
							continue;
						}

						String name = player.getString("name");
						String playerId = player.getString("id");
						String newsText = "Statuswechsel: " + name + " (" + playerId + ") ist jetzt NICHT_IN_LIGA";
						News news = new News(NewsArt.SPIELERSTATUS, newsText, player.getString("id"));

						if (!newsManager.contains(news)) {
							newsManager.addNews(news, true);
						}
						LOGGER.info(newsText);

						JSONObject stat = new JSONObject();
						stat.put("status", "NICHT_IN_LIGA");
						stat.put("grund", "");
						stat.put("seit", "");
						stat.put("bis", JSONObject.NULL);
						stat.put("details", "");
						stat.put("lastUpdate", lastUpdate);
						status = stat;
						comunioStatus = stat;

						data.put("verein", "0");

					} catch (Exception e) {

					}

				}
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
     * @param pointsDB JSON-Datenbank (Key = Spieler-ID, Value = JSONArray mit Spieltagen)
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
                    if (next.isNull("totalPoints")) continue;

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
                if (k <= currentSpieltag) out.put(map.get(k));
            }

            pointsDB.put(playerId, out);
        });
    }


}
