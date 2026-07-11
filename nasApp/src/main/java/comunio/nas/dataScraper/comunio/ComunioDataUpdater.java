package comunio.nas.dataScraper.comunio;

import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.dataScraper.cheats.KontostandBerechner;
import comunio.nas.dataScraper.comAnalystics.ComAnalysticsTopFlop;
import comunio.nas.dataScraper.comstats.ComstatsDataScraper;
import comunio.nas.dataScraper.ligainsider.LigainsiderRankingUpdater;
import comunio.nas.dataScraper.tools.ExportNotInLiga;
import comunio.nas.dataScraper.tools.PlayerpointsToPlayerObject;
import comunio.nas.dataScraper.tools.SeasonChange;
import comunio.nas.dataScraper.transfermarktDe.TmDePlayerDataUpdater;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.dataVariable.UserLoginData;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.community.Community;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.helper.PlayerDbFixer;
import comunio.nas.objects.player.SonstigeAttribute;
import comunio.nas.objects.user.User;
import comunio.nas.util.LoadJSONfromFile;
import comunio.nas.util.StatusManager;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aktualisiert die Comunio-Spielerdatenbank und die Marktwertdatenbank auf
 * Basis der aktuellen Comunio-API-Daten. Die Punktehistorie wird nur bei
 * abgeschlossenem Spieltag (finished=true) aktualisiert. Die Marktwerte werden
 * täglich mit Datum gespeichert.
 *
 * Alle Logs werden in comunio_updater.log geschrieben.
 */
public class ComunioDataUpdater {
	private static final Logger LOGGER = LogManager.getLogger(ComunioDataUpdater.class);

	/** Status des aktuellen Spieltags */
	public static MatchdayInfo currentMatchdayInfo;
	public static Community community = new Community();
	public static UserLoginData uld;

	/**
	 * Hauptmethode: Startet den Aktualisierungsprozess.
	 * 
	 * @param args keine Argumente nötig
	 */
	public static void main(String[] args) {
		long completStartTime = System.nanoTime(); // Startzeit für die Performance-Messung

		LastUpdates lastUpdates = new LastUpdates();
		User user = new User();

		LOGGER.info("Starte ComunioDataUpdater...");

		uld = new UserLoginData(args);

		Login.login(uld.getUsername(), uld.getPasswortAlsString(), community, user);

		try {
			// Spieltag-Status von der API holen
			currentMatchdayInfo = MatchdayInfo.fetchCurrentMatchday();
			if (currentMatchdayInfo != null) {
				LOGGER.info("Current matchday: " + currentMatchdayInfo.getCurrentMatchday() + ", Finished: " + currentMatchdayInfo.isFinished());
			} else {
				LOGGER.warning("Failed to fetch current matchday info");
			}

			// --------------------DATA-DOWNLOAD------------------------------------

			long dataLoaderStartTime = System.nanoTime(); // Startzeit für die Performance-Messung

			LOGGER.info("Lade Vereinsdaten von GitHub");
			JSONArray clubDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.CLUB_DB_URL);

			LOGGER.info("Lade Verletzungen von GitHub");
			JSONObject injuryDB = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.INJURIES_DB_URL);

			LOGGER.info("Lade Spielerdatenbank von GitHub");
			JSONObject playerDBObject = LoadJSONfromFile.loadJsonObjectWithPlayerArrayFromUrl(Urls.PLAYER_DB_URL);
			PlayerDbFixer.removeAllStatusFromPlayerObject(playerDBObject);

