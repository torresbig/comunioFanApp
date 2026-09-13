package comunio.nas.dataScraper.espn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.espn.EspnClubMapObject;
import comunio.nas.objects.espn.EspnPlayerContainer;
import comunio.nas.objects.espn.EspnPlayerMapObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.player.Spielerstats;

/**
 * Orchestriert das ESPN-Update für die Spieler-Datenbank.
 * <p>
 * Läuft NACH JEDEM NEUEN SPIELTAG (gesteuert über
 * {@link LastUpdates#getLastEspnMatchday()}) und aktualisiert:
 * <ul>
 * <li><b>Spielerinfos</b> (Profil-Daten) → in die Spieler-DB
 * ({@link EspnPlayerDataIntegrator})</li>
 * <li><b>Spielerstats</b> (Saison-Statistiken) → in die Spieler-DB
 * ({@link EspnStatsIntegrator})</li>
 * <li><b>Vereins-Mapping</b> → als separate Datei auf GitHub (Option A)</li>
 * </ul>
 * Transfermarkt.de bleibt unverändert bestehen – ESPN läuft zusätzlich.
 */
public class EspnPlayerUpdater {

	private static final Logger LOGGER = LogManager.getLogger(EspnPlayerUpdater.class);

	/** Konstruktor privat – statische Nutzung. */
	private EspnPlayerUpdater() {
	}

	/**
	 * Führt das ESPN-Update aus, wenn ein neuer Spieltag vorliegt.
	 *
	 * @param playerDBObject      Die Spieler-DB (mit "playerDB"-Array)
	 * @param clubDB              Die Vereins-DB (für das Mapping)
	 * @param lastUpdates         Für den Spieltag-Trigger
	 * @param currentMatchdayInfo Der aktuelle Spieltag
	 */
	public static void updatePlayers(JSONObject playerDBObject, JSONArray clubDB, Map<String, EspnClubMapObject> espnToComunioClubMap, EspnPlayerContainer playerContainer, LastUpdates lastUpdates, MatchdayInfo currentMatchdayInfo) {
		if (playerDBObject == null || lastUpdates == null || currentMatchdayInfo == null) {
			LOGGER.warning("ESPN-Update übersprungen: Parameter null!");
			return;
		}

		int currentPointsMatchday = currentMatchdayInfo.getPointsMatchday();
		int lastEspnMatchday = lastUpdates.getLastEspnMatchday();

		// Trigger: Nur bei neuem Spieltag (oder erstem Lauf)
		if (lastEspnMatchday == currentPointsMatchday) {
			LOGGER.info("ESPN: Kein neuer Spieltag (" + currentPointsMatchday + "), Update übersprungen.");
			return;
		}

		LOGGER.info("ESPN-Update gestartet für Spieltag " + currentPointsMatchday + " (letzter: " + lastEspnMatchday + ")...");

		if (espnToComunioClubMap == null || espnToComunioClubMap.isEmpty()) {
			LOGGER.warning("ESPN: Kein Vereins-Mapping vorhanden, Update wird ausgeführt");
			espnToComunioClubMap = EspnClubUpdater.updateEspnToComunioClubMap(clubDB);
			if (espnToComunioClubMap == null || espnToComunioClubMap.isEmpty()) {
				LOGGER.warning("ESPN: Vereins-Mapping konnte nicht erstellt werden, Update abgebrochen.");
				return;
			}
			LOGGER.info("ESPN: Vereins-Mapping aktualisiert: " + espnToComunioClubMap.size() + " Vereine.");
		}

		try {
			// 1. Alle 18 Rosters laden (inkl. Rohdaten für Stats)
			JSONObject espnData = EspnRosterCollector.collectAllRosters(espnToComunioClubMap, clubDB);
			JSONArray espnPlayers = espnData.optJSONArray("players");
			if (espnPlayers == null || espnPlayers.length() == 0) {
				LOGGER.warning("ESPN: Keine Spieler geladen, Update abgebrochen.");
				return;
			}

			playerContainer = EspnPlayerMapper.buildPlayerMapping(playerDBObject, espnPlayers, playerContainer, espnToComunioClubMap);

			// 5. Spielerinfos + Spielerstats in die DB integrieren
			int updated = integrateAllPlayers(playerDBObject, espnPlayers, espnToComunioClubMap, playerContainer);
			LOGGER.info("ESPN: " + updated + " Spieler aktualisiert (Infos + Stats).");

			// 6. Spieltag merken
			lastUpdates.setLastEspnMatchday(currentPointsMatchday);
			LOGGER.info("ESPN-Update abgeschlossen für Spieltag " + currentPointsMatchday + ".");

		} catch (Exception e) {
			LOGGER.warning("ESPN-Update fehlgeschlagen: " + e.getMessage());
		}
	}

