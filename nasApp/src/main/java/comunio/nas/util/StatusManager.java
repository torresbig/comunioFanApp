package comunio.nas.util;

import comunio.nas.enu.NewsArt;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.player.Status;
import comunio.nas.util.player.PlayerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Zentrale Hilfsklasse zum Lesen und Schreiben des Spieler-Status aus der
 * zentralen injuryDB (Map von playerId -> Status-JSON).
 */
public class StatusManager {

	private static final Logger LOGGER = LogManager.getLogger(StatusManager.class);

	private Map<String, List<Status>> statusBuffer; // Buffer für Spielerstatus, damit nicht mehrfach in der injuryDB gelesen werden
	private JSONObject injuryDB; // Referenz auf die zentrale Status-Datenbank (wird in mergeAllStatuses übergeben)


	public StatusManager(JSONObject injuryDB) {
		statusBuffer = new HashMap<>();
		this.setInjuryDB(injuryDB);
	}

	/**
	 * Fügt einen neuen Status für einen Spieler zum Status-Buffer hinzu. Dieser
	 * Buffer wird später verwendet, um die Informationen zu einem konsistenten
	 * Status zusammenzuführen, bevor sie in der injuryDB gespeichert werden.
	 *
	 * @param playerId Die ID des Spielers
	 * @param status   Das Status-Objekt, das zum Buffer hinzugefügt werden soll
	 */
	public void addStatusToBuffer(String playerId, Status status) {
		if (playerId == null || playerId.isEmpty() || status == null) {
			return;
		}
		statusBuffer.computeIfAbsent(playerId, k -> new java.util.ArrayList<>()).add(status);
	}

	/**
	 * Liest den aktuellen Status eines Spielers aus der zentralen Status‑Datenbank
	 * (injuryDB) aus. Wenn kein Eintrag für den Spieler vorhanden ist, wird ein
	 * Standard‑Status (UNBESTIMMT) zurückgegeben.
	 *
	 * @param playerId  Die ID des Spielers
	 * @param injuryDB  Die zentrale Status‑Datenbank (JSON‑Objekt)
	 * @return Das Status‑Objekt für den Spieler oder null, wenn kein Eintrag in der injuryDB vorhanden ist
	 */
	public static Status getStatusForPlayer(String playerId, JSONObject injuryDB) {
		if (injuryDB == null || playerId == null || playerId.isEmpty()) {
			return createAktivStatus();
		}
		JSONObject statusJson = injuryDB.optJSONObject(playerId, null);
		return (statusJson == null) ? createAktivStatus() : Status.fromJSON(statusJson);
	}

	/**
	 * Speichert den aktualisierten Status eines Spielers in der zentralen
	 * Status‑Datenbank. Darf nur am Ende aller Zusammenführungen aufgerufen werden,
	 * damit die News‑Generierung korrekt funktioniert.
	 *
	 * @param playerId Die ID des Spielers
	 * @param status   Das aktuelle {@link Status}-Objekt
	 * @param injuryDB Die zentrale Status‑Datenbank (JSON‑Objekt)
	 */
	private static void setStatusForPlayer(String playerId, Status status, JSONObject injuryDB) {
		if (injuryDB == null || playerId == null || playerId.isEmpty() || status == null) {
			return;
		}
		injuryDB.put(playerId, status.toJSON());
	}

	/**
	 * Erzeugt einen Standard‑Status (UNBESTIMMT).
	 *
	 * @return {@link Status}-Objekt mit Default‑Werten
	 */
	private static Status createDefaultStatus() {
		Status defaultStatus = new Status();
		defaultStatus.setStatus(SpielerStatus.UNBESTIMMT);
		defaultStatus.setDetails("");
		defaultStatus.setGrund("");
		defaultStatus.setSeit(new ComunioDate().toString());
		defaultStatus.setBis("");
		return defaultStatus;
	}
	
	/**
	 * Erzeugt einen Standard‑Status (UNBESTIMMT).
	 *
	 * @return {@link Status}-Objekt mit Default‑Werten
	 */
	private static Status createAktivStatus() {
		Status defaultStatus = createDefaultStatus();
		defaultStatus.setStatus(SpielerStatus.AKTIV);
		return defaultStatus;
	}

