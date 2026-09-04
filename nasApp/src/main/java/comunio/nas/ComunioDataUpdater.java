package comunio.nas;

import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.dataScraper.cheats.KontostandBerechner;
import comunio.nas.dataScraper.comAnalystics.ComAnalysticsTopFlop;
import comunio.nas.dataScraper.comstats.ComstatsDataScraper;
import comunio.nas.dataScraper.comunio.ClubUpdater;
import comunio.nas.dataScraper.comunio.LineupParser;
import comunio.nas.dataScraper.comunio.Login;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataScraper.comunio.NewsAnalyzerComunio;
import comunio.nas.dataScraper.comunio.PlayerUpdater;
import comunio.nas.dataScraper.comunio.Transfermarkt;
import comunio.nas.dataScraper.comunio.UserUpdater;
import comunio.nas.dataScraper.ligainsider.LigainsiderRankingUpdater;
import comunio.nas.dataScraper.tools.ExportNotInLiga;
import comunio.nas.dataScraper.tools.PlayerpointsToPlayerObject;
import comunio.nas.dataScraper.tools.SeasonChange;
import comunio.nas.dataScraper.transfermarktDe.TmDePlayerDataUpdater;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.Urls;
import comunio.nas.dataVariable.UserLoginData;
import comunio.nas.error.ErrorsContainer;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.community.Community;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.UpdaterContextData;
import comunio.nas.objects.player.SonstigeAttribute;
import comunio.nas.objects.user.User;
import comunio.nas.util.LoadJSONfromFile;
import comunio.nas.util.StatusManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aktualisiert die Comunio-Spielerdatenbank und die Marktwertdatenbank auf
 * Basis der aktuellen Comunio-API-Daten.
 */
public class ComunioDataUpdater {
	private static final Logger LOGGER = LogManager.getLogger(ComunioDataUpdater.class);

	public static MatchdayInfo currentMatchdayInfo;
	public static Community community = new Community();
	public static UserLoginData uld;
	public static ErrorsContainer errorDb = new ErrorsContainer();

	/**
	 * Hauptmethode: Orchestriert den gesamten Aktualisierungsprozess.
	 */
	public static void main(String[] args) {
		long completeStartTime = System.nanoTime();
		LOGGER.info("Starte ComunioDataUpdater...");

		uld = new UserLoginData(args);
		LastUpdates lastUpdates = new LastUpdates();
		User user = new User();

		Login.login(uld.getUsername(), uld.getPasswortAlsString(), community, user);

		try {
			currentMatchdayInfo = MatchdayInfo.fetchCurrentMatchday();
			logMatchdayInfo();

			// 1. Daten von GitHub / externen Quellen laden
			UpdaterContextData context = loadAllData(lastUpdates, user);

			// 2. Saisonwechsel prüfen & verarbeiten
			boolean seasonChanged = handleSeasonTransit(context, lastUpdates, user);

			// 3. Fachliche Datenverarbeitung
			processData(context, seasonChanged, lastUpdates, user);

			// 4. Ergebnisse zurück auf GitHub hochladen
			uploadAllData(context, lastUpdates);

			long completeEndTime = System.nanoTime();
			logExecutionTime("komplettes Programm", completeEndTime, completeStartTime);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Fehler im Hauptprozess: " + e.getMessage(), e);
		}
	}

	// =========================================================================
	// PRIVATE HILFSMETHODEN (STRUKTURIERUNG)
	// =========================================================================

	private static void logMatchdayInfo() {
		if (currentMatchdayInfo != null) {
			LOGGER.info("Current matchday: " + currentMatchdayInfo.getCurrentMatchday() + ", Finished: " + currentMatchdayInfo.isFinished());
		} else {
			LOGGER.warning("Failed to fetch current matchday info");
		}
	}

	private static UpdaterContextData loadAllData(LastUpdates lastUpdates, User user) throws Exception {
		long start = System.nanoTime();
		UpdaterContextData ctx = new UpdaterContextData();

		LOGGER.info("Lade ErrorDb von GitHub");
		try {
			errorDb.fromJson(LoadJSONfromFile.loadJsonObjectFromUrl(Urls.ERROR_DB_URL));
		} catch (Exception e) {
			LOGGER.warning("Fehler beim Laden der ErrorDb von GitHub: " + e.getMessage());
			errorDb = new ErrorsContainer();
		}

		LOGGER.info("Lade LastUpdates Liste von GitHub");
		JSONObject lastUpdatesList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.LASTUPDATES_LIST_URL);
		lastUpdates.fromJson(lastUpdatesList);