	/**
	 * Integriert für jeden ESPN-Spieler die Profil-Daten und die Saison-Stats in
	 * den passenden DB-Spieler (Verein + Name müssen übereinstimmen).
	 *
	 * @return Anzahl der aktualisierten Spieler
	 */
	private static int integrateAllPlayers(JSONObject playerDBObject, JSONArray espnPlayers, Map<String, EspnClubMapObject> espnToComunioClubMap, EspnPlayerContainer playerContainer) {
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
		if (playerDB == null) {
			return 0;
		}

		int updated = 0;
		int faildWrongClub = 0;
		int failedNoData = 0;
		int failedNoStats = 0;
		int failedIdMismatch = 0;

		List<String> onlyErrorPlayers = new ArrayList<>();

		for (int i = 0; i < espnPlayers.length(); i++) {
			JSONObject espnPlayer = espnPlayers.getJSONObject(i);
			String espnClubId = espnPlayer.optJSONObject("club").optString("espnId", "");
			String comunioClubId = espnToComunioClubMap != null && espnToComunioClubMap.containsKey(espnClubId) ? espnToComunioClubMap.get(espnClubId).getComunioId() : null;
			if (comunioClubId == null || comunioClubId.isEmpty()) {
				continue; // Verein nicht gemappt
			}

			String espnName = espnPlayer.optString("name", "");
			boolean match = false;

			// DB-Spieler suchen (gleicher Verein + Namensübereinstimmung)
			for (int j = 0; j < playerDB.length(); j++) {
				JSONObject dbPlayer = playerDB.optJSONObject(j);
				if (dbPlayer == null) {
					failedNoData++;
					continue;
				}
				JSONObject data = dbPlayer.optJSONObject("data");
				if (data == null) {
					failedNoData++;
					LOGGER.warning("ESPN-Update: Spieler " + espnName + " übersprungen, da kein 'data'-Objekt in der DB vorhanden ist.");
					continue;
				}
				String dbClubId = data.optString("verein", "");
				if (!comunioClubId.equals(dbClubId)) {
					continue;
				}

				if (playerContainer != null && playerContainer.getPlayerMap() != null && playerContainer.getPlayerMap().isEmpty()) {
					EspnPlayerMapObject playerObject= playerContainer.getPlayerMap().get(espnPlayer.optString("espnId", ""));
					String comunioPlayerId = playerObject.getComunioId();
					if (comunioPlayerId != null && !comunioPlayerId.isEmpty() && !comunioPlayerId.equals(dbPlayer.optString("id", ""))) {
						continue; // Spieler-ID stimmt nicht überein
					} else {
						match = true;
					}
				}

				// Spieler gefunden → Profil-Daten befüllen (nur leere Felder)
				boolean changed = EspnPlayerDataIntegrator.integratePlayerData(playerDBObject, buildSinglePlayerJson(espnPlayer), espnToComunioClubMap) > 0;

				// Saison-Stats in die DB schreiben (data.stats)
				//TODO: STATS KOMMEN WOHL WOANDERS HER! 
				JSONObject statsJson = extractStatsFromPlayer(espnPlayer);
				if (statsJson != null && statsJson.length() > 0) {
					Spielerstats stats = Spielerstats.fromJSON(data.optJSONObject("stats"));
					applyStats(stats, statsJson);
					data.put("stats", stats.toJSON());
					changed = true;
				} else {
					failedNoStats++;
				}

				if (changed) {
					updated++;
				}
				break;
			}
			if (!match) {
				failedIdMismatch++;
				onlyErrorPlayers.add(espnName + " (ESPN-ID: " + espnPlayer.optString("espnId", "") + ", ESPN-Verein: " + espnClubId + ")");
				LOGGER.warning("ESPN-Update: Spieler " + espnName + " übersprungen, da kein passender DB-Spieler gefunden wurde (Verein: " + comunioClubId + ").");
			}
		}
		LOGGER.info("ESPN-Update: Spieler aktualisiert: " + updated + ", übersprungen (falscher Verein): " + faildWrongClub + ", übersprungen (kein 'data'-Objekt): " + failedNoData + ", übersprungen (keine Stats): " + failedNoStats + ", übersprungen (ID-Mismatch): " + failedIdMismatch);
		if (!onlyErrorPlayers.isEmpty()) {
			ComunioDataUpdater.errorDb.addError(new Error(ErrorType.ESPN_PLAYER_INTEGRATOR, "integrateAllPlayers: " + onlyErrorPlayers.toString()));
		}

		return updated;
	}

