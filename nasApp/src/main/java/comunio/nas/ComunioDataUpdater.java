package comunio.nas;

import comunio.nas.cheats.KontostandBerechner;
import comunio.nas.dataScraper.comAnalystics.ComAnalysticsTopFlop;
import comunio.nas.dataScraper.comstats.ComstatsDataScraper;
import comunio.nas.dataScraper.comunio.ClubUpdater;
import comunio.nas.dataScraper.comunio.Login;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataScraper.comunio.NewsAnalyzerComunio;
import comunio.nas.dataScraper.comunio.PlayerUpdater;
import comunio.nas.dataScraper.comunio.Transfermarkt;
import comunio.nas.dataScraper.comunio.UserUpdater;
import comunio.nas.dataScraper.espn.EspnClubUpdater;
import comunio.nas.dataScraper.espn.EspnPlayerUpdater;
import comunio.nas.dataScraper.ligainsider.LigainsiderClubUpdater;
import comunio.nas.dataScraper.ligainsider.LigainsiderRankingUpdater;
import comunio.nas.dataScraper.tools.ExportNotInLiga;
import comunio.nas.dataScraper.tools.SeasonChange;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.dataVariable.UserLoginData;
import comunio.nas.error.ErrorsContainer;
import comunio.nas.objects.News;
import comunio.nas.objects.community.Community;
import comunio.nas.objects.helper.JsonHelper;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.helper.PlayerDbFixer;
import comunio.nas.objects.orga.UpdaterContextData;
import comunio.nas.objects.player.SonstigeAttribute;
import comunio.nas.objects.user.User;