		LOGGER.info("Lade Matchday Liste von GitHub");
		ctx.matchdayInfoList = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.MATCHDAYDATA_LIST_URL);

		LOGGER.info("Lade Vereinsdaten von GitHub");
		ctx.clubDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.CLUB_DB_URL);

		LOGGER.info("Lade Verletzungen von GitHub");
		ctx.injuryDB = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.INJURIES_DB_URL);

		LOGGER.info("Lade Spielerdatenbank von GitHub");
		ctx.playerDBObject = LoadJSONfromFile.loadJsonObjectWithPlayerArrayFromUrl(Urls.PLAYER_DB_URL);

		LOGGER.info("Lade Playerpoints von GitHub");
		ctx.pointsDB = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.POINTS_DB_URL);
		PlayerpointsToPlayerObject.putPointsToPlayerObject(ctx.pointsDB, ctx.playerDBObject);

		LOGGER.info("Lade Marktwertdatenbank von GitHub");
		ctx.marketValueDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.MARKET_VALUE_DB_URL);

		LOGGER.info("Lade Newsdatenbank von GitHub");
		JSONObject newsDbObjcet = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NEWS_DB_URL);
		ctx.newsManager = NewsManager.fromJsonObject(newsDbObjcet);

		LOGGER.info("Lade Userdatenbank von GitHub");
		ctx.userMap = getUserMap();

		LOGGER.info("Lade UserLineup von GitHub");
		JSONObject userLineupJson = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.USER_LINEUPS);
		ctx.lineupParser = new LineupParser();
		ctx.lineupParser.loadMapFromJsonString(userLineupJson);

		LOGGER.info("Lade NotInLiga-PlayerDB von GitHub");
		ctx.notInligaDBObj = LoadJSONfromFile.loadJsonObjectFromUrl(Urls.NOTINLIGA_DB_URL);

		LOGGER.info("Lade TransfermarktListe von GitHub");
		ctx.transfermarktListe = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.TRANSFERMARKT_LIST_URL);

		LOGGER.info("Lade Player to User Map");
		ctx.playerToUserMap = GitHubUploader.downloadPlayerToUserMap(Urls.USER_TO_PLAYER_URL);

		logExecutionTime("Github-Download", System.nanoTime(), start);
		return ctx;
	}

	private static boolean handleSeasonTransit(UpdaterContextData ctx, LastUpdates lastUpdates, User user) {
		boolean seasonChanged = SeasonChange.analyzeNewsForSeasonTransit(ctx.newsManager, ctx.playerDBObject, ctx.marketValueDB, ctx.pointsDB, ctx.matchdayInfoList, ctx.userMap, ctx.transfermarktListe, ctx.playerToUserMap, currentMatchdayInfo, lastUpdates, user, ctx.clubDB);

		if (seasonChanged) {
			ClubUpdater.fetchClubsAsArray(ctx.clubDB);
			LOGGER.info("Saisonwechsel wurde verarbeitet. Fahre direkt mit der Datenverarbeitung der neuen Saison fort...");
		}
		return seasonChanged;
	}

	private static void processData(UpdaterContextData ctx, boolean seasonChanged, LastUpdates lastUpdates, User user) {
		long start = System.nanoTime();

		StatusManager statusManager = new StatusManager(ctx.injuryDB);

		UserUpdater.updateAllUsers(lastUpdates, ctx.playerDBObject, ctx.marketValueDB, ctx.notInligaDBObj, ctx.playerToUserMap, ctx.userMap, community, currentMatchdayInfo, ctx.newsManager, user);
		UserUpdater.updateUserPoints(ctx.userMap, community, currentMatchdayInfo);
		
		KontostandBerechner kontostandBerechner = new KontostandBerechner();
		kontostandBerechner.calculateKontostaende(ctx.userMap, ctx.newsManager);

		// User-Objekt aktualisieren, falls in der Map geändert
		if (ctx.userMap.containsKey(user.getId())) {
			user = ctx.userMap.get(user.getId());
		}
		ctx.matchdayInfoList.put(String.valueOf(currentMatchdayInfo.getCurrentMatchday()), currentMatchdayInfo.toJson());

		Transfermarkt.acceptOrDecline160erOffer(ctx.playerDBObject, user, false, ctx.notInligaDBObj);
		PlayerUpdater.updatePlayers(seasonChanged, ctx.clubDB, ctx.playerDBObject, ctx.marketValueDB, ctx.playerToUserMap, ctx.newsManager, currentMatchdayInfo, ctx.notInligaDBObj, lastUpdates, user, statusManager);

		ctx.lineupParser.fetchLineupForAllUsers(ctx.userMap, currentMatchdayInfo);

		SonstigeAttribute.setSpielerAttributePerformance(ctx.playerDBObject, currentMatchdayInfo, ctx.newsManager);
		Transfermarkt.getTransfermarktListe(ctx.playerDBObject, ctx.transfermarktListe, ctx.notInligaDBObj, lastUpdates, user);
		ComAnalysticsTopFlop.getComAnalysticsTopFlopData(ctx.playerDBObject, lastUpdates);

		ComstatsDataScraper.getPlaytimeForNewMatchdays(currentMatchdayInfo.getPointsMatchday(), ctx.playerDBObject, ctx.notInligaDBObj);

		if (uld.isDebug()) {
			LOGGER.info("DEBUG-Modus: Transfermarkt.de Daten werden NICHT aktualisiert.");
		} else {
			TmDePlayerDataUpdater.updateAllPlayerWithMissedData(ctx.playerDBObject, ctx.clubDB, false, lastUpdates, statusManager);
			TmDePlayerDataUpdater.updateAllPlayerWithLink(ctx.playerDBObject, false, false, lastUpdates, statusManager);
		}

		NewsAnalyzerComunio.analyzeNews(ctx.newsManager, ctx.playerDBObject, ctx.playerToUserMap, ctx.notInligaDBObj, currentMatchdayInfo, lastUpdates, user);
		TmDePlayerDataUpdater.updateVerletzteVonTransfermarkt(ctx.playerDBObject, ctx.clubDB, ctx.newsManager, LOGGER, lastUpdates, statusManager);
		LigainsiderRankingUpdater.updateLigainsiderRanking(ctx.playerDBObject, ctx.clubDB, currentMatchdayInfo, lastUpdates);

		kontostandBerechner.calculateKontostaende(ctx.userMap, ctx.newsManager);

		ExportNotInLiga.exportAndRemoveNotInLiga(ctx.playerDBObject, ctx.notInligaDBObj, lastUpdates, ctx.injuryDB);
		statusManager.mergeAllStatuses(ctx.newsManager, ctx.playerDBObject, ctx.notInligaDBObj);

		// Im Kontext abspeichern für den Upload
		ctx.statusManager = statusManager;

		logExecutionTime("Datenverarbeitung (alles)", System.nanoTime(), start);
	}

	private static void uploadAllData(UpdaterContextData ctx, LastUpdates lastUpdates) {
		long start = System.nanoTime();

		LOGGER.info("Lade aktualisierte Verletzten-Datenbank (injuryDB) auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.INJURIES_DB_URL, ctx.statusManager.getInjuryDB());

		LOGGER.info("Lade aktualisierte Punkte-Datenbank (pointsDB) auf GitHub hoch");
		PlayerpointsToPlayerObject.getPointsArrayFromAllPlayer(ctx.pointsDB, ctx.playerDBObject);
		GitHubUploader.uploadPlayerPoints(ctx.pointsDB);

		LOGGER.info("Lade aktualisierte Spielerdatenbank auf GitHub hoch");
		GitHubUploader.uploadPlayerDatabase(ctx.playerDBObject);

		LOGGER.info("Lade aktualisierte PlayerToUserMap auf GitHub hoch");
		GitHubUploader.uploadPlayerToUserMap(ctx.playerToUserMap);

		LOGGER.info("Lade aktualisierte Marktwertdatenbank auf GitHub hoch");
		GitHubUploader.uploadMarketValueDatabase(ctx.marketValueDB);

		LOGGER.info("Lade aktualisierte Userdatenbank auf GitHub hoch");
		GitHubUploader.uploadUserDatabase(userMapToJSONArray(ctx.userMap));

		LOGGER.info("Lade aktualisierte UserLineups auf GitHub hoch");
		GitHubUploader.uploadUserLineups(ctx.lineupParser.getFinalJSONObject());

		LOGGER.info("Lade aktualisierte TransfermarktListe auf GitHub hoch");
		GitHubUploader.uploadTransfermarktListe(ctx.transfermarktListe);

		LOGGER.info("Lade aktualisierte MatchdayInfo auf GitHub hoch");
		GitHubUploader.uploadMatchdayInfoListe(ctx.matchdayInfoList);

		LOGGER.info("Lade aktualisierte LastUpdates auf GitHub hoch");
		GitHubUploader.uploadLastUpdateListe(lastUpdates.toJson());

		LOGGER.info("Lade aktualisierte NotInLigaPlayerDB auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.NOTINLIGA_DB_URL, ctx.notInligaDBObj);

		LOGGER.info("Lade aktualisierte ClubDB auf GitHub hoch");
		GitHubUploader.uploadClubsDatabase(ctx.clubDB);

		LOGGER.info("Lade aktualisierte ErrorDb.json auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.ERROR_DB_URL, errorDb.toJson());

		LOGGER.info("Lade aktualisierte News auf GitHub hoch");
		JSONObject newsDbObjcet = ctx.newsManager.objectToJson();
		GitHubUploader.uploadNews(newsDbObjcet);

		logExecutionTime("Github-Upload", System.nanoTime(), start);
	}

	private static Map<String, User> getUserMap() {
		Map<String, User> userMap = new HashMap<>();
		try {
			JSONArray userDB = LoadJSONfromFile.loadJsonArrayFromUrl(Urls.USER_DB_URL);
			for (int i = 0; i < userDB.length(); i++) {
				JSONObject userJson = userDB.getJSONObject(i);
				User u = User.fromJson(userJson);
				userMap.put(u.getId(), u);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Laden der Userdatenbank: " + e.getMessage(), e);
		}
		return userMap;
	}

	private static JSONArray userMapToJSONArray(Map<String, User> userMap) {
		JSONArray userArray = new JSONArray();
		for (User user : userMap.values()) {
			userArray.put(user.toJson());
		}
		return userArray;
	}

	private static void logExecutionTime(String taskName, long end, long start) {
		long ms = (end - start) / 1_000_000;
		String msg = "Ladezeit für " + taskName + ": " + ms + " ms";
		LOGGER.info(msg);
		System.out.println(msg);
	}

}