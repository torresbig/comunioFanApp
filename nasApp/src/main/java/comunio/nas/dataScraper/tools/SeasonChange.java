package comunio.nas.dataScraper.tools;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import comunio.nas.dataScraper.comunio.Login;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataScraper.comunio.NewsAnalyzerComunio;
import comunio.nas.dataVariable.Dates;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.player.PlayerTools;
import comunio.nas.objects.user.User;

public class SeasonChange {

	private static final Logger LOGGER = LogManager.getLogger(SeasonChange.class);

	/** URLs der Dateien, die beim Saisonwechsel archiviert und geleert werden. */
	private static final List<String> SEASON_ARCHIVE_URLS = List.of(Urls.USER_DB_URL, Urls.POINTS_DB_URL, Urls.MATCHDAYDATA_LIST, Urls.NEWS_DB_URL, Urls.TRANSFERMARKT_LIST, Urls.USER_TO_PLAYER_URL, Urls.CLUB_DB_URL);
//	private static final List<String> SEASON_ARCHIVE_URLS = List.of(Urls.USER_TO_PLAYER_URL);

	/**
	 * Archiviert alle saisonrelevanten Dateien für das angegebene Jahr und leert
	 * die Originaldateien für die neue Saison.
	 *
	 * <p>
	 * Ablauf für jede Datei in {@link #SEASON_ARCHIVE_URLS}:
	 * </p>
	 * <ol>
	 * <li>Originalinhalt von GitHub herunterladen.</li>
	 * <li>Inhalt unter {@code <originalPfadOrdner>/<year>/<dateiname>}
	 * archivieren.</li>
	 * <li>Originaldatei mit leerem Inhalt ({@code {}} oder {@code []})
	 * überschreiben.</li>
	 * </ol>
	 *
	 * @param year Jahr, für das die Daten archiviert werden (z. B. 2025).
	 * @throws Exception bei Netzwerk- oder Upload-Fehlern.
	 */
	public static void archiveSeasonData(int year) throws Exception {
		LOGGER.info("Starte Saisonarchivierung für Jahr " + year + " ...");

		HttpClient client = HttpClient.newHttpClient();

		for (String urlString : SEASON_ARCHIVE_URLS) {
			String originalPath = Urls.getFilePathForGit(urlString);
			String filename = Urls.getFilename(urlString);

			// Zielpfad für das Archiv: z. B. "data/2025/PointsDB.json"
			String[] pathSegments = originalPath.split("/");
			StringBuilder archivePathBuilder = new StringBuilder();
			for (int i = 0; i < pathSegments.length - 1; i++) {
				archivePathBuilder.append(pathSegments[i]).append("/");
			}
			archivePathBuilder.append(year).append("/").append(filename);
			String archivePath = archivePathBuilder.toString();

			// Aktuellen Inhalt von GitHub laden mit Cache-Buster
			String urlWithCacheBuster = urlString + (urlString.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlWithCacheBuster)).header("User-Agent", "Mozilla/5.0").GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			String content;
			if (response.statusCode() == 404) {
				LOGGER.warning("Datei nicht gefunden (404): " + originalPath + " – überspringe Archivierung.");
				continue;
			} else if (response.statusCode() != 200) {
				LOGGER.warning("Unerwarteter Statuscode " + response.statusCode() + " für " + originalPath + " – überspringe.");
				continue;
			}
			content = response.body();

			// 1) Inhalt ins Jahresarchiv hochladen
			GitHubUploader.uploadToGitHub(archivePath, content, "Archiviert für Saison " + year);
			LOGGER.info("Archiviert: " + originalPath + " -> " + archivePath);

//			// 2) Originaldatei leeren
//			String emptyContent = content.trim().startsWith("[") ? "[]" : "{}";
//			GitHubUploader.uploadToGitHub(originalPath, emptyContent, "Geleert für neue Saison (Archiv in " + year + ")");
//			LOGGER.info("Geleert: " + originalPath);
		}