import java.util.ArrayList;
import java.util.List;
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
	public static User user = new User();

	/**
	 * Hauptmethode: Orchestriert den gesamten Aktualisierungsprozess.
	 */
	public static void main(String[] args) {
		long completeStartTime = System.nanoTime();
		LOGGER.info("Starte ComunioDataUpdater...");

		uld = new UserLoginData(args);
		LastUpdates lastUpdates = new LastUpdates();

		Login.login(uld.getUsername(), uld.getPasswortAlsString(), community, user);

		try {
			currentMatchdayInfo = MatchdayInfo.fetchCurrentMatchday();
			logMatchdayInfo();

			// 1. Daten von GitHub / externen Quellen laden
			long start = System.nanoTime();
			UpdaterContextData context = new UpdaterContextData().loadAllData(lastUpdates, user);
			logExecutionTime("Github-Download", System.nanoTime(), start);

			// 2. Fixing data if nessassary
			boolean startFix = false; 
			startFixing(context, lastUpdates, startFix, true);

			// 3. Saisonwechsel prüfen & verarbeiten
			boolean seasonChanged = handleSeasonTransit(context, lastUpdates, user);

			// 4. Fachliche Datenverarbeitung
			processData(context, seasonChanged, lastUpdates, user);

			// 5. Ergebnisse zurück auf GitHub hochladen
			long startUpload = System.nanoTime();
			context.uploadAllData(context, lastUpdates);
			logExecutionTime("Github-Upload", System.nanoTime(), startUpload);

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

	private static void processData(UpdaterContextData ctx, boolean seasonChanged, LastUpdates lastUpdates, User user) {
		long start = System.nanoTime();

		UserUpdater.updateAllUsers(lastUpdates, ctx.getPlayerDBObject(), ctx.getMarketValueDB(), ctx.getNotInligaDBObj(), ctx.getPlayerToUserMap(), ctx.getUserMap(), community, currentMatchdayInfo, ctx.getNewsManager(), user);
		UserUpdater.updateUserPoints(ctx.getUserMap(), community, currentMatchdayInfo);
		ctx.setOwnerList(ctx.getUserMap().keySet());

		KontostandBerechner kontostandBerechner = new KontostandBerechner();
		kontostandBerechner.calculateKontostaende(ctx.getUserMap(), ctx.getNewsManager());

		// User-Objekt aktualisieren, falls in der Map geändert
		if (ctx.getUserMap().containsKey(user.getId())) {
			user = ctx.getUserMap().get(user.getId());
		}
		ctx.getMatchdayInfoList().put(String.valueOf(currentMatchdayInfo.getCurrentMatchday()), currentMatchdayInfo.toJson());

		Transfermarkt.acceptOrDecline160erOffer(ctx.getPlayerDBObject(), user, false, ctx.getNotInligaDBObj());
		PlayerUpdater.updatePlayers(seasonChanged, ctx.getClubDbContainer().toJSON(), ctx.getPlayerDBObject(), ctx.getMarketValueDB(), ctx.getPlayerToUserMap(), ctx.getNewsManager(), currentMatchdayInfo, ctx.getNotInligaDBObj(), lastUpdates, user);

		ctx.getLineupParser().fetchLineupForAllUsers(ctx.getUserMap(), currentMatchdayInfo);

		SonstigeAttribute.setSpielerAttributePerformance(ctx.getPlayerDBObject(), currentMatchdayInfo, ctx.getNewsManager());
		Transfermarkt.getTransfermarktListe(ctx.getPlayerDBObject(), ctx.getComunioTransfermarktContainer(), ctx.getNotInligaDBObj(), lastUpdates, user);
		ComAnalysticsTopFlop.getComAnalysticsTopFlopData(ctx.getPlayerDBObject(), lastUpdates);

//		ComstatsDataScraper.getPlaytimeForInputToInput(1, 3, ctx.getPlayerDBObject(), ctx.getNotInligaDBObj(), lastUpdates);
		ComstatsDataScraper.getPlaytimeForNewMatchdays(currentMatchdayInfo.getPointsMatchday(), ctx.getPlayerDBObject(), ctx.getNotInligaDBObj(), lastUpdates);
		EspnPlayerUpdater.updatePlayers(ctx.getPlayerDBObject(), ctx.getClubDbContainer().toJSON(), ctx.getEspnClubMappingContainer().getClubMap(), ctx.getEspnPlayerMappingContainer(), lastUpdates, currentMatchdayInfo);

		NewsAnalyzerComunio.analyzeNews(ctx.getNewsManager(), ctx.getPlayerDBObject(), ctx.getPlayerToUserMap(), ctx.getNotInligaDBObj(), currentMatchdayInfo, lastUpdates, user);
//		TmDePlayerDataUpdater.updateVerletzteVonTransfermarkt(ctx.getPlayerDBObject(), ctx.clubDB, ctx.getNewsManager(), LOGGER, lastUpdates, statusManager);
		LigainsiderRankingUpdater.updateLigainsiderRanking(ctx.getPlayerDBObject(), ctx.getClubDbContainer().getClubDb(), currentMatchdayInfo, lastUpdates);
		ctx.getInjuryDB().updateAllInjuredBannedPlayer(ctx.getPlayerDBArray(), ctx.getClubDbMap(), currentMatchdayInfo, ctx.getNewsManager());
		ctx.getPossibleFormationMap().updatePossibleFormation(ctx.getClubDbContainer().getClubDb(), ctx.getPlayerDBObject(), currentMatchdayInfo);

		kontostandBerechner.calculateKontostaende(ctx.getUserMap(), ctx.getNewsManager());

		PlayerUpdater.updateAllFromNotInLigaDb(ctx.getPlayerDBObject(), ctx.getMarketValueDB(), ctx.getPlayerToUserMap(), ctx.getNewsManager(), lastUpdates, user);

		ExportNotInLiga.exportAndRemoveNotInLiga(ctx.getPlayerDBObject(), ctx.getNotInligaDBObj(), lastUpdates, JsonHelper.mapToJSONObject(ctx.getInjuryDB().getInjuriedAndBannedPlayer()));

		logExecutionTime("Datenverarbeitung (alles)", System.nanoTime(), start);
	}

	private static void logMatchdayInfo() {
		if (currentMatchdayInfo != null) {
			LOGGER.info("Current matchday: " + currentMatchdayInfo.getCurrentMatchday() + ", Finished: " + currentMatchdayInfo.isFinished());
		} else {
			LOGGER.warning("Failed to fetch current matchday info");
		}
	}

	private static boolean handleSeasonTransit(UpdaterContextData ctx, LastUpdates lastUpdates, User user) {
		boolean seasonChanged = SeasonChange.analyzeNewsForSeasonTransit(ctx.getNewsManager(), ctx.getPlayerDBObject(), ctx.getMarketValueDB(), ctx.getPointsDB(), ctx.getMatchdayInfoList(), ctx.getUserMap(), ctx.getComunioTransfermarktContainer().getTransfermarktMap(), ctx.getPlayerToUserMap(), currentMatchdayInfo, lastUpdates, user, ctx.getClubDbContainer().toJSON());

		if (seasonChanged) {
			ClubUpdater.fetchClubsAsArray(ctx.getClubDbContainer().toJSON());
			ctx.getEspnClubMappingContainer().setClubMap(EspnClubUpdater.updateEspnToComunioClubMap(ctx.getClubDbContainer().toJSON()));
			LigainsiderClubUpdater.updateLigainsiderClubDate(ctx.getClubDbContainer().toJSON());
			LOGGER.info("Saisonwechsel wurde verarbeitet. Fahre direkt mit der Datenverarbeitung der neuen Saison fort...");
		}
		return seasonChanged;
	}

	private static void logExecutionTime(String taskName, long end, long start) {
		long ms = (end - start) / 1_000_000;
		String msg = "Ladezeit für " + taskName + ": " + ms + " ms";
		LOGGER.info(msg);
		System.out.println(msg);
	}

	
	/**
	 * Führt den "Fixing"-Prozess für die Spielerdatenbank durch.
	 * <p>
	 * Wenn {@code ausfuhren} {@code true} ist:
	 * <ul>
	 *   <li>Wird {@link PlayerDbFixer#removeFromPlayerToUserMapIfNotInLiga} ausgeführt,
	 *       um nicht mehr in der Liga befindliche Spieler aus der Benutzer-Spiegelung
	 *       (playerToUserMap) zu entfernen.</li>
	 *   <li>Alle geänderten Daten werden anschließend über
	 *       {@link UpdaterContextData#uploadAllData} hochgeladen.</li>
	 *   <li>Steht {@code onlyFix} ebenfalls auf {@code true}, wird das Programm nach
	 *       Abschluss mit {@link System#exit} mit Status {@code 0} (Erfolg)
	 *       beendet. Dies ist nützlich für modusspezifische Durchläufe,
	 *       bei denen nach dem Fixing keine weitere Logik ausgeführt werden soll.</li>
	 * </ul>
	 * <p>
	 * Wenn {@code ausfuhren} {@code false} ist, passiert nichts.
	 *
	 * @param context     Der Kontext mit allen relevanten Datenbanken und Maps.
	 * @param lastUpdates Die Zeichenfolgen der letzten Aktualisierungen.
	 * @param ausfuhren   Gibt an, ob der Fixing-Prozess überhaupt gestartet werden soll.
	 * @param onlyFix     Wenn {@code true}, wird nach Abschluss das Programm beendet.
	 */
	public static void startFixing(UpdaterContextData context, LastUpdates lastUpdates, boolean ausfuhren, boolean onlyFix) {
		if (ausfuhren) {
			List<News> result = new ArrayList<>(); 
			result = PlayerDbFixer.findAndInsertMissingTransfers("2026-09-01", "2026-09-25", context.getNewsManager(), user, currentMatchdayInfo, lastUpdates, context.getPlayerDBObject(), context.getPlayerToUserMap(), context.getNotInligaDBObj(), context.getUserMap());
			KontostandBerechner kontostandBerechner = new KontostandBerechner();
			kontostandBerechner.calculateKontostaende(context.getUserMap(), context.getNewsManager());
			context.uploadAllData(context, lastUpdates);
			if (onlyFix) {
				System.exit(0);
			}
		}
	}

}