			LOGGER.info("Lade Playerpoints von GitHub");
			JSONObject pointsDB = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.POINTS_DB_URL);

			PlayerpointsToPlayerObject.putPointsToPlayerObject(pointsDB, playerDBObject);

			LOGGER.info("Lade Matchday Liste von GitHub");
			JSONObject matchdayInfoList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.MATCHDAYDATA_LIST);

			LOGGER.info("Lade LastUpdates Liste von GitHub");
			JSONObject lastUpdatesList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.LASTUPDATES_LIST);
			lastUpdates.fromJson(lastUpdatesList);

			LOGGER.info("Lade Marktwertdatenbank von GitHub");
			JSONArray marketValueDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.MARKET_VALUE_DB_URL);

			LOGGER.info("Lade Newsdatenbank von GitHub");
			JSONObject newsDbObjcet = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NEWS_DB_URL);
			// NewsManager initialisieren aus JSON
			NewsManager newsManager = NewsManager.fromJsonObject(newsDbObjcet);

			LOGGER.info("Lade Userdatenbank von GitHub");
			JSONArray userDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.USER_DB_URL);

			LOGGER.info("Lade NotInLiga-PlayerDB von GitHub");
			JSONObject notInligaDBObj = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NOTINLIGA_DB_URL);

			LOGGER.info("Lade TransfermarktListe von GitHub");
			JSONArray transfermarktListe = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.TRANSFERMARKT_LIST);

			LOGGER.info("Lade Player to User Map");
			Map<String, String> playerToUserMap = GitHubUploader.downloadPlayerToUserMap(Urls.USER_TO_PLAYER_URL);

			// -------------------- SEASON TRANSIT ANALYSE ---------------------------
			// NEU:
			boolean seasonChanged = SeasonChange.analyzeNewsForSeasonTransit(newsManager, playerDBObject, marketValueDB, pointsDB, matchdayInfoList, userDB, transfermarktListe, playerToUserMap, currentMatchdayInfo, lastUpdates, user, clubDB);

			if (seasonChanged) { 
				ClubUpdater.fetchClubsAsArray(clubDB); // wichtig, dass es am Anfang gemacht wird, da die ClubDB von
														// vielen anderen Funktionen benötigt wird.
				LOGGER.info("Saisonwechsel wurde verarbeitet. Fahre direkt mit der Datenverarbeitung der neuen Saison fort...");
				// Optional: Hier kannst du Variablen nachladen, falls die neue Saison
				// sofort frische API-Daten erfordert, die vorab gefehlt haben.
			}

			long dataLoaderEndTime = System.nanoTime(); // Endzeit für die Messung
			System.out.println("Ladezeit für Github-Download: " + (dataLoaderEndTime - dataLoaderStartTime) / 1_000_000 + " ms");

			// --------------------VERARBEITUNG------------------------------------

			long datenverarbeitungStartTime = System.nanoTime(); // Startzeit für die Performance-Messung

			StatusManager statusManager = new StatusManager(injuryDB);// muss

			UserUpdater.updateAllUsers(lastUpdates, playerDBObject, marketValueDB, notInligaDBObj, playerToUserMap, userDB, community, currentMatchdayInfo, newsManager, user); // wichtig, dass es am Anfang gemacht // wird.

			// kontostände errechnen:
			KontostandBerechner kontostandBerechner = new KontostandBerechner();
			userDB = kontostandBerechner.calculateKontostaende(userDB, newsManager);

			matchdayInfoList.put(String.valueOf(currentMatchdayInfo.getCurrentMatchday()), currentMatchdayInfo.toJson());

			Transfermarkt.acceptOrDecline160erOffer(playerDBObject, user, false, notInligaDBObj);

			PlayerUpdater.updatePlayers(seasonChanged, clubDB, playerDBObject, marketValueDB, playerToUserMap, newsManager, currentMatchdayInfo, notInligaDBObj, lastUpdates, user, statusManager);

			SonstigeAttribute.setSpielerAttributePerformance(playerDBObject, currentMatchdayInfo, newsManager);
			Transfermarkt.getTransfermarktListe(playerDBObject, transfermarktListe, notInligaDBObj, lastUpdates, user);
			ComAnalysticsTopFlop.getComAnalysticsTopFlopData(playerDBObject, lastUpdates);

			ComstatsDataScraper.getPlaytimeForNewMatchdays(currentMatchdayInfo.getPointsMatchday(), playerDBObject, notInligaDBObj);

			// Alle Transfermakrt.de Daten werden aktualisiert. Egal ob schon verhanden oder
			// nicht.
			if (uld.isDebug()) {
				LOGGER.info("DEBUG-Modus: Transfermarkt.de Daten werden NICHT aktualisiert.");
			} else {
				TmDePlayerDataUpdater.updateAllPlayerWithMissedData(playerDBObject, clubDB, false, lastUpdates, statusManager);
				TmDePlayerDataUpdater.updateAllPlayerWithLink(playerDBObject, false, false, lastUpdates, statusManager);
			}

			// news von Comunio werden ausgelesen
			NewsAnalyzerComunio.analyzeNews(newsManager, playerDBObject, playerToUserMap, notInligaDBObj, currentMatchdayInfo, lastUpdates, user);

			// clubsMapping ist dein JSONArray mit allen Vereinen und transfermarktDoDe.name
			TmDePlayerDataUpdater.updateVerletzteVonTransfermarkt(playerDBObject, clubDB, newsManager, LOGGER, lastUpdates, statusManager);
			// Ligainsider Ranking laden:
			LigainsiderRankingUpdater.updateLigainsiderRanking(playerDBObject, clubDB, currentMatchdayInfo, lastUpdates);

			// kontostände errechnen:
			kontostandBerechner = new KontostandBerechner();
			userDB = kontostandBerechner.calculateKontostaende(userDB, newsManager);

			// Aus der normalen SpielerDB werden die NOT_IN_LIGA spieler gelöscht
			ExportNotInLiga.exportAndRemoveNotInLiga(playerDBObject, notInligaDBObj, lastUpdates, injuryDB);

			// Alle Statusänderungen werden in der injuryDB aktualisiert und gleichzeitig in News umgewandelt, wenn es einen Statuswechsel gibt.
			statusManager.mergeAllStatuses(newsManager, playerDBObject, notInligaDBObj);

			long datenverarbeitungEndTime = System.nanoTime(); // Endzeit für die Messung
			System.out.println("Ladezeit für Datenverarbeitung (alles) " + (datenverarbeitungEndTime - datenverarbeitungStartTime) / 1_000_000 + " ms");

			// --------------------DATA-UPLOAD------------------------------------

			long dataUploaderStartTime = System.nanoTime(); // Startzeit für die Performance-Messung

			// Ergebnisse auf GitHub hochladen
			LOGGER.info("Lade aktualisierte Spielerdatenbank auf GitHub hoch");

			LOGGER.info("Lade aktualisierte Verletzten-Datenbank (injuryDB) auf GitHub hoch");
			GitHubUploader.uploadToGitHub(Urls.INJURIES_DB_URL, statusManager.getInjuryDB());

			LOGGER.info("Lade aktualisierte Punkte-Datenbank (pointsDB) auf GitHub hoch");
			PlayerpointsToPlayerObject.getPointsArrayFromAllPlayer(pointsDB, playerDBObject);
			GitHubUploader.uploadPlayerPoints(pointsDB);

			// SPIELTAGSPUNKTE MÜSSEN AUS DEM OBJECT:

			LOGGER.info("Lade aktualisierte Spielerdatenbank auf GitHub hoch");
			GitHubUploader.uploadPlayerDatabase(playerDBObject);

			LOGGER.info("Lade aktualisierte PlayerToUserMap auf GitHub hoch");
			GitHubUploader.uploadPlayerToUserMap(playerToUserMap);

			LOGGER.info("Lade aktualisierte Marktwertdatenbank auf GitHub hoch");
			GitHubUploader.uploadMarketValueDatabase(marketValueDB);

			LOGGER.info("Lade aktualisierte Userdatenbank auf GitHub hoch");
			GitHubUploader.uploadUserDatabase(userDB);

			LOGGER.info("Lade aktualisierte TransfermarktListe auf GitHub hoch");
			GitHubUploader.uploadTransfermarktListe(transfermarktListe);

			LOGGER.info("Lade aktualisierte MatchdayInfo auf GitHub hoch");
			GitHubUploader.uploadMatchdayInfoListe(matchdayInfoList);

			LOGGER.info("Lade aktualisierte LastUpdates auf GitHub hoch");
			GitHubUploader.uploadLastUpdateListe(lastUpdates.toJson());

			LOGGER.info("Lade aktualisierte NotInLigaPlayerDB auf GitHub hoch");
			GitHubUploader.uploadToGitHub(Urls.NOTINLIGA_DB_URL, notInligaDBObj);

			LOGGER.info("Lade aktualisierte ClubDB auf GitHub hoch");
			GitHubUploader.uploadClubsDatabase(clubDB);

			// WICHTIG: News hochladen (von dir ergänzt)
			LOGGER.info("Lade aktualisierte News auf GitHub hoch");
			// Am Ende synchronisieren und uploaden
			newsDbObjcet = newsManager.objectToJson();
			GitHubUploader.uploadNews(newsDbObjcet);

			long dataUploaderEndTime = System.nanoTime(); // Endzeit für die Messung
			System.out.println("Ladezeit für Github-Upload: " + (dataUploaderEndTime - dataUploaderStartTime) / 1_000_000 + " ms");

			// ----------------------ENDE-------------------------------------------

			long completeEndTime = System.nanoTime(); // Endzeit für die Messung
			LOGGER.info("Updater erfolgreich abgeschlossen. \nLadezeit komplettes Programm: " + (completeEndTime - completStartTime) / 1_000_000 + " ms");
			System.out.println("Ladezeit komplettes Programm: " + (completeEndTime - completStartTime) / 1_000_000 + " ms");
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Fehler im Hauptprozess: " + e.getMessage(), e);
		}
	}

}