		LOGGER.info("Saisonarchivierung für Jahr " + year + " abgeschlossen.");
	}

	/**
	 * Convenience-Methode: Archiviert mit dem aktuellen Systemjahr.
	 *
	 * @throws Exception bei Netzwerk- oder Upload-Fehlern.
	 * @see #archiveSeasonData(int)
	 */
	public static void archiveCurrentSeasonData() throws Exception {
		archiveSeasonData(Year.now().minusYears(1).getValue());

	}

	
	/**
	 * Analysiert die Comunio-System-News auf das Ereignis "Saisonübergang abgeschlossen".
	 * Wenn dieser Eintrag in den aktuellen News gefunden wird und der Übergang noch nicht
	 * verarbeitet wurde, wird der In-Memory-Saisonwechsel angestoßen.
	 * * <p><strong>Ablauf des Saisonwechsels:</strong></p>
	 * <ol>
	 * <li>Sicherung der aktuellen Tabellenstände im historischen Jahresarchiv auf GitHub.</li>
	 * <li>Berechnung und Setzen des neuen offiziellen Saisonstart-Datums (Erkennungsdatum + 1 Tag).</li>
	 * <li>Aktualisierung des {@code seasonTransition}-Zeitstempels in {@link LastUpdates}.</li>
	 * <li>Zurücksetzen aller relevanten Datums-Zeitstempel in {@code lastUpdates} auf {@link Instant#EPOCH}, 
	 * damit nachfolgende Scraper-Prozesse gezwungen werden, die API-Daten vollständig neu einzulesen.</li>
	 * <li>Bereinigung spielerspezifischer Attribute der bestehenden Spielerdatenbank (Punkte, Statistiken, etc.).</li>
	 * <li>Vollständiges Leeren (In-Memory Reset) aller saisonspezifischen Kollektionen und JSON-Strukturen 
	 * (Marktwerte, Punktehistorie, Matchdays, Userdaten, Transfermarktlisten, Player-to-User-Mappings).</li>
	 * <li>Synchroner Upload der nun geleerten, initialen Datenstrukturen auf GitHub, um das Repository für die neue Saison vorzubereiten.</li>
	 * </ol>
	 *
	 * <p><strong>Wichtig:</strong> Diese Methode sollte idealerweise regelmäßig beim Start des Datenaktualisierungsprozesses aufgerufen werden, um einen nahtlosen Übergang zwischen den Saisons zu gewährleisten. Sie ist so konzipiert, dass sie nur einmal pro Saisonübergang eine Aktion auslöst, basierend auf dem Vergleich des letzten erkannten Saisonübergangs mit dem aktuellen Spieltag.</p>
	 * @param user                Der aktuell angemeldete User, um die News-URL korrekt zu generieren.
	 * @param newsManager         Verwaltungsobjekt für die News-Datenbank zur Konvertierung und Abfrage.
	 * @param playerDBObject      Das JSON-Objekt der Spielerdatenbank (enthält das "playerDB"-Array).
	 * @param marketValueDB       Das JSON-Array der Marktwertdatenbank (wird geleert).
	 * @param pointsDB            Das JSON-Objekt der Punktedatenbank (wird geleert).
	 * @param matchdayInfoList    Das JSON-Objekt der Spieltagsdaten-Liste (wird geleert).
	 * @param userDB              Das JSON-Array der Userdatenbank (wird geleert).
	 * @param transfermarktListe  Das JSON-Array der Transfermarktliste (wird geleert).
	 * @param playerToUserMap     Die Map für die Zuordnung von Spielern zu Usern (wird geleert).
	 * @param matchdayInfo        Die aktuellen Spieltagsinformationen zur Überprüfung des Saisonstatus.
	 * @param lastUpdates         Die Instanz zur Verwaltung der letzten Update-Zeitstempel.
	 * @return {@code true}, wenn ein Saisonübergang erkannt und erfolgreich im RAM sowie auf GitHub verarbeitet wurde;
	 * {@code false}, wenn kein Saisonübergang vorlag oder der Prozess abgebrochen wurde.
	 */
	public static boolean analyzeNewsForSeasonTransit(
			NewsManager newsManager, 
			JSONObject playerDBObject, 
			JSONArray marketValueDB,
			JSONObject pointsDB,
			JSONObject matchdayInfoList,
			JSONArray userDB,
			JSONArray transfermarktListe,
			Map<String, String> playerToUserMap,
			MatchdayInfo matchdayInfo, 
			LastUpdates lastUpdates,
			User user,
			JSONArray clubDB
	) {

		lastUpdates = lastUpdates != null ? lastUpdates : new LastUpdates();
		
		if (matchdayInfo == null) {
			LOGGER.warning("MatchdayInfo ist null. Saisonübergangserkennung könnte ungenau sein.");
			return false;
		}

		// Nur prüfen, wenn Saisonende erkannt und noch kein Übergang für diesen Zeitraum markiert wurde
		if (matchdayInfo.isSeasonOver(lastUpdates)) {
			try {
				LOGGER.info("Saisonende erkannt. Aktueller Spieltag: " + matchdayInfo.getCurrentMatchday() + ". Warte auf Saisonübergang in den News...");
				
				ComunioDate lastTransferNewsDate = NewsAnalyzerComunio.getLastTransferNewsDateFromGitHub(newsManager.dbToJson());

				int page = 0;
				int limit = 20;
				boolean hasMore = true;
				boolean stopBecauseOfOldDate = false;

				LOGGER.info("Starte News-Abfrage ab (einschließlich): " + lastTransferNewsDate);

				// Seitenweise News abrufen
				while (hasMore && !stopBecauseOfOldDate) {
					String url = Urls.COM_NEWS(user) + "?group=true&originaltypes=true&start=" + page + "&limit=" + limit;
					JSONObject newsRoot;

					try {
						Connection connection = Jsoup.connect(url)
								.header("Accept", "application/json, text/plain, */*")
								.header("Authorization", "Bearer " + Login.getToken())
								.ignoreContentType(true);
						Document doc = connection.get();
						newsRoot = doc != null ? new JSONObject(doc.body().text()) : null;
					} catch (Exception e) {
						LOGGER.warning("Fehler beim Laden der News: " + e.getMessage());
						break;
					}

					if (newsRoot == null || !newsRoot.has("newsList")) {
						LOGGER.info("Keine weiteren News gefunden.");
						break;
					}

					JSONObject newsList = newsRoot.getJSONObject("newsList");
					JSONObject groups = newsList.optJSONObject("groups");
					if (groups == null) {
						LOGGER.info("Keine News-Gruppen gefunden.");
						break;
					}

					boolean foundOldDate = false;

					// Jede Datum-Gruppe verarbeiten
					for (String dateKey : groups.keySet()) {
						ComunioDate newsDate = new ComunioDate(dateKey);
						if (newsDate.before(lastTransferNewsDate)) {
							LOGGER.info("Zu altes Datum (" + newsDate + ") gefunden. Stoppe nach dieser Seite.");
							foundOldDate = true;
							continue;
						}

						JSONArray entries = groups.getJSONObject(dateKey).getJSONArray("entries");

						for (int i = 0; i < entries.length(); i++) {
							JSONObject entry = entries.getJSONObject(i);
							String type = entry.optString("type");
							String title = entry.optString("title", "");

							if ("SYSTEM_ADMINISTRATION".equals(type)) {
								if (title != null && title.contains("Saisonübergang abgeschlossen")) {

									LOGGER.info("Saisonübergang am " + newsDate + " erkannt. Starte Archivierungsprozess...");

									// 1. BACKUP: Aktuellen Stand der Saisonarchivierung im Jahresordner sichern
									try {
										SeasonChange.archiveCurrentSeasonData();
									} catch (Exception e) {
										LOGGER.log(Level.SEVERE, "Kritischer Fehler bei der Saison-Archivierung! Abbruch aus Sicherheitsgründen.", e);
										return false;
									}

									// 2. LOGIK: Neues Saisonstart-Datum setzen (1 Tag nach dem Übergang)
									ComunioDate newSeasonStart = new ComunioDate(newsDate).addDays(1);
									Dates.setSeasonStart(newSeasonStart);

									// 3. MARKIERUNG & RESET: LastUpdates-Zeitstempel konfigurieren
									lastUpdates.setSeasonStart(newSeasonStart.toInstant());
									
									// Alle Zeitstempel auf EPOCH setzen, damit Daten im Nachgang zwingend frisch von den APIs geholt werden
									lastUpdates.setPlayerDbFull(Instant.EPOCH);
									lastUpdates.setPlayerDbShort(Instant.EPOCH);
									lastUpdates.setMatchdayInfo(Instant.EPOCH);
									lastUpdates.setNews(Instant.EPOCH);
									lastUpdates.setUsers(Instant.EPOCH);
									lastUpdates.setClubDb(Instant.EPOCH);
									LOGGER.info("Alle LastUpdate-Zeitstempel zurückgesetzt, damit Daten beim Weiterlaufen neu geladen werden.");

									// 4. IN-MEMORY BEREINIGUNG: Datenstrukturen im RAM leeren
									// Spieler-Attribute für die neue Saison bereinigen
									PlayerTools.deletePlayerDbAttributeForNewSeason(playerDBObject);
									
									// Map leeren
									playerToUserMap.clear();
									
									// JSONArrays im RAM elementweise leeren
									
									while (userDB.length() > 0) userDB.remove(0);
									while (transfermarktListe.length() > 0) transfermarktListe.remove(0);
									while (clubDB.length() > 0) clubDB.remove(0);
									newsManager.clear(); // NewsManager intern leeren (alle News entfernen)
									
									// JSONObjects im RAM durch Entfernen aller Keys leeren
									for (String key : List.of(pointsDB.keySet().toArray(new String[0]))) pointsDB.remove(key);
									for (String key : List.of(matchdayInfoList.keySet().toArray(new String[0]))) matchdayInfoList.remove(key);

									LOGGER.info("Lokale Datenbank-Objekte im Arbeitsspeicher erfolgreich für die neue Saison zurückgesetzt.");

									// 5. SYNCHRONISATION: Initiale, leere Saisondaten auf GitHub pushen
									try {
										LOGGER.info("Lade initiale (leere) Tabellenstrukturen auf GitHub hoch...");
//										GitHubUploader.uploadLastUpdateListe(lastUpdates.toJson());
//										GitHubUploader.uploadPlayerDatabase(playerDBObject);
//										GitHubUploader.uploadPlayerToUserMap(playerToUserMap);
//										GitHubUploader.uploadMarketValueDatabase(marketValueDB);
//										GitHubUploader.uploadUserDatabase(userDB);
//										GitHubUploader.uploadTransfermarktListe(transfermarktListe);
//										GitHubUploader.uploadMatchdayInfoListe(matchdayInfoList);
//										GitHubUploader.uploadClubsDatabase(clubDB);
										LOGGER.info("Initiale Saisondaten erfolgreich auf GitHub synchronisiert.");
									} catch (Exception e) {
										LOGGER.log(Level.SEVERE, "Fehler beim Upload der leeren Initialdaten auf GitHub. Prozess läuft mit RAM-Daten weiter.", e);
									}

									// Signalisiere erfolgreiche Verarbeitung an den ComunioDataUpdater
									return true;
								}
							}
						}
					}

					if (foundOldDate) {
						stopBecauseOfOldDate = true;
					}
					hasMore = newsList.optBoolean("hasMore", false);
					page++;
				}
			} catch (Exception e) {
				LOGGER.severe("Fehler in analyzeNewsForSeasonTransit: " + e.getMessage());
			}
		}
		
		return false;
	}

}