	/**
	 * Führt die Status‑Informationen aus dem Status‑Buffer mit den bestehenden
	 * Einträgen in der injuryDB zusammen. Dabei wird die Merge‑Logik angewendet, um
	 * die verschiedenen Einträge zu einem konsistenten Status zu kombinieren. Am
	 * Ende werden die Änderungen in der injuryDB gespeichert und ggf. News
	 * generiert.
	 *
	 * @param playerId     Die ID des Spielers
	 * @param playerName   Der Name des Spielers (für News‑Generierung)
	 * @param injuryDB     Die zentrale Status‑Datenbank (JSON‑Objekt)
	 * @param statusBuffer Der Status‑Buffer mit neuen Informationen (Map von
	 *                     playerId -> List<Status>)
	 * @param newsManager  Das NewsManager-Objekt zur Verwaltung von News
	 */
	private void mergeStatusForPlayer(String playerId, String playerName, NewsManager newsManager) {

		Status existingDB = getStatusForPlayer(playerId, this.injuryDB);
		boolean hasPlayerInDb = this.injuryDB.has(playerId);

		if (this.statusBuffer == null || this.statusBuffer.isEmpty() || !this.statusBuffer.containsKey(playerId)) {
			if (hasPlayerInDb) {
				this.injuryDB.remove(playerId);
				setNewsForPlayerWiederGesund(playerName, playerId, newsManager);
				return;
			} else {
				LOGGER.info("StatusBuffer enthält keinen Eintrag für Spieler: %s (%s), aber auch kein Eintrag in injuryDB. Keine Aktion erforderlich.".formatted(playerName, playerId));
				return;
			}
		}

		List<Status> incomingList = this.statusBuffer.get(playerId);
		if (incomingList == null || incomingList.isEmpty()) {
			if (hasPlayerInDb) {
				this.injuryDB.remove(playerId);
				setNewsForPlayerWiederGesund(playerName, playerId, newsManager);
				return;
			} else {
				LOGGER.info("StatusBuffer enthält keinen Eintrag für Spieler: %s (%s), aber auch kein Eintrag in injuryDB. Keine Aktion erforderlich.".formatted(playerName, playerId));
				return;
			}
		}

		// ---------- Merge‑Logik ----------
		Status mergedStatus;
		if (incomingList.size() == 1) {
			mergedStatus = incomingList.get(0);
			LOGGER.info("Einzelner Statuseintrag für Spieler %s (%s) wird übernommen.".formatted(playerName, playerId));
		} else {
			// Mehrere Einträge: sukzessives Mergen (1+2, Ergebnis+3, Ergebnis+4, …)
			mergedStatus = incomingList.get(0);
			for (int i = 1; i < incomingList.size(); i++) {
				mergedStatus = StatusMerger.merge(mergedStatus, incomingList.get(i));
			}
			LOGGER.info("Mehrere Statuseinträge für Spieler %s (%s) wurden zusammengeführt (%d Einträge).".formatted(playerName, playerId, incomingList.size()));
		}

		// ---------- Persistieren und News erzeugen ----------
		setStatusForPlayer(playerId, mergedStatus, this.injuryDB);
		setNewsForPlayer(mergedStatus, existingDB, playerName, playerId, newsManager);
	}

	public void mergeAllStatuses(NewsManager newsManager, JSONObject playerDBObject, JSONObject notInLigaDBObj) {
		if (this.statusBuffer == null || this.statusBuffer.isEmpty()) {
			LOGGER.info("StatusBuffer ist leer. Keine Status‑Änderungen zu verarbeiten.");
			return;
		}
		JSONArray playerDB = playerDBObject.optJSONArray("playerDB");

		for (String playerId : this.statusBuffer.keySet()) {
			JSONObject playerJson = PlayerHelper.findPlayerByComunioId(playerDB, playerId, notInLigaDBObj);
			String playerName = (playerJson != null) ? playerJson.optString("name", "UNBEKANNT") : "UNBEKANNT";
			mergeStatusForPlayer(playerId, playerName, newsManager);
		}
	}

	/**
	 * Hilfsmethode zur Generierung von News basierend auf dem Statuswechsel eines
	 * Spielers. Diese Methode vergleicht den neuen Status mit dem bestehenden
	 * Eintrag in der injuryDB und erstellt eine entsprechende News, wenn sich der
	 * Status geändert hat.
	 *
	 * @param newStatus   Der neu berechnete Status des Spielers
	 * @param existingDB  Der bestehende Status des Spielers aus der injuryDB (kann
	 *                    null sein)
	 * @param playerName  Der Name des Spielers (für die News‑Generierung)
	 * @param playerId    Die ID des Spielers (für die News‑Generierung)
	 * @param newsManager Das NewsManager-Objekt zur Verwaltung von News
	 */
	private static void setNewsForPlayer(Status newStatus, Status existingDB, String playerName, String playerId, NewsManager newsManager) {

		if (existingDB == null) {
			existingDB = createDefaultStatus();
		}

		if (newStatus.getStatus() != existingDB.getStatus()) {
			String newsText = "";
			switch (newStatus.getStatus()) {
			case NICHT_IN_LIGA:
				newsText = "Statuswechsel: " + playerName + " (" + playerId + ") ist jetzt NICHT_IN_LIGA";
				break;
			case AKTIV:
				newsText = "Statuswechsel: " + playerName + " (" + playerId + ") ist wieder AKTIV";
				break;
			case VERLETZT, AUFBAUTRAINING:
				newsText = "Statuswechsel: " + playerName + " (" + playerId + ") ist jetzt " + newStatus.getStatus().toString();
				break;
			case FUENFTE_GELBE_KARTE, GELBROTE_KARTE, GESPERRT, ROTE_KARTE:
				newsText = "Statuswechsel: " + playerName + " (" + playerId + ") ist jetzt geperrt (" + newStatus.getStatus().toString() + ")";
				break;
			default:
				break;

			}
			News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);

			if (!newsManager.contains(news)) {
				newsManager.addNews(news, true);
			}
		}
	}

	private static void setNewsForPlayerWiederGesund(String playerName, String playerId, NewsManager newsManager) {
		String newsText = "Statuswechsel: " + playerName + " (" + playerId + ") ist wieder AKTIV";
		News news = new News(NewsArt.SPIELERSTATUS, newsText, playerId);

		if (!newsManager.contains(news)) {
			newsManager.addNews(news, true);
		}
	}

	public JSONObject getInjuryDB() {
		
		return injuryDB;
	}

	public void setInjuryDB(JSONObject injuryDB) {
		this.injuryDB = injuryDB;
	}

}