	/**
	 * Baut ein einzelnes Spieler-JSON für den Integrator (der erwartet ein
	 * "players"-Array).
	 */
	private static JSONObject buildSinglePlayerJson(JSONObject espnPlayer) {
		JSONObject wrapper = new JSONObject();
		JSONArray players = new JSONArray();
		players.put(espnPlayer);
		wrapper.put("players", players);
		return wrapper;
	}

	/**
	 * Extrahiert die Saison-Stats aus einem aufbereiteten ESPN-Spieler-JSON. Die
	 * aufbereiteten Spieler enthalten die Stats NICHT – daher wird hier geprüft, ob
	 * sie vorhanden sind (der RosterCollector könnte sie ergänzen).
	 */
	private static JSONObject extractStatsFromPlayer(JSONObject espnPlayer) {
		JSONObject stats = espnPlayer.optJSONObject("stats");
		return stats;
	}

	/**
	 * Wendet die ESPN-Stats auf ein Spielerstats-Objekt an.
	 */
	private static void applyStats(Spielerstats stats, JSONObject statsJson) {
		if (statsJson.has("foulsCommitted"))
			stats.setFoulsCommitted(statsJson.getInt("foulsCommitted"));
		if (statsJson.has("foulsSuffered"))
			stats.setFoulsSuffered(statsJson.getInt("foulsSuffered"));
		if (statsJson.has("ownGoals"))
			stats.setOwnGoals(statsJson.getInt("ownGoals"));
		if (statsJson.has("appearances"))
			stats.setAppearances(statsJson.getInt("appearances"));
		if (statsJson.has("subIns"))
			stats.setSubIns(statsJson.getInt("subIns"));
		if (statsJson.has("goalAssists"))
			stats.setGoalAssists(statsJson.getInt("goalAssists"));
		if (statsJson.has("offsides"))
			stats.setOffsides(statsJson.getInt("offsides"));
		if (statsJson.has("shotsOnTarget"))
			stats.setShotsOnTarget(statsJson.getInt("shotsOnTarget"));
		if (statsJson.has("totalShots"))
			stats.setTotalShots(statsJson.getInt("totalShots"));
		if (statsJson.has("saves"))
			stats.setSaves(statsJson.getInt("saves"));
		if (statsJson.has("shotsFaced"))
			stats.setShotsFaced(statsJson.getInt("shotsFaced"));
		if (statsJson.has("goalsConceded"))
			stats.setGoalsConceded(statsJson.getInt("goalsConceded"));
		if (statsJson.has("totalGoals"))
			stats.setTore(statsJson.getInt("totalGoals"));
		if (statsJson.has("yellowCards"))
			stats.setGelbekarten(statsJson.getInt("yellowCards"));
		if (statsJson.has("redCards"))
			stats.setRotekarten(statsJson.getInt("redCards"));
	}

}