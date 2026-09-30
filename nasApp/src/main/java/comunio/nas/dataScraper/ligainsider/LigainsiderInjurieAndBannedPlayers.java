package comunio.nas.dataScraper.ligainsider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.NewsArt;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.JsonHelper;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.InjuryAndBlockedData;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.ClubMapper;
import comunio.nas.util.player.PlayerMatcher;

//LigainsiderInjurieAndBannedPlayers.java
public class LigainsiderInjurieAndBannedPlayers {
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderInjurieAndBannedPlayers.class);

	private Map<String, InjuryAndBlockedData> injuriedAndBannedPlayer;
	private Map<String, List<InjuryAndBlockedData>> history;
	private Instant lastUpdate;

	public LigainsiderInjurieAndBannedPlayers() {
		this.injuriedAndBannedPlayer = new HashMap<>();
		this.history = new HashMap<>();
	}

	public void updateAllInjuredBannedPlayer(JSONArray playerDB, Map<String, ClubObject> clubDb, MatchdayInfo currentMatchday, NewsManager newsManager) {
		Map<String, List<InjuryAndBlockedData>> currentInjuries = parseVerletzteUndGesperrte();

		int countOldSizeInjuried = injuriedAndBannedPlayer.size();
		int countcurrentInjuries = currentInjuries.values().stream().mapToInt(List::size).sum();

		if (lastUpdate != null && new ComunioDate(this.lastUpdate).isSameDay(new ComunioDate())) {
			return;
		}

		if (currentInjuries == null || currentInjuries.isEmpty()) {
			LOGGER.info("keine Verletzten oder Gesperrten Spieler gefunden!");
//			handlePlayersLeftInjury(currentInjuries); // Spieler ohne Eintrag in History verschieben
			return;
		}

		// Statusänderungen prüfen und History aktualisieren
		try {

			for (Map.Entry<String, List<InjuryAndBlockedData>> entry : currentInjuries.entrySet()) {
				String clubId = ClubMapper.getComunioIdFromName(entry.getKey(), JsonHelper.mapValuesToJSONArray(clubDb));
				String clubName = clubDb.containsKey(clubId) ? clubDb.get(clubId).getName() : null;
				if (clubName == null) {
					LOGGER.warning("Kein passenden Club zur ID gefunden: " + clubId);
					return;
				}

				entry.getValue().forEach(currentPlayer -> {
					JSONObject player = PlayerMatcher.findPlayerByNameAndClub(playerDB, clubDb, currentPlayer.getPlayerName(), clubName);
					if (player == null) {
						player = PlayerMatcher.findPlayerByNameAndClub(playerDB, clubDb, currentPlayer.extractPlayerNameFromUrl(), clubName);
					}

					if (player != null && !player.isEmpty()) {
						String playerId = player.getString("id");

						// Validierung: Leere oder null-ID verhindern
						if (playerId == null || playerId.isBlank()) {
							LOGGER.warning("Ungültige ComunioPlayerId für Spieler: " + currentPlayer.getPlayerName() + " – wird übersprungen");
							return; // oder continue, falls du in einem Stream bist
						}

						// FIX: Comunio-ID ins Objekt schreiben, damit handlePlayersLeftInjury korrekt
						// vergleichen kann
						currentPlayer.setComunioPlayerId(playerId);
						countVerarbeitung++;
						currentPlayer.setQuelle("ligainsider");
						mergeStatus(playerId, currentPlayer, newsManager);

						JSONObject data = player.getJSONObject("data");
						data.put("ligainsider", currentPlayer.toPlayerDataJSON());

					} else {
						ComunioDataUpdater.errorDb.addError(new Error(ErrorType.LIGAINSIDERSTATUS, "Spieler wurde nicht gefunden! Spieler" + currentPlayer.getPlayerName() + " | Club: " + clubName));
					}
				});

			}
		} catch (Exception e) {
			LOGGER.warning("Fehler im prozess für verletzte spieler bei ligainsider:   " + e.getMessage());
			return;
		}

		// Spieler, die nicht mehr in aktuellen Daten vorkommen, in History verschieben
		handlePlayersLeftInjury(currentInjuries, newsManager);
		this.lastUpdate = Instant.now();
		LOGGER.info("Es wurden " + countVerarbeitung + " Spieler von der Webseite Ligainsider Verarbeitet!! InjuryMap bevor: " + countOldSizeInjuried + " danach: " + this.injuriedAndBannedPlayer.size() + ", gefunde Verletzte auf Ligainsider: " + countcurrentInjuries);

		countVerarbeitung = 0;
	}

	int countVerarbeitung = 0;
	int countRemove = 0;

	private void handlePlayersLeftInjury(Map<String, List<InjuryAndBlockedData>> currentInjuries, NewsManager newsManager) {
		Set<String> currentPlayers = currentInjuries.values().stream().flatMap(List::stream).map(InjuryAndBlockedData::getComunioPlayerId).collect(Collectors.toSet());

		this.injuriedAndBannedPlayer.entrySet().removeIf(entry -> {
			String playerId = entry.getKey();
			if (!currentPlayers.contains(playerId)) {
				InjuryAndBlockedData removedData = entry.getValue();
				removedData.setStatusChangeString(new ComunioDate().toString());
				addPlayerToHistory(playerId, removedData);
				if (newsManager != null) {
					String newsText = "Statuswechsel: " + removedData.getPlayerName() + " (" + playerId + ") ist wieder AKTIV";
					News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);
					if (!newsManager.contains(news)) {
						newsManager.addNews(news, true);
					}
				}
				countRemove++;
				return true;
			}
			return false;
		});
		LOGGER.info("Es wurden " + countRemove + " Spieler wieder auf Aktiv gesetzt, da sie nicht mehr in den Daten der Webseite auftauchen!");
		countRemove = 0;
	}

	private void addPlayerToHistory(String playerId, InjuryAndBlockedData data) {
		this.history.computeIfAbsent(playerId, k -> new ArrayList<>()).add(data);
	}

	/**
	 * laden aller verletzten und gesperrten spieler mit zusäzlichen Infomationen
	 * die alle Ligainsider bezogen sind. Daher muss noch ein Mapping auf comunio
	 * erfolgen und die daten wie spielerId und link in die DB unter
	 * data.ligainsider eingetragen werden. Ausgabe als Map, wobei der Key die
	 * Ligainsider VereinsID ist. (nicht Comunio
	 * 
	 * @return
	 */
	private Map<String, List<InjuryAndBlockedData>> parseVerletzteUndGesperrte() {
		Map<String, List<InjuryAndBlockedData>> result = new HashMap<>();
		String url = Urls.LIGAINSIDER_INJURY;
		LOGGER.info("Lade verletzte und gesperrte Spieler von: " + url);
		try {

			// ... innerhalb deiner Methode
			Document doc = Jsoup.connect(url).get();

			// Alle Tabellen-Container selektieren
			Elements tables = doc.select(".personal_table.personal_table_top");

			for (Element table : tables) {
				List<InjuryAndBlockedData> clubList = new ArrayList<>();

				// 1. NUR den Vereinsnamen holen (aus dem h2 innerhalb von .leg_table_title)
				// .text() auf dem ganzen Container würde die Spaltenüberschriften mitnehmen
				String clubName = table.select(".leg_table_title h2").text().trim();

				Elements rows = table.select(".small_table_row");

				for (Element row : rows) {
					InjuryAndBlockedData resultObject = new InjuryAndBlockedData();
					if (row.select(".small_table_column_noentry").isEmpty()) {
						String playerName = row.select(".left_title").text().trim();

						// 2. Das Symbol (Alt-Text des Bildes)
						String statusSymbol = row.select(".left_icon img").attr("alt").trim();

						if (row.select(".small_table_column_noentry").isEmpty()) {

							// 1. Spieler-Link Element finden
							Element playerLinkElem = row.select(".small_table_column1 > a").first();
							String playerLink = "";
							String playerId = "";

							if (playerLinkElem != null) {
								// Vollständige URL (z.B. https://www.ligainsider.de/emre-can_1812/)
								playerLink = playerLinkElem.absUrl("href");

								// Relativer Pfad für die ID-Extraktion (z.B. /emre-can_1812/)
								String relativePath = playerLinkElem.attr("href");

								// 2. ID extrahieren (alles zwischen dem letzten Unterstrich '_' und dem letzten
								// Slash '/')
								if (relativePath.contains("_")) {
									// Extrahiert "1812" aus "/emre-can_1812/"
									playerId = relativePath.substring(relativePath.lastIndexOf("_") + 1, relativePath.lastIndexOf("/"));
								}
							}

							resultObject.setClub(clubName);
							resultObject.setPlayerName(playerName);
							resultObject.setLigainsiderPlayerId(playerId);
							resultObject.setPlayerLink(playerLink);
						}

						String grund = row.select(".small_table_column2").text().trim();

						// 3. News Text UND Link extrahieren
						Element newsElement = row.select(".small_table_column3 a").first();
						String newsText = "";
						String newsLink = "";
						if (newsElement != null) {
							newsText = newsElement.text().trim();
							// .absUrl("href") gibt den kompletten Link inkl. https://... zurück
							newsLink = newsElement.absUrl("href");
						}

						String dauer = row.select(".small_table_column4").text().trim();
						resultObject.setSinceString(dauer);
						resultObject.setReason(grund);
						resultObject.setStatus(statusSymbol);
						if (!newsLink.isEmpty()) {
							resultObject.setLastNewsLink(newsLink);
						}
						if (!newsText.isEmpty()) {
							resultObject.setLastNewsText(newsText);
						}
						clubList.add(resultObject);
					}

				}
				if (!clubList.isEmpty()) {
					result.put(clubName, clubList);

				}
			}
		} catch (Exception e) {
			LOGGER.warning("Fehler beim abfragen der Ligainsiderwebsite - VERLETZE & GESPERRTE Spieler -->" + url + " << FEHLER: " + e.getMessage());
		}
		return result;
	}


	/**
	 * Erstellt eine Instanz aus einem JSONObject mit der neuen Struktur:
	 * {
	 *   "injuriedAndBannedPlayer": { "playerId": { ...playerData... } },
	 *   "history": { "playerId": [{ ... }, ...] },
	 *   "lastUpdate": "..."
	 * }
	 */
	private static LigainsiderInjurieAndBannedPlayers fromJSON(JSONObject rootJson) {
	    LigainsiderInjurieAndBannedPlayers instance = new LigainsiderInjurieAndBannedPlayers();
	    if (rootJson == null || rootJson.isEmpty()) {
	        return instance;
	    }

	    // injuriedAndBannedPlayer
	    JSONObject injuredObj = rootJson.optJSONObject("injuriedAndBannedPlayer");
	    if (injuredObj != null) {
	        injuredObj.keySet().forEach(playerId -> {
	            JSONObject playerJson = injuredObj.getJSONObject(playerId);
	            InjuryAndBlockedData data = InjuryAndBlockedData.fromJSON(playerJson);
	            if (!data.getStatus().isAKTIV()) {
	                data.setComunioPlayerId(playerId);
	                instance.injuriedAndBannedPlayer.put(playerId, data);
	            }
	        });
	    }

	    // history
	    JSONObject historyObj = rootJson.optJSONObject("history");
	    if (historyObj != null) {
	        historyObj.keySet().forEach(playerId -> {
	            JSONArray historyArray = historyObj.optJSONArray(playerId);
	            if (historyArray != null) {
	                List<InjuryAndBlockedData> historyList = new ArrayList<>();
	                for (int i = 0; i < historyArray.length(); i++) {
	                    JSONObject histJson = historyArray.getJSONObject(i);
	                    InjuryAndBlockedData histData = InjuryAndBlockedData.fromJSON(histJson);
	                    histData.setComunioPlayerId(playerId);
	                    historyList.add(histData);
	                }
	                if (!historyList.isEmpty()) {
	                    instance.history.put(playerId, historyList);
	                }
	            }
	        });
	    }

	    // lastUpdate
	    if (rootJson.has("lastUpdate") && !rootJson.isNull("lastUpdate")) {
	        instance.lastUpdate = Instant.parse(rootJson.getString("lastUpdate"));
	    }

	    return instance;
	}

	/**
	 * Erstellt eine Instanz aus einem JSONArray von Spieler-Objekten.
	 * Jedes Objekt enthält das Feld "comunioPlayerId" als Identifier.
	 */
	private static LigainsiderInjurieAndBannedPlayers fromJSON(JSONArray jsonArray) {
	    LigainsiderInjurieAndBannedPlayers instance = new LigainsiderInjurieAndBannedPlayers();
	    for (int i = 0; i < jsonArray.length(); i++) {
	        JSONObject playerJson = jsonArray.getJSONObject(i);
	        String playerId = playerJson.optString("comunioPlayerId");
	        if (playerId == null || playerId.isEmpty()) {
	            continue;
	        }
	        InjuryAndBlockedData data = InjuryAndBlockedData.fromJSON(playerJson);
	        data.setComunioPlayerId(playerId);
	        if (!data.getStatus().isAKTIV()) {
	            instance.injuriedAndBannedPlayer.put(playerId, data);
	        }
	    }
	    return instance;
	}


	/**
	 * Dispatchet das übergebene JSON‑Objekt (entweder {@link JSONObject} oder
	 * {@link JSONArray}) an die jeweiligen overloads.
	 *
	 * @param obj JSON‑Daten (Objekt oder Array).
	 * @return neue Instanz von {@link LigainsiderInjurieAndBannedPlayers}.
	 */
	public static LigainsiderInjurieAndBannedPlayers fromJSON(Object obj) {
		if (obj instanceof JSONArray) {
			return fromJSON((JSONArray) obj);
		} else if (obj instanceof JSONObject) {
			return fromJSON((JSONObject) obj);
		} else {
			throw new IllegalArgumentException("Unsupported JSON type");
		}
	}

	/**
	 * Gibt die JSON‑Darstellung der gesamten Klasse zurück. Enthält die Maps
	 * {@code injuriedAndBannedPlayer} und {@code history} als verschachtelte
	 * JSON‑Objekte.
	 *
	 * @return JSON‑Objekt mit den internen Daten.
	 */
	public JSONObject toJSON() {
		JSONObject root = new JSONObject();

		// injuriedAndBannedPlayer
		JSONObject injured = new JSONObject();
		for (Map.Entry<String, InjuryAndBlockedData> e : injuriedAndBannedPlayer.entrySet()) {
			injured.put(e.getKey(), e.getValue().toJSON());
		}
		root.put("injuriedAndBannedPlayer", injured);

		// history (SpielerId → Liste von JSON‑Objekten)
		JSONObject historyMap = new JSONObject();
		for (Map.Entry<String, List<InjuryAndBlockedData>> e : history.entrySet()) {
			JSONArray list = new JSONArray();
			for (InjuryAndBlockedData data : e.getValue()) {
				list.put(data.toJSON());
			}
			historyMap.put(e.getKey(), list);
		}
		root.put("history", historyMap);
		root.put("lastUpdate", lastUpdate.toString());
		return root;
	}

	public Map<String, InjuryAndBlockedData> getInjuriedAndBannedPlayer() {
		return injuriedAndBannedPlayer;
	}

	public void setInjuriedAndBannedPlayer(Map<String, InjuryAndBlockedData> injuriedAndBannedPlayer) {
		this.injuriedAndBannedPlayer = injuriedAndBannedPlayer;
	}

	public void addInjuriedAndBannedPlayer(String playerId, InjuryAndBlockedData playerDetails) {
		if (this.injuriedAndBannedPlayer == null) {
			this.injuriedAndBannedPlayer = new HashMap<>();
		}
		if (playerId == null || playerId.equals("") || playerId.equals(" ") || playerId.isBlank()) {
			System.out.println();
		}
		this.injuriedAndBannedPlayer.put(playerId, playerDetails);
		LOGGER.info("Spieler hinzugefügt: " + playerDetails.toString());
	}

	public Map<String, List<InjuryAndBlockedData>> getHistory() {
		return history;
	}

	public void setHistory(Map<String, List<InjuryAndBlockedData>> history) {
		this.history = history;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	// Add this method (e.g., after addPlayerToHistory or before
	// parseVerletzteUndGesperrte)
	public void mergeStatus(String playerId, InjuryAndBlockedData newData, NewsManager newsManager) {
		if (playerId == null || playerId.isEmpty() || newData == null) {
			return;
		}

		InjuryAndBlockedData oldData = this.injuriedAndBannedPlayer.get(playerId);
		SpielerStatus newStatus = newData.getStatus();

		// Case: New status is AKTIV -> remove from injured map (if present) and log as
		// recovered
		if (newStatus == SpielerStatus.AKTIV) {
			if (oldData != null) {
				oldData.setStatusChangeString(new ComunioDate().toString());
				addPlayerToHistory(playerId, oldData);
				this.injuriedAndBannedPlayer.remove(playerId);
				// Generate news: player recovered
				if (newsManager != null) {
					String newsText = "Statuswechsel: " + oldData.getPlayerName() + " (" + playerId + ") ist wieder AKTIV";
					News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);
					if (!newsManager.contains(news)) {
						newsManager.addNews(news, true);
					}
				}
			}
			// If not in map, nothing to do (should not appear in injured list per
			// requirement)
			return;
		}

		// Case: New status is NOT AKTIV
		if (oldData == null) {
			// New injured/banned player
			this.injuriedAndBannedPlayer.put(playerId, newData);
			// Generate news: player became injured/banned
			if (newsManager != null) {
				String newsText = "Statuswechsel: " + newData.getPlayerName() + " (" + playerId + ") ist jetzt " + newStatus.toString();
				News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);
				if (!newsManager.contains(news)) {
					newsManager.addNews(news, true);
				}
			}
			return;
		}

		// Existing player in map
		if (oldData.getStatus().equals(newStatus)) {
			// No status change -> nothing to do
			return;
		}

		// Status changed (and neither is AKTIV, as handled above)
		oldData.setStatusChangeString(new ComunioDate().toString());
		addPlayerToHistory(playerId, oldData);

		// Update fields from newData (keep object reference same for map/history
		// consistency)
		oldData.setPlayerName(newData.getPlayerName());
		oldData.setClub(newData.getClub());
		oldData.setPlayerLink(newData.getPlayerLink());
		oldData.setLigainsiderPlayerId(newData.getLigainsiderPlayerId());
		oldData.setReason(newData.getReason());
		oldData.setLastNewsLink(newData.getLastNewsLink());
		oldData.setLastNewsText(newData.getLastNewsText());
		oldData.setSinceString(newData.getSinceString());
		oldData.setStatus(newStatus); // This is the key update
		oldData.setQuelle(newData.getQuelle());
		oldData.setComunioPlayerId(newData.getComunioPlayerId()); // Ensure ID is set

		// Generate news: status changed
		if (newsManager != null) {
			String newsText = "Statuswechsel: " + oldData.getPlayerName() + " (" + playerId + ") ist jetzt " + newStatus.toString();
			News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);
			if (!newsManager.contains(news)) {
				newsManager.addNews(news, true);
			}
		}
	}
}
