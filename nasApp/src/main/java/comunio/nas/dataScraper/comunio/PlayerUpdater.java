package comunio.nas.dataScraper.comunio;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.enu.NewsArt;
import comunio.nas.enu.Position;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.helper.PlayerDbHelper;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.orga.UpdaterContextData;
import comunio.nas.objects.player.Spielerstats;
import comunio.nas.objects.user.User;
import comunio.nas.util.HttpHeaderUtil;
import comunio.nas.util.player.PlayerHelper;

public class PlayerUpdater {

	private static final Logger LOGGER = LogManager.getLogger(PlayerUpdater.class);

	/**
	 * Aktualisiert die Spielerdatenbank basierend auf der Saison- und
	 * Spieltagssituation. Führt zuerst eine großflächige Detailaktualisierung
	 * durch, falls ein "Big Update" erforderlich ist (z.B. neue Saison, unbekannte
	 * letzte Aktualisierung oder neuer Matchday mit verfügbaren Punkten).
	 * Anschließend wird eine schnelle Aktualisierung über die Vereine durchgeführt.
	 * Messen und protokolliert die Ladezeiten für beide Vorgänge.
	 *
	 * @param isNewSeason         Gibt an, ob eine neue Saison begonnen hat.
	 * @param clubDB              JSONArray der Vereine aus der Datenbank.
	 * @param playerDBObject      JSONObject der Spielerdatenbank.
	 * @param marketValueDB       JSONArray der Marktwerte.
	 * @param playerToUserMap     Map der Spieler zu Benutzern.
	 * @param newsManager         Manager für News-Updates.
	 * @param currentMatchdayInfo Informationen zum aktuellen Spieltag.
	 * @param notInLigaDBObj      JSONObject der Nicht-im-Liga-Spieler.
	 * @param lastUpdates         Zeitpunkte der letzten Updates.
	 * @param user                Der Benutzer.
	 */
	public static void updatePlayers(boolean isNewSeason, JSONArray clubDB, JSONObject playerDBObject, JSONArray marketValueDB, Map<String, String> playerToUserMap, NewsManager newsManager, MatchdayInfo currentMatchdayInfo, JSONObject notInLigaDBObj, LastUpdates lastUpdates, User user) {

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.log(Level.WARNING, "KEINE SIELERDATENBANK vorhanden!");
			return;
		}

		String lastBigUpdate = lastUpdates.getPlayerDbFull() != null ? new ComunioDate(Date.from(lastUpdates.getPlayerDbFull())).toString() : null;
		int lastProcessedMatchday = playerDBObject.optInt("lastProcessedMatchday", 0);
		int currentMatchday = currentMatchdayInfo.getCurrentMatchday();

		LOGGER.log(Level.INFO, "Aktueller Spieltag: " + currentMatchday + ", zuletzt verarbeitet: " + lastProcessedMatchday);

		long startTime = System.nanoTime();

		// Konsolidierte Prüfung für den Big Update-Schritt
		if (shouldPerformBigUpdate(lastBigUpdate, isNewSeason, currentMatchdayInfo, lastProcessedMatchday)) {
			try {
				// Entscheidet an einem zentralen Ort, ob der schnelle Weg genommen wird
				Runnable detailsUpdater = ComunioDataUpdater.uld.isDebug() ? //
						() -> getDetailsForAllPlayerFast(clubDB, playerDBObject, marketValueDB, playerToUserMap, newsManager, currentMatchdayInfo, lastUpdates, user) : //
						() -> getDetailsForAllPlayer(clubDB, playerDBObject, marketValueDB, playerToUserMap, newsManager, currentMatchdayInfo, lastUpdates, user);
				detailsUpdater.run();
				LOGGER.log(Level.INFO, "Detailabfrage aller spieler durchgeführt.");
				return; 
			} catch (Exception e) {
				LOGGER.log(Level.INFO, "Exception bei der BigUpdate-Abfrage: ", e);
			}
		}
		System.out.println("Ladezeit Playerdaten (detail): " + (System.nanoTime() - startTime) / 1_000_000 + " ms");

		startTime = System.nanoTime();

		// Kurzabfrage über Clubs
		try {
			ComunioDate lastUpdate = lastUpdates.getPlayerDbShort() != null ? new ComunioDate(Date.from(lastUpdates.getPlayerDbShort())) : null;
			if (lastUpdate == null || lastUpdate.before(new ComunioDate()) || isNewSeason) {
				processClub(clubDB, playerDBObject, marketValueDB, playerToUserMap, newsManager, currentMatchdayInfo, notInLigaDBObj, lastUpdates);
				LOGGER.log(Level.INFO, "Spielerabfrage über Clubs durchgeführt!");
			}
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "Exception bei der Club-Abfrage: ", e);
		}

		System.out.println("Ladezeit Playerdaten (short): " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
	}

	/**
	 * Prüft, ob ein "Big Update" erforderlich ist. Ein solches ist erforderlich,
	 * wenn: - Die letzte Aktualisierung unbekannt ist. - Es den Start einer neuen
	 * Saison gibt. - Der aktuelle Matchday neu ist, Punkte verfügbar sind, noch
	 * nicht verarbeitet, und Punkteabruf möglich ist.
	 *
	 * @param lastBigUpdate         String-Darstellung des letzten Big Updates.
	 * @param isNewSeason           Gibt an, ob eine neue Saison begonnen hat.
	 * @param currentMatchdayInfo   Informationen zum aktuellen Spieltag.
	 * @param lastProcessedMatchday Der zuletzt verarbeitete Spieltag.
	 * @return true, wenn ein Big Update erforderlich ist, false sonst.
	 */
	private static boolean shouldPerformBigUpdate(String lastBigUpdate, boolean isNewSeason, MatchdayInfo currentMatchdayInfo, int lastProcessedMatchday) {
		if (lastBigUpdate == null || lastBigUpdate.isBlank() || isNewSeason) {
			return true;
		}
		int pointsMatchday = currentMatchdayInfo.getPointsMatchday();
		return pointsMatchday > 0 && currentMatchdayInfo.isNewMatchday(lastProcessedMatchday) && pointsMatchday != lastProcessedMatchday && currentMatchdayInfo.canFetchPoints();
	}

	public static void updateAllFromNotInLigaDb(JSONObject playerDBObject, JSONArray marketValueDB, Map<String, String> playerToUserMap, NewsManager newsManager, LastUpdates lastUpdates, User user) {
		JSONArray playerDb = playerDBObject.optJSONArray("playerDB");
		if (playerDb != null) {

			List<JSONObject> filteredPlayers = StreamSupport.stream(playerDb.spliterator(), false).map(JSONObject.class::cast).filter(player -> player.optBoolean("fromNotInLiga", false)).collect(Collectors.toList());

			// Jetzt hast du die genaue Anzahl:
			int count = filteredPlayers.size();
			LOGGER.info("NotInLigaDb Spieler zurückgeholt! Gefundene Spieler: " + count);
			LOGGER.info("NotInLigaDb Spieler Update wird durchgeführt");
			// Und verarbeitest sie anschließend:
			filteredPlayers.forEach(playerObject -> {
				loadPlayerData(playerObject, playerToUserMap, newsManager, playerDBObject, user, lastUpdates, marketValueDB);
				playerObject.remove("fromNotInLiga");
			});
		}
	}

	static void getDetailsForAllPlayer(JSONArray clubDB, JSONObject playerDBObject, JSONArray marketValueDB, Map<String, String> playerToUserMap, NewsManager newsManager, MatchdayInfo currentMatchdayInfo, LastUpdates lastUpdates, User user) {

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.log(Level.WARNING, "KEINE SIELERDATENBANK vorhanden!");
			return;
		}
		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.getJSONObject(i);
			loadPlayerData(player, playerToUserMap, newsManager, playerDBObject, user, lastUpdates, marketValueDB);

			// TODO: kann bald wieder raus.
			playerDBObject.getJSONObject("data").remove("lastUpdate");
			playerDBObject.getJSONObject("data").remove("lastBigUpdate");

			lastUpdates.setPlayerDbFull(Instant.now());
			lastUpdates.setPlayerDbShort(Instant.now());
			playerDBObject.put("lastProcessedMatchday", currentMatchdayInfo.getPointsMatchday());
			try {
				if (ComunioDataUpdater.uld.isDebug()) {
					Thread.sleep(200);
				} else {
					Thread.sleep(2000); // Rate-Limit beachten
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Führt eine Detailabfrage für alle Spieler durch. Im normalen Modus (NAS)
	 * sequenziell mit 1,5s Pause, im Debug-Modus (System Property
	 * {@code nas.debug=true}) parallel mit mehreren Threads für schnellere
	 * Durchführung. Die Thread-Sicherheit für {@code playerDBObject} und
	 * {@code playerToUserMap} wird dabei gewährleistet.
	 */
	static void getDetailsForAllPlayerFast(JSONArray clubDB, JSONObject playerDBObject, JSONArray marketValueDB, Map<String, String> playerToUserMap, NewsManager newsManager, MatchdayInfo currentMatchdayInfo, LastUpdates lastUpdates, User user) {

		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.log(Level.WARNING, "KEINE SIELERDATENBANK vorhanden!");
			return;
		}

		LOGGER.info("Debug-Modus: Parallele Abfrage aller Spieler");
		ConcurrentHashMap<String, String> concurrentPlayerMap = new ConcurrentHashMap<>(playerToUserMap);
		int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), playerDB.length());
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.optJSONObject(i);
			if (player == null)
				continue;
			final JSONObject currentPlayer = player;
			executor.submit(() -> {
				try {
					// Netzwerkaufruf parallel (keine Synchronisation nötig)
					JSONObject apiPlayer = fetchSpielerJson(currentPlayer.getString("id"), user);
					if (apiPlayer != null) {

						// Schreibzugriffe auf shared Strukturen (playerDBObject, playerToUserMap,
						// newsManager)
						synchronized (playerDBObject) {
							updateSpielerFromJson(currentPlayer, apiPlayer, concurrentPlayerMap, newsManager, playerDBObject, lastUpdates);
							updateMarketValueData(apiPlayer, marketValueDB);
						}
					}
				} catch (Exception e) {
					handleSpielerDataLoadingException(e);
				}
			});
		}

		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		// Gemeinsame Updates nach paralleler Verarbeitung
		synchronized (playerDBObject) {
			lastUpdates.setPlayerDbFull(Instant.now());
			lastUpdates.setPlayerDbShort(Instant.now());
			playerDBObject.put("lastProcessedMatchday", currentMatchdayInfo.getPointsMatchday());
		}
		lastUpdates.setPlayerDbFull(Instant.now());
		lastUpdates.setPlayerDbShort(Instant.now());

		// Original-Map aktualisieren
		playerToUserMap.clear();
		playerToUserMap.putAll(concurrentPlayerMap);

	}

	/**
	 * Verarbeitet alle Spieler eines Vereins: - Aktualisiert Punkte und
	 * Punktehistorie (nur bei abgeschlossenem Spieltag) - Aktualisiert
	 * Marktwert-Historie (täglich)
	 */
	static void processClub(JSONArray clubDB, JSONObject playerDBObject, JSONArray marketValueDB, Map<String, String> playerToUserMap, NewsManager newsManager, MatchdayInfo currentMatchdayInfo, JSONObject notInLigaDBObj, LastUpdates lastUpdates) {
		LOGGER.log(Level.INFO, "Spielerdaten von jedem Club werden geladen...");
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			LOGGER.log(Level.WARNING, "KEINE SIELERDATENBANK vorhanden!");
			return;
		}
		Set<String> updatedPlayerIds = new HashSet<>();
		// Vereine verarbeiten
		for (int i = 0; i < clubDB.length(); i++) {
			JSONObject club = clubDB.getJSONObject(i);
			if (club.getBoolean("inLiga") && !club.getString("id").equals("0")) {
				LOGGER.info("Verarbeite Verein: " + club.getString("name"));
				String clubId = club.getString("id");
				// nach dem ersten durchgang erst den Sleep machen!
				if (i > 0) {
					try {
						if (ComunioDataUpdater.uld.isDebug()) {
							Thread.sleep(200);
						} else {
							Thread.sleep(1500); // Rate-Limit beachten
						}
					} catch (InterruptedException e) {
						LOGGER.log(Level.WARNING, "Fehler beim ThreadSleep!", e);
						e.printStackTrace();
					}
				}
				try {
					String url = Urls.COM_CLUB + clubId;
					Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
					String jsonResponse = Jsoup.connect(url)//
							.userAgent(HttpHeaderUtil.getRandomUserAgent())//
							.header("Accept", "application/json, text/plain, */*")//
							.header("Authorization", "Bearer " + Login.getToken())//
							.header("Accept-Encoding", "gzip, deflate, br, zstd")//
							.header("Accept-Language", "de-DE,en-EN;q=0.9")//
							.header("x-timezone", "Europe/Berlin")//
							.ignoreContentType(true)//
							.execute()//
							.body();

					JSONObject clubData = new JSONObject(jsonResponse);
					JSONArray squad = clubData.getJSONArray("squad");
					LOGGER.info("Gefunden: " + squad.length() + " Spieler für Verein-ID: " + clubId);

					for (int j = 0; j < squad.length(); j++) {
						JSONObject apiPlayer = squad.getJSONObject(j);
						boolean updateOk = true; 
						updateOk = updatePlayerData(playerDBObject, apiPlayer, playerToUserMap, newsManager, currentMatchdayInfo, clubDB, notInLigaDBObj, lastUpdates);
						updateMarketValueData(apiPlayer, marketValueDB);
						if(updateOk) {
							String apiPlayerId = PlayerHelper.convertIdToString(apiPlayer.get("id"));
							updatedPlayerIds.add(apiPlayerId);
						}
					}

					lastUpdates.setPlayerDbShort(Instant.now());
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Fehler beim Verarbeiten des Vereins " + clubId + ": " + e.getMessage(), e);
				}

			}
		}
		LOGGER.info("Clubupdater durchgelaufen. Anzahl Spieler die geupdatet wurden: "+ updatedPlayerIds.size() + " - Anzahl Datenbank: " + playerDB.length());
		PlayerDbHelper.checkPlayersSetWithDbAndUpdateSingel( playerDBObject, updatedPlayerIds, playerToUserMap, newsManager, lastUpdates, marketValueDB);	
		LOGGER.info("Nachprüfung durchgeführt. Alle spieler überprüft, ob sie in der Liga sind.");

	}
	
	

	/**
	 * Aktualisiert die Punkte und Spieltagspunkte eines Spielers. Punkte werden nur
	 * bei abgeschlossenem Spieltag verarbeitet.
	 */
	private static boolean updatePlayerData(JSONObject playerDBObject, JSONObject apiPlayer, Map<String, String> playerToUserMap, NewsManager newsManager, MatchdayInfo currentMatchdayInfo, JSONArray clubDB, JSONObject notInLigaDBObj, LastUpdates lastUpdates) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		try {
			StringBuilder log = new StringBuilder();
			log.append("Update Playerdata wird gestartet...").append(System.lineSeparator());
			String apiPlayerId = PlayerHelper.convertIdToString(apiPlayer.get("id"));

			JSONObject player = PlayerHelper.findPlayerByComunioId(playerDB, apiPlayerId, notInLigaDBObj);
			JSONObject data = new JSONObject();
			String playerName = null;

			if (player == null) {
				log.append("Creating new player record: " + apiPlayerId).append(System.lineSeparator());

				playerName = apiPlayer.getString("name");
				String verein = PlayerHelper.convertIdToString(apiPlayer.getJSONObject("club").get("id"));
				if (verein.equals("61") || verein.equals("0")) {
					return false;
				}
				player = PlayerHelper.createNewEmptyPlayerJSON(apiPlayerId, playerName);
				data = player.optJSONObject("data");
				// Data-Objekt mit Standardwerten
				data.put("position", Position.fromString(apiPlayer.getString("position")).toString());
				data.put("verein", verein);

				int points = apiPlayer.getInt("points");
				int lastPoints = apiPlayer.optInt("lastPoints");
				addPointsToPlayerData(points, lastPoints, data, lastUpdates, currentMatchdayInfo);

				setWertAndLastwert(data, apiPlayer);

				playerToUserMap.put(apiPlayerId, "1");
				GitHubUploader.mappingChanged = true;

//				TmDePlayerDataUpdater.getTmDeDataForPlayer(player, clubDB, false, log, lastUpdates, );
				playerDB.put(player);

				// News: Neuer Spieler
				newsManager.addNews(new News(NewsArt.NEW_PLAYER, "Neuer Spieler: " + apiPlayer.getString("name") + " (ID: " + apiPlayerId + ")", apiPlayerId), true);
			} else {

				playerName = apiPlayer.getString("name");
				log.append("Player found: " + playerName + " --> start Update....").append(System.lineSeparator());
				data = player.getJSONObject("data");
				String oldPosition = data.getString("position");
				Position newPosition = Position.fromString(apiPlayer.getString("position"));
				String oldClubId = data.getString("verein");
				String newClubId = PlayerHelper.convertIdToString(apiPlayer.getJSONObject("club").get("id"));

				// Positionswechsel prüfen
				if (!oldPosition.equals(newPosition.toString())) {
					newsManager.addNews(News.getPositionswechsel(oldPosition, newPosition.toString(), apiPlayerId, playerName), true);
				}

				// Vereinswechsel prüfen
				if (!oldClubId.equals(newClubId)) {
					newsManager.addNews(News.getVereinswechsel(oldClubId, newClubId, apiPlayerId, playerName), true);
				}

				// Daten aktualisieren
				data.put("position", newPosition.toString());
				data.put("verein", newClubId);

				int totalPoints = extractTotalPoints(apiPlayer);

				addPointsToPlayerData(totalPoints, null, data, lastUpdates, currentMatchdayInfo);

				setWertAndLastwert(data, apiPlayer);

			}

			// Punktehistorie-Logik
			if (currentMatchdayInfo != null && currentMatchdayInfo.isFinished()) {
				updateSpielerPointsFromJson(playerDBObject, data, apiPlayer, lastUpdates);
			}
			
			System.out.println("Spieler: " + playerName + " (ID: " + apiPlayerId + ") erfolgreich aktualisiert!");
			return true; 
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error updating player data: " + e.getMessage(), e);
			return false; 
		}
	}

	private static void setWertAndLastwert(JSONObject data, JSONObject apiPlayer) {
		int newWert = 0;
		if (apiPlayer.has("quotedprice")) {
			newWert = apiPlayer.getInt("quotedprice");
		} else {
			newWert = apiPlayer.getInt("price");
		}
		int oldWert = data.has("wert") ? data.getInt("wert") : -1;
		if (newWert != oldWert) {
			data.put("lastWert", oldWert);
			data.put("wert", newWert);
		}
	}

	/**
	 * Hilfsmethode um die punkte je nach saisonstand an die korrekte stelle zu
	 * setzten. wenn es zwischen den Saisons ist, sollen die punkte 0 sein und die
	 * aktuelle ausgelesenen punkte sollen zu LastSeasonPoints
	 */
	private static void addPointsToPlayerData(Integer points, Integer lastPoints, JSONObject data, LastUpdates lastUpdates, MatchdayInfo matchdayInfo) {
		// punkte auf 0 wenn saison noch nicht los ging.
		if ((matchdayInfo.getCurrentMatchday() == 999 || matchdayInfo.getCurrentMatchday() == 1 && !matchdayInfo.isFinished()) || matchdayInfo.isStuckBetweenTheSeasons(lastUpdates)) {
			data.put("punkte", 0);
			data.put("lastPoints", 0);
			if (points > 0) {
				data.put("lastSeasonPoints", points);
			}
		} else {
			if (points != null) {
				data.put("punkte", points);
			}

			if (lastPoints != null) {
				data.put("lastPoints", lastPoints);
			}

		}
	}

	/**
	 * Aktualisiert die Marktwert-Daten eines Spielers in der Datenbank.
	 * 
	 * Speichert den aktuellen Marktwert für das heutige Datum, wenn sich der Wert
	 * gegenüber dem letzten Eintrag geändert hat oder wenn seit dem letzten Eintrag
	 * mindestens 5 Tage mit gleichem Wert vergangen sind. Zudem wird
	 * sichergestellt, dass maximal 150 Einträge pro Spieler gespeichert sind,
	 * ältere Einträge werden bei Überschreitung gelöscht.
	 *
	 * @param apiPlayer     JSONObject mit aktuellen Spielerinfos inklusive "id" und
	 *                      "quotedprice"
	 * @param marketValueDB JSONArray mit allen Marktwert-Daten der Spieler
	 */
	public static void updateMarketValueData(JSONObject apiPlayer, JSONArray marketValueDB) {
		try {
			// Spieler-ID als String aus API-Daten
			int id = 0;
			if (apiPlayer.has("id")) {
				id = apiPlayer.getInt("id");
			} else {
				apiPlayer.getInt("playerId");
			}
			// Spieler-ID als String aus API-Daten
			String playerId = PlayerHelper.convertIdToString(id);
			// Aktueller Marktwert

			int currentValue = 0;
			if (apiPlayer.has("quotedprice")) {
				currentValue = apiPlayer.getInt("quotedprice");
			} else {
				currentValue = apiPlayer.getInt("price");
			}
			// Datum heute (dd.MM.yyyy)
			String currentDate = new ComunioDate().toString();

			// Suche bestehendes Marktwertobjekt des Spielers
			JSONObject playerMarketValue = PlayerHelper.findPlayerByComunioId(marketValueDB, playerId, null);
			if (playerMarketValue == null) {
				// Wenn nicht gefunden, neues Objekt anlegen und in DB speichern
				playerMarketValue = new JSONObject();
				playerMarketValue.put("id", playerId);
				playerMarketValue.put("name", apiPlayer.getString("name"));
				playerMarketValue.put("data", new JSONObject());
				playerMarketValue.getJSONObject("data").put("normal", new JSONArray());
				playerMarketValue.getJSONObject("data").put("plus", new JSONArray());
				marketValueDB.put(playerMarketValue);
			}

			// Normaler Wertedaten-Array für Spieler
			JSONArray normalValues = playerMarketValue.getJSONObject("data").getJSONArray("normal");

			boolean addEntry = true;

			if (normalValues.length() > 0) {
				JSONObject lastEntry = normalValues.getJSONObject(0); // Neuester Eintrag zuerst
				String lastDateStr = lastEntry.getString("date");
				int lastValue = lastEntry.getInt("wert");

				if (lastDateStr.equals(currentDate)) {
					// Schon Eintrag für heute vorhanden
					if (lastValue == currentValue) {
						// Wert unverändert, nichts ändern
						addEntry = false;
					} else {
						// Wert für heute geändert: Update des Eintrags
						lastEntry.put("wert", currentValue);
						addEntry = false;
					}
				} else if (lastValue == currentValue) {
					// Wert unverändert zum letzten Eintrag
					// Prüfe, ob mindestens 5 Tage seit letztem Eintrag vergangen sind
					java.time.LocalDate lastDate = java.time.LocalDate.parse(lastDateStr, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
					java.time.LocalDate currentLocalDate = java.time.LocalDate.parse(currentDate, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
					long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastDate, currentLocalDate);

					if (daysDiff >= 5) {
						// Seit letztem identischem Wert 5 oder mehr Tage → neuen Eintrag anlegen
						addEntry = true;
					} else {
						// Weniger als 5 Tage → keinen neuen Eintrag
						addEntry = false;
					}
				}
			}

			if (addEntry) {
				JSONObject newEntry = new JSONObject();
				newEntry.put("date", currentDate);
				newEntry.put("wert", currentValue);

				// Neuen Eintrag vorne in Array einfügen
				JSONArray updatedValues = new JSONArray();
				updatedValues.put(newEntry);
				for (int i = 0; i < normalValues.length(); i++) {
					updatedValues.put(normalValues.getJSONObject(i));
				}

				// Begrenzung auf 150 Einträge — älteste entfernen
				if (updatedValues.length() > 150) {
					JSONArray trimmedValues = new JSONArray();
					for (int i = 0; i < 150; i++) {
						trimmedValues.put(updatedValues.getJSONObject(i));
					}
					updatedValues = trimmedValues;
				}

				playerMarketValue.getJSONObject("data").put("normal", updatedValues);
			}

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Fehler beim Aktualisieren des Marktwerts: " + e.getMessage(), e);
		}
	}

	public static void updateEachComunioPlayer(JSONArray playerDB, Map<String, String> playerToUserMap, NewsManager newsManager, JSONObject playerDBObject, LastUpdates lastUpdates, User user, JSONArray marketValueDB) {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		// Map thread-sicher machen
		ConcurrentHashMap<String, String> playerToUserMap2 = new ConcurrentHashMap<>(playerToUserMap);

		for (int i = 0; i < playerDB.length(); i++) {
			JSONObject player = playerDB.optJSONObject(i);

			if (player == null) {
				LOGGER.warning("Kein 'Spieler'-Objekt vorhanden ");
				continue;
			}

			executor.submit(() -> {
				loadPlayerData(player, playerToUserMap2, newsManager, playerDBObject, user, lastUpdates, marketValueDB);
			});
		}

		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		lastUpdates.setPlayerDbFull(Instant.now());
		lastUpdates.setPlayerDbShort(Instant.now());

		// Ursprüngliche Map aktualisieren:
		playerToUserMap.clear();
		playerToUserMap.putAll(playerToUserMap2);
	}

	/**
	 * Spieler daten werden über diesen Link geladen:
	 * https://www.comunio.de/api/players/32684 Der Link m�sste aber auch schon in
	 * den spielerdaten sein!
	 * 
	 * @param marketValueDB
	 * 
	 * 
	 * @param spieler       Der Spieler, dessen Daten geladen werden sollen.
	 * @param db            Die Datenbank, die verwendet wird.
	 * @param loader        Der Loader, der verwendet wird.
	 * @return Der Spieler mit den geladenen Daten.
	 */
	public static void loadPlayerData(JSONObject player, Map<String, String> playerToUserMap, NewsManager newsManager, JSONObject playerDBObject, User user, LastUpdates lastUpdates, JSONArray marketValueDB) {
		if (player == null || player.getString("id") == null || player.getString("id").isEmpty()) {
			handleInvalidSpieler(player);
			return;
		}
		try {
			JSONObject apiPlayer = fetchSpielerJson(player.getString("id"), user);
			if (apiPlayer != null) {
				updateSpielerFromJson(player, apiPlayer, playerToUserMap, newsManager, playerDBObject, lastUpdates);
				updateMarketValueData(apiPlayer, marketValueDB);
			}
		} catch (Exception e) {
			handleSpielerDataLoadingException(e);
			return;
		}
	}

	/**
	 * Behandelt ungültige Spieler.
	 * 
	 * @param spieler Der Spieler, der überprüft wird.
	 * @param db      Die Datenbank, die verwendet wird.
	 */
	private static void handleInvalidSpieler(JSONObject player) {
		if (player == null) {
			LOGGER.warning("Spielerobject ist null.");
		} else if (player.getString("name") == null && player.getString("id") == null) {
			LOGGER.warning("Spieler: ID und Name sind null.");
		} else if (player.getString("name") == null) {
			LOGGER.warning("Spieler: NAME ist null. ID: " + (player.getString("id")));
		} else if (player.getString("id") == null) {
			LOGGER.warning("Spieler: ID ist null. NAME: " + (player.getString("name")));
		}
	}

	/**
	 * Ruft das Spieler-JSON ab.
	 * 
	 * @param spieler Der Spieler, dessen Daten abgerufen werden.
	 * @param db      Die Datenbank, die verwendet wird.
	 * @param loader  Der Loader, der verwendet wird.
	 * @return Das Spieler-JSON.
	 * @throws Exception Wenn ein Fehler auftritt.
	 */
	private static JSONObject fetchSpielerJson(String playerID, User user) {
		try {

			String url = "https://www.comunio.de/api/communities/" + ComunioDataUpdater.community.getId() + "/users/" + user.getId() + "/players/" + playerID;
			Login.ensureValidToken(ComunioDataUpdater.uld.getUsername(), ComunioDataUpdater.uld.getPasswortAlsString());
			String jsonResponse = Jsoup.connect(url)//
					.userAgent(HttpHeaderUtil.getRandomUserAgent())//
					.header("Accept", "application/json, text/plain, */*")//
					.header("Authorization", "Bearer " + Login.getToken())//
					.header("Accept-Encoding", "gzip, deflate, br, zstd")//
					.header("Accept-Language", "de-DE,en-EN;q=0.9")//
					.header("x-timezone", "Europe/Berlin")//
					.ignoreContentType(true)//
					.execute()//
					.body();

			JSONObject playerData = new JSONObject(jsonResponse);

			LOGGER.info("Comunio-Daten für Spieler " + playerData.getString("name") + " (ID: " + playerID + ") erfoglreich ABGEFRAGT! Verarbeitung startet!");

			return playerData;
		} catch (Exception e) {
			LOGGER.warning("Comunio-Daten für Spieler ID: " + playerID + "NICHT geladen! | Exception: " + e.getMessage());
			return null;
		}

	}

	/**
	 * Aktualisiert den Spieler anhand des JSON.
	 * 
	 * @param spieler Der Spieler, der aktualisiert wird.
	 * @param json    Das JSON, das verwendet wird.
	 * @param db      Die Datenbank, die verwendet wird.
	 */
	private static void updateSpielerFromJson(JSONObject player, JSONObject apiPlayer, Map<String, String> playerToUserMap, NewsManager newsManager, JSONObject playerDBObject, LastUpdates lastUpdates) {
		JSONObject old = new JSONObject(player.toString());
		String playerId, playerName, oldClub, oldOwner;
		playerId = player.getString("id");
		playerName = player.getString("name");
		oldOwner = playerToUserMap.get(playerId);

		JSONObject data = player.optJSONObject("data");
		if (data == null) {
			data = new JSONObject();
		}

		JSONObject oldData = old.optJSONObject("data");

		oldClub = data.getString("verein");

		boolean oldDataFaild = false;
		if (oldData == null) {
			oldData = new JSONObject();
			oldDataFaild = true;
		}
		try {
			setWertAndLastwert(data, apiPlayer);

			Position pos = Position.fromString(apiPlayer.getString("type"));
			data.put("position", pos.toString());
			if (!oldDataFaild && !oldData.getString("position").equals(data.getString("position"))) {
				News news = News.getPositionswechsel(oldData.getString("position"), pos.toString(), playerId, playerName);// new News(NewsArt.POSITIONSWECHSEL, job.toString(), playerId);
				newsManager.addNews(news, true);

			}
			Spielerstats stats = Spielerstats.fromJSON(apiPlayer.getJSONObject("general"));
			updateSpielerStatsFromJson(stats, apiPlayer);
			updateSpielerCardsFromJson(stats, apiPlayer);
			data.put("stats", stats.toJSON());

			updateSpielerHistoryFromJson(data, apiPlayer);
			
			String status = apiPlayer.has("status") ? apiPlayer.getString("status") : "no";
			
			data.put("retired", "RETIRED".equals(status) ? true : false);
			

			updateSpielerPointsFromJson(playerDBObject, data, apiPlayer, lastUpdates);

			lastUpdates.setPlayerDbFull(Instant.now());
			lastUpdates.setPlayerDbShort(Instant.now());

			int wert = 0;
			if (apiPlayer.has("quotedprice")) {
				wert = apiPlayer.getInt("quotedprice");
			} else {
				wert = apiPlayer.getInt("price");
			}
			try {
				String newOwnerID = String.valueOf(apiPlayer.getJSONObject("owner").getInt("id"));
				if (!newOwnerID.equals(oldOwner)) {
					if (UpdaterContextData.ownerList.contains(newOwnerID)) {
						JSONObject newsText = new JSONObject();
						newsText.put("playerName", playerName);
						newsText.put("playerId", playerId);
						newsText.put("playerValue", wert);
						newsText.put("seller", oldOwner);
						newsText.put("buyer", newOwnerID);

						if (oldOwner == null || oldOwner.isBlank() || oldOwner.equals("")) {
							newsText.put("info", "neuer Spieler. war noch nicht in der Liste drin!");
							newsText.put("seller", oldOwner);
						}

						String text = newsText.toString();
						News news = new News(NewsArt.OWNERCHANGE, text, playerId);
						playerToUserMap.put(playerId, newOwnerID);
						GitHubUploader.mappingChanged = true;
						if (!newsManager.contains(news)) {
							newsManager.addNews(news, true);
						}
					} else {
						LOGGER.log(Level.WARNING, "beim update wurde ein spieler einem User zugeordnet, der nicht meht spielt. vermutlich wurde er entfernt. ");
					}

				}
				String newClubID = String.valueOf(apiPlayer.getJSONObject("club").getInt("id"));
				if(newClubID.equals("61")) {
					newClubID = "0";
				}

				// Vereinswechsel prüfen
				if (!oldClub.equals(newClubID)) {
					String apiPlayerId = String.valueOf(apiPlayer.getInt("playerId"));
					data.put("verein", newClubID);
					newsManager.addNews(News.getVereinswechsel(oldClub, newClubID, apiPlayerId, playerName), true);
				}

			} catch (Exception e) {
				LOGGER.warning("FEHLER BEIM ERMITTELN DES OWNERS von Comunio!!!! " + apiPlayer.toString() + " | Exception: " + e.getMessage());
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.warning("Fehler bei updateSpielerFromJson bei Spieler: " + playerName + " | Exception: " + e.getStackTrace());
		}
	}

	/**
	 * Aktualisiert die Spielerstatistiken anhand des JSON.
	 * 
	 * @param spieler Der Spieler, dessen Statistiken aktualisiert werden.
	 * @param json    Das JSON, das verwendet wird.
	 */
	private static void updateSpielerStatsFromJson(Spielerstats stats, JSONObject apiPlayer) {
		if (stats == null) {
			stats = new Spielerstats();
		}
		JSONObject durchschnitt = apiPlayer.getJSONObject("average");
		stats.setNotenDurchschnitt(durchschnitt.getString("grade"));
		stats.setPunkteDurchschnitt(durchschnitt.getString("points"));
	}

	/**
	 * Aktualisiert die Spielerhistorie anhand des JSON.
	 * 
	 * @param spieler Der Spieler, dessen Historie aktualisiert wird.
	 * @param json    Das JSON, das verwendet wird.
	 */
	private static void updateSpielerHistoryFromJson(JSONObject playerData, JSONObject apiPlayer) {
		JSONArray punkteProSaison = apiPlayer.getJSONObject("historical").getJSONArray("points");
		if (!punkteProSaison.isEmpty()) {
			schleife: {
				for (Object ob : punkteProSaison) {
					JSONObject jOb = (JSONObject) ob;
					if (jOb.has("points") && jOb.has("season")) {
						String po = jOb.getString("points");
						if (!po.equals("-") && !po.isBlank()) {
							try {
								int pun = Integer.valueOf(po);
								if (pun > 0 || pun < 0) {
									String season = "20" + jOb.getString("season").split("/")[0];
									JSONArray arrHist = playerData.optJSONArray("historicalPoints");
									if (arrHist == null) {
										arrHist = new JSONArray();
									}
									for (int i = 0; i < arrHist.length(); i++) {
										JSONObject xxx = arrHist.getJSONObject(i);
										if (xxx.has(season)) {
											break schleife; // ich möchte, nicht, dass ein Eintrag doppelt vorkommt!
										}
									}
									JSONObject saisonJob = new JSONObject();
									saisonJob.put(season, pun);
									arrHist.put(saisonJob);
									playerData.put("historicalPoints", arrHist);
								}
							} catch (NumberFormatException ne) {
								LOGGER.warning("Fehler bei updateSpielerHistoryFromJson bei Spieler | Exception: " + ne.getStackTrace());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Aktualisiert die Spielerkarten anhand des JSON.
	 * 
	 * @param spieler Der Spieler, dessen Karten aktualisiert werden.
	 * @param json    Das JSON, das verwendet wird.
	 */
	private static void updateSpielerCardsFromJson(Spielerstats stats, JSONObject apiPlayer) {

		JSONObject karten = apiPlayer.getJSONObject("cards");
		stats.setGelbekarten(karten.getInt("yellow"));
		stats.setRotekarten(karten.getInt("red"));
		stats.setGelbrotekarten(karten.getInt("yellowRed"));
	}

	/**
	 * Aktualisiert alle relevanten Punktefelder und die Punktehistorie des Spielers
	 * anhand der API-Response, unabhängig davon, wie die Punkte im
	 * zugrundeliegenden JSON benannt oder strukturiert sind.
	 *
	 * @param playerData Das interne Spieler-Objekt, das aktualisiert wird (inkl.
	 *                   History etc.)
	 * @param apiPlayer  Die externe Datenquelle für diesen Spieler aus der API
	 *                   (verschiedene Formate möglich)
	 */
	public static void updateSpielerPointsFromJson(JSONObject playerDBObject, JSONObject playerData, JSONObject apiPlayer, LastUpdates lastUpdates) {
		try {
			// Den aktuellen Spieltag bestimmen (externe Methode; kann auch injizierbar
			// sein)
			MatchdayInfo matchdayInfo = MatchdayInfo.fetchCurrentMatchday();
			if (matchdayInfo.getPointsMatchday() == 0) {
				LOGGER.warning("Aktueller Spieltag konnte nicht ermittelt werden. Punkteaktualisierung wird übersprungen.");
				return;
			}

			int aktuellerSpieltag = matchdayInfo.getPointsMatchday();

			// 1. Punkte-Historie abrufen oder anlegen
			// Prüfen, ob für aktuellen Spieltag schon Punkte gespeichert sind
			JSONArray punkteHistorie = playerData.optJSONArray("spieltagspunkte");
			if (punkteHistorie != null) {
				for (int i = 0; i < punkteHistorie.length(); i++) {
					JSONObject eintrag = punkteHistorie.getJSONObject(i);
					if (eintrag.optInt("key", -1) == aktuellerSpieltag) {
						// Punkte für aktuellen Spieltag sind schon vorhanden - nichts tun
						return;
					}
				}
			} else {
				punkteHistorie = new JSONArray();
				playerData.put("spieltagspunkte", punkteHistorie);
			}

			// 2. Gesamtpunkte flexibel auslesen (über Hilfsmethode, robust für alle
			// Response-Typen)
			Integer totalPoints = extractTotalPoints(apiPlayer);

			// 3. Letzten gespeicherten Eintrag der Historie auslesen (für Delta- und
			// Lückenerkennung)
			int letzterGespeicherterSpieltag = 0;
			Integer letzteGespeicherteGesamtpunkte = null;
			if (punkteHistorie.length() > 0) {
				JSONObject lastEntry = punkteHistorie.getJSONObject(punkteHistorie.length() - 1);
				letzterGespeicherterSpieltag = lastEntry.optInt("key", 0);
				letzteGespeicherteGesamtpunkte = getNullableIntFromJson(lastEntry, "totalPoints");
			}

			// 4. Punkte dieses Spieltags auslesen, wenn vorhanden
			Integer lastPoints = extractLastPoints(apiPlayer);

			// 5. Gesamt- und Spieltagspunkte im Spielerobjekt persistieren
			addPointsToPlayerData(totalPoints, lastPoints, playerData, lastUpdates, matchdayInfo);

			// 6. Prüfen, ob für aktuellen Spieltag diese Daten schon gespeichert wurden
			boolean punktSchonVorhanden = false;
			for (int i = 0; i < punkteHistorie.length(); i++) {
				JSONObject obj = punkteHistorie.getJSONObject(i);
				if (obj.optInt("key") == aktuellerSpieltag) {
					punktSchonVorhanden = true;
					break;
				}
			}

			// 7. Neuen Eintrag für aktuellen Spieltag hinzufügen (je nach Datenlage)
			if (!punktSchonVorhanden) {
				JSONObject neuerEintrag = new JSONObject();
				neuerEintrag.put("key", aktuellerSpieltag);
				if (lastPoints != null) {
					// Standardfälle: Es gibt "lastPoints" (=Punkte des aktuellen Spieltags)
					neuerEintrag.put("value", lastPoints);
				} else if (letzteGespeicherteGesamtpunkte != null && totalPoints != null) {
					// Fallback: lastPoints fehlt. Differenz aus totalPoints seit dem letzten
					// gespeicherten Wert berechnen
					neuerEintrag.put("value", totalPoints - letzteGespeicherteGesamtpunkte);
				}
				if (totalPoints != null) {
					neuerEintrag.put("totalPoints", totalPoints);
				}
				if (neuerEintrag.has("value")) {
					punkteHistorie.put(neuerEintrag);
				}

			}

			// 8. Lückenerkennung und Auffüllen der Punktehistorie
			if (letzterGespeicherterSpieltag > 0 && letzterGespeicherterSpieltag < aktuellerSpieltag - 1 && totalPoints != null && letzteGespeicherteGesamtpunkte != null) {
				int fehlendeSpieltage = aktuellerSpieltag - letzterGespeicherterSpieltag - 1;
				int punkteDifferenz = totalPoints - letzteGespeicherteGesamtpunkte;
				if (fehlendeSpieltage > 0 && punkteDifferenz > 0) {
					fuelleLueckeMitDifferenz(punkteHistorie, letzterGespeicherterSpieltag, aktuellerSpieltag, letzteGespeicherteGesamtpunkte, totalPoints);
					playerData.put("spieltagspunkte", punkteHistorie);
				}
			}

			// 9. Letzten verarbeiteten Spieltag speichern (für zukünftige Vergleichslogik)
			playerDBObject.put("lastProcessedMatchday", aktuellerSpieltag);
			// Hier explizit speichern:
			playerData.put("spieltagspunkte", punkteHistorie);
		} catch (Exception e) {
			// Fehlerbehandlung: Logging oder ggf. eigene Exception werfen
			System.err.println("Fehler beim Aktualisieren der Spielerpunkte: " + e.getMessage());
			// e.printStackTrace(); // Für Debugging
		}
	}

	/**
	 * Extrahiert die Gesamtpunkte aus einem beliebig strukturierten
	 * Api-Response-Objekt. Robust gegen unterschiedliche Feldbenennungen und
	 * Datentypen.
	 *
	 * @param apiPlayer Das Spielerobjekt aus der externen API
	 * @return Integer-Wert der Gesamtpunkte (null = Feld fehlt oder ungültig)
	 */
	private static Integer extractTotalPoints(JSONObject apiPlayer) {
		// Priorität: totalPoints > points (falls KEIN lastPoints) > points (immer noch
		// als fallback)
		if (apiPlayer.has("totalPoints"))
			return getNullableIntFromJson(apiPlayer, "totalPoints");
		if (apiPlayer.has("points") && !apiPlayer.has("lastPoints"))
			return getNullableIntFromJson(apiPlayer, "points");
		if (apiPlayer.has("points"))
			return getNullableIntFromJson(apiPlayer, "points");
		return null;
	}

	/**
	 * Extrahiert die Punkte für den aktuellen Spieltag aus einem beliebig
	 * strukturierten Api-Response-Objekt.
	 *
	 * @param apiPlayer Das Spielerobjekt aus der externen API
	 * @return Integer der Spieltagspunkte (null = Feld fehlt oder ungültig)
	 */
	private static Integer extractLastPoints(JSONObject apiPlayer) {
		if (apiPlayer.has("lastPoints"))
			return getNullableIntFromJson(apiPlayer, "lastPoints");

		// Für API-Varianten, die statt lastPoints ein historical-Array haben:
		if (apiPlayer.has("historical")) {
			JSONObject historical = apiPlayer.optJSONObject("historical");
			if (historical != null && historical.has("points")) {
				JSONArray pointsArray = historical.optJSONArray("points");
				if (pointsArray != null && pointsArray.length() > 0) {
					JSONObject last = pointsArray.getJSONObject(pointsArray.length() - 1);
					return getNullableIntFromJson(last, "points");
				}
			}
		}
		// Fallback: points könnte Spieltagspunkte sein (wenn totalPoints fehlt)
		return null;
	}

	/**
	 * Liest einen Integerwert aus einem JSON-Feld, gibt null zurück, wenn das Feld
	 * fehlt oder nicht konvertiert werden kann.
	 *
	 * @param json Das JSONObject
	 * @param key  Das Keyfeld
	 * @return Integer oder null
	 */
	private static Integer getNullableIntFromJson(JSONObject json, String key) {
		if (!json.has(key))
			return null;
		Object value = json.opt(key);
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	/**
	 * Füllt Lücken in der Punktehistorie mit interpolierten Einzelspieltagspunkten
	 * auf, sodass für jeden Spieltag ein Eintrag existiert.
	 *
	 * @param punkteHistorie   Die Punktehistorie-JSONArray, in die eingefügt wird
	 * @param vonSpieltag      Vorheriger gespeicherter Spieltag (z.B. 5)
	 * @param bisSpieltag      Aktueller Spieltag (z.B. 8)
	 * @param alteGesamtpunkte Gesamtpunkte nach dem vonSpieltag
	 * @param neueGesamtpunkte Gesamtpunkte nach dem bisSpieltag
	 */
	private static void fuelleLueckeMitDifferenz(JSONArray punkteHistorie, int vonSpieltag, int bisSpieltag, int alteGesamtpunkte, int neueGesamtpunkte) {

		int diff = neueGesamtpunkte - alteGesamtpunkte;
		int fehlendeSpieltage = bisSpieltag - vonSpieltag - 1;

		if (fehlendeSpieltage <= 0 || diff <= 0) {
			return; // Nichts zu tun
		}
		// Punkte gleichmäßig verteilen, Rest auf die ersten Spieltage
		int basisPunkte = diff / (fehlendeSpieltage + 1);
		int rest = diff % (fehlendeSpieltage + 1);

		for (int i = 1; i <= fehlendeSpieltage; i++) {
			JSONObject interpolierterEintrag = new JSONObject();
			for (int j = 0; j < punkteHistorie.length(); j++) {
				JSONObject obj = punkteHistorie.getJSONObject(j);
				if (obj.optInt("key") == vonSpieltag + i) {
					// Es gibt schon einen Eintrag für diesen Spieltag, Lücke ist nicht mehr
					// vorhanden, nichts tun
					return;
				}
			}
			interpolierterEintrag.put("key", vonSpieltag + i);
			int value = basisPunkte + (i <= rest ? 1 : 0);
			interpolierterEintrag.put("value", value);
			interpolierterEintrag.put("info", "Interpoliert, weil Datenlücke erkannt");
			interpolierterEintrag.put("totalPoints", alteGesamtpunkte + (basisPunkte * i) + Math.min(i, rest));
			punkteHistorie.put(interpolierterEintrag);
		}
	}

	/**
	 * Behandelt Ausnahmen beim Laden der Spielerdaten.
	 * 
	 * @param e       Die Ausnahme, die behandelt wird.
	 * @param spieler Der Spieler, dessen Daten geladen wurden.
	 * @param db      Die Datenbank, die verwendet wird.
	 */
	private static void handleSpielerDataLoadingException(Exception e) {
		e.printStackTrace();
		LOGGER.warning("Fehler beim Laden der Spielerdaten: " + e.getStackTrace());

	}

}
