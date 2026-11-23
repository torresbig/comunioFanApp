package comunio.nas.dataScraper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataScraper.ligainsider.LigainsiderInjurieAndBannedPlayers;
import comunio.nas.dataScraper.wettbasis.WettbasisInjurieAndBannedPlayers;
import comunio.nas.enu.NewsArt;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.error.Error;
import comunio.nas.error.ErrorType;
import comunio.nas.objects.News;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.club.ClubObject;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.ligainsider.InjuryAndBlockedData;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.wettbasis.WettbasisClubObject;
import comunio.nas.util.ClubMapper;
import comunio.nas.util.player.PlayerMatcher;

public class InjuredAndBanned {

	
	private static final Logger LOGGER = LogManager.getLogger(InjuredAndBanned.class);

	public Map<String, InjuryAndBlockedData> injuriedAndBannedPlayer;
	public Map<String, List<InjuryAndBlockedData>> history;
	public Instant lastUpdate;
	/** Primärquelle für Gleichstand bei gleicher Aktualität. */
	private static final String SOURCE_LIGAINSIDER = "ligainsider";
	/** Sekundärquelle für Abdeckung von fehlenden Spielern. */
	private static final String SOURCE_WETTBASIS = "wettbasis";
	
	
	/**
	 * Aktualisiert den Verletzten-/Sperrenstatus aus mehreren Quellen
	 * (Ligainsider + Wettbasis) und merged die Ergebnisse pro Comunio-Spieler.
	 * <p>
	 * Regeln:
	 * <ul>
	 *   <li>Spieler bleibt verletzt/gesperrt, solange er in mindestens einer Quelle auftaucht.</li>
	 *   <li>Konflikte werden nach Status-Härte, dann Aktualität, dann Quellenstärke gelöst.</li>
	 *   <li>Bei gleichem Status bleibt die Ursprungsquelle erhalten, Infos werden nur ergänzt.</li>
	 * </ul>
	 */
	public void updateAllInjuredBannedPlayer(JSONArray playerDB, Map<String, ClubObject> clubDb, MatchdayInfo currentMatchday, NewsManager newsManager) {
		Map<String, List<InjuryAndBlockedData>> ligainsiderInjuries = LigainsiderInjurieAndBannedPlayers.parseVerletzteUndGesperrte();
		Map<String, List<InjuryAndBlockedData>> wettbasisInjuries = WettbasisInjurieAndBannedPlayers.parseVerletzteUndGesperrte();

		int countOldSizeInjuried = this.injuriedAndBannedPlayer.size();
		int countLigaInjuries = countEntries(ligainsiderInjuries);
		int countWettbasisInjuries = countEntries(wettbasisInjuries);

		if (this.lastUpdate != null && new ComunioDate(this.lastUpdate).isSameDay(new ComunioDate())) {
			return;
		}

		if ((ligainsiderInjuries == null || ligainsiderInjuries.isEmpty()) && (wettbasisInjuries == null || wettbasisInjuries.isEmpty())) {
			LOGGER.info("keine Verletzten oder Gesperrten Spieler gefunden!");
			return;
		}

		Map<String, InjuryAndBlockedData> resolvedCurrentPlayers = new HashMap<>();
		Set<String> activeCurrentPlayerIds = new HashSet<>();

		try {
			processSourceInjuries(ligainsiderInjuries, SOURCE_LIGAINSIDER, playerDB, clubDb, resolvedCurrentPlayers, activeCurrentPlayerIds);
			applyWettbasisClubMapping(clubDb, WettbasisInjurieAndBannedPlayers.getLastClubMapping());
			processSourceInjuries(wettbasisInjuries, SOURCE_WETTBASIS, playerDB, clubDb, resolvedCurrentPlayers, activeCurrentPlayerIds);
		} catch (Exception e) {
			LOGGER.warning("Fehler im prozess für verletzte spieler (Ligainsider/Wettbasis):   " + e.getMessage());
			return;
		}

		for (Map.Entry<String, InjuryAndBlockedData> entry : resolvedCurrentPlayers.entrySet()) {
			mergeStatus(entry.getKey(), entry.getValue(), newsManager);
		}

		handlePlayersLeftInjury(activeCurrentPlayerIds, newsManager);
		this.lastUpdate = Instant.now();
		LOGGER.info("Es wurden " + countVerarbeitung + " Spieler verarbeitet. InjuryMap bevor: " + countOldSizeInjuried + " danach: " + this.injuriedAndBannedPlayer.size() + ", Ligainsider: " + countLigaInjuries + ", Wettbasis: " + countWettbasisInjuries + ", final verletzt/gesperrt: " + resolvedCurrentPlayers.size());

		countVerarbeitung = 0;
	}

	int countVerarbeitung = 0;
	int countRemove = 0;

	/**
	 * Entfernt Spieler aus der Injury-Map, wenn sie in keiner aktuellen Quelle mehr vorkommen.
	 */
	private void handlePlayersLeftInjury(Set<String> currentPlayers, NewsManager newsManager) {
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

	/**
	 * Zählt alle Injury-Einträge über alle Clubs einer Quelle.
	 */
	private int countEntries(Map<String, List<InjuryAndBlockedData>> injuries) {
		if (injuries == null || injuries.isEmpty()) {
			return 0;
		}
		return injuries.values().stream().mapToInt(List::size).sum();
	}

	/**
	 * Verarbeitet eine Einzelquelle, mapped auf Comunio-Spieler und schreibt den
	 * konsolidierten Kandidaten in {@code resolvedCurrentPlayers}.
	 */
	private void processSourceInjuries(Map<String, List<InjuryAndBlockedData>> sourceInjuries, String source, JSONArray playerDB, Map<String, ClubObject> clubDb,
			Map<String, InjuryAndBlockedData> resolvedCurrentPlayers, Set<String> activeCurrentPlayerIds) {
		if (sourceInjuries == null || sourceInjuries.isEmpty()) {
			return;
		}

		for (Map.Entry<String, List<InjuryAndBlockedData>> entry : sourceInjuries.entrySet()) {
			String clubId = ClubMapper.getComunioIdFromName(entry.getKey(), clubDb);
			String clubName = clubId != null && clubDb.containsKey(clubId) ? clubDb.get(clubId).getName() : null;
			if (clubName == null) {
				LOGGER.warning("Kein passenden Club zur ID gefunden (" + source + "): " + clubId + " / " + entry.getKey());
				continue;
			}

			for (InjuryAndBlockedData currentPlayer : entry.getValue()) {
				JSONObject player = PlayerMatcher.findPlayerByNameAndClub(playerDB, clubDb, clubName, currentPlayer.getPlayerName());
				if (player == null) {
					player = PlayerMatcher.findPlayerByNameAndClub(playerDB, clubDb, clubName, currentPlayer.extractPlayerNameFromUrl());
				}

				if (player == null || player.isEmpty()) {
					ComunioDataUpdater.errorDb.addError(new Error(ErrorType.LIGAINSIDERSTATUS,
							"Spieler wurde nicht gefunden! Spieler " + currentPlayer.getPlayerName() + " | Club: " + clubName + " | Quelle: " + source));
					continue;
				}

				String playerId = player.optString("id", "");
				if (playerId.isBlank()) {
					LOGGER.warning("Ungültige ComunioPlayerId für Spieler: " + currentPlayer.getPlayerName() + " – wird übersprungen");
					continue;
				}

				currentPlayer.setComunioPlayerId(playerId);
				currentPlayer.setQuelle(source);
				activeCurrentPlayerIds.add(playerId);
				countVerarbeitung++;

				InjuryAndBlockedData oldResolved = resolvedCurrentPlayers.get(playerId);
				InjuryAndBlockedData newResolved = resolveSourceConflict(oldResolved, currentPlayer);
				resolvedCurrentPlayers.put(playerId, newResolved);

				JSONObject data = player.optJSONObject("data");
				if (data != null) {
					data.put(source, currentPlayer.toPlayerDataJSON());
				}
			}
		}
	}

	/**
	 * Überträgt zuletzt erkannte Wettbasis-Clubinformationen in die Club-DB,
	 * sofern ein Comunio-Club zugeordnet werden kann.
	 */
	private void applyWettbasisClubMapping(Map<String, ClubObject> clubDb, Map<String, WettbasisClubObject> wettbasisClubMap) {
		if (clubDb == null || clubDb.isEmpty() || wettbasisClubMap == null || wettbasisClubMap.isEmpty()) {
			return;
		}
		for (Map.Entry<String, WettbasisClubObject> entry : wettbasisClubMap.entrySet()) {
			ClubObject comunioClub = ClubMapper.getComunioClubObjectFromName(entry.getKey(), clubDb);
			if (comunioClub != null) {
				comunioClub.setWettbasisClub(entry.getValue());
			}
		}
	}

	/**
	 * Löst Konflikte zwischen zwei Quellen-Datensätzen desselben Spielers.
	 * Reihenfolge: Status-Härte -> Aktualität -> Quellenstärke.
	 */
	private InjuryAndBlockedData resolveSourceConflict(InjuryAndBlockedData existing, InjuryAndBlockedData candidate) {
		if (existing == null) {
			return candidate;
		}
		if (candidate == null) {
			return existing;
		}

		SpielerStatus existingStatus = existing.getStatus();
		SpielerStatus candidateStatus = candidate.getStatus();

		if (!existingStatus.equals(candidateStatus)) {
			int severityCompare = Integer.compare(statusSeverity(candidateStatus), statusSeverity(existingStatus));
			if (severityCompare > 0) {
				return enrichMissingFields(candidate, existing);
			}
			if (severityCompare < 0) {
				return enrichMissingFields(existing, candidate);
			}

			int recencyCompare = compareRecency(candidate, existing);
			if (recencyCompare > 0) {
				return enrichMissingFields(candidate, existing);
			}
			if (recencyCompare < 0) {
				return enrichMissingFields(existing, candidate);
			}

			int sourceCompare = Integer.compare(sourcePriority(candidate.getQuelle()), sourcePriority(existing.getQuelle()));
			if (sourceCompare > 0) {
				return enrichMissingFields(candidate, existing);
			}
			return enrichMissingFields(existing, candidate);
		}

		// Kein Statuswechsel: Ursprung bleibt, nur fehlende Infos ergänzen.
		return enrichMissingFields(existing, candidate);
	}

	/**
	 * Ergänzt nur fehlende Attribute aus {@code fallback} in {@code base}.
	 * Bereits gesetzte Felder in {@code base} bleiben unverändert.
	 */
	private InjuryAndBlockedData enrichMissingFields(InjuryAndBlockedData base, InjuryAndBlockedData fallback) {
		if (base == null) {
			return fallback;
		}
		if (fallback == null) {
			return base;
		}

		if (isBlank(base.getPlayerName()) && !isBlank(fallback.getPlayerName())) {
			base.setPlayerName(fallback.getPlayerName());
		}
		if (isBlank(base.getClub()) && !isBlank(fallback.getClub())) {
			base.setClub(fallback.getClub());
		}
		if (isBlank(base.getPlayerLink()) && !isBlank(fallback.getPlayerLink())) {
			base.setPlayerLink(fallback.getPlayerLink());
		}
		if (isBlank(base.getLigainsiderPlayerId()) && !isBlank(fallback.getLigainsiderPlayerId())) {
			base.setLigainsiderPlayerId(fallback.getLigainsiderPlayerId());
		}
		if (isBlank(base.getReason()) && !isBlank(fallback.getReason())) {
			base.setReason(fallback.getReason());
		}
		if (isBlank(base.getLastNewsLink()) && !isBlank(fallback.getLastNewsLink())) {
			base.setLastNewsLink(fallback.getLastNewsLink());
		}
		if (isBlank(base.getLastNewsText()) && !isBlank(fallback.getLastNewsText())) {
			base.setLastNewsText(fallback.getLastNewsText());
		}
		if (isBlank(base.getSinceString()) && !isBlank(fallback.getSinceString())) {
			base.setSinceString(fallback.getSinceString());
		}
		if (isBlank(base.getComunioPlayerId()) && !isBlank(fallback.getComunioPlayerId())) {
			base.setComunioPlayerId(fallback.getComunioPlayerId());
		}
		return base;
	}

	/**
	 * Definiert die Quellenstärke bei Gleichstand.
	 * Ligainsider > Wettbasis > unbekannt.
	 */
	private int sourcePriority(String source) {
		if (SOURCE_LIGAINSIDER.equalsIgnoreCase(source)) {
			return 2;
		}
		if (SOURCE_WETTBASIS.equalsIgnoreCase(source)) {
			return 1;
		}
		return 0;
	}

	/**
	 * Vergleicht zwei Datensätze nach Relevanzzeitpunkt.
	 * Größerer Rückgabewert bedeutet: {@code a} ist aktueller als {@code b}.
	 */
	private int compareRecency(InjuryAndBlockedData a, InjuryAndBlockedData b) {
		Instant ta = getRelevantTimestamp(a);
		Instant tb = getRelevantTimestamp(b);
		return ta.compareTo(tb);
	}

	/**
	 * Ermittelt einen für den Recency-Vergleich nutzbaren Zeitstempel.
	 * Bevorzugt sinceTimestamp, sonst abfrageTimestamp.
	 */
	private Instant getRelevantTimestamp(InjuryAndBlockedData data) {
		if (data == null) {
			return Instant.EPOCH;
		}
		if (data.getSinceTimestamp() != null) {
			return data.getSinceTimestamp();
		}
		if (data.getAbfrageTimestamp() != null) {
			return data.getAbfrageTimestamp();
		}
		return Instant.EPOCH;
	}

	/**
	 * Ordnet Spielerstatus eine fachliche Härte zu.
	 * Höherer Wert = schwerwiegenderer Ausfall.
	 */
	private int statusSeverity(SpielerStatus status) {
		if (status == null) {
			return 0;
		}
		switch (status) {
		case VERLETZT:
			return 100;
		case GESPERRT:
		case ROTE_KARTE:
		case GELBROTE_KARTE:
		case FUENFTE_GELBE_KARTE:
			return 95;
		case AUFBAUTRAINING:
			return 80;
		case NICHT_IM_KADER:
			return 70;
		case NICHT_IN_LIGA:
			return 60;
		case SONSTIGES:
			return 50;
		case UNBESTIMMT:
			return 40;
		case AKTIV:
		default:
			return 0;
		}
	}

	/**
	 * Null-safe Prüfung auf leere Strings.
	 */
	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}


	public InjuredAndBanned() {
		this.injuriedAndBannedPlayer = new HashMap<>();
		this.history = new HashMap<>();
	}
	private void addPlayerToHistory(String playerId, InjuryAndBlockedData data) {
		this.history.computeIfAbsent(playerId, k -> new ArrayList<>()).add(data);
	}

	
	/**
	 * Erstellt eine Instanz aus einem JSONObject mit der neuen Struktur:
	 * {
	 *   "injuriedAndBannedPlayer": { "playerId": { ...playerData... } },
	 *   "history": { "playerId": [{ ... }, ...] },
	 *   "lastUpdate": "..."
	 * }
	 */
	private static InjuredAndBanned fromJSON(JSONObject rootJson) {
		InjuredAndBanned instance = new InjuredAndBanned();
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
	private static InjuredAndBanned fromJSON(JSONArray jsonArray) {
		InjuredAndBanned instance = new InjuredAndBanned();
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
	 * @return neue Instanz von {@link InjuredAndBanned}.
	 */
	public static InjuredAndBanned fromJSON(Object obj) {
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

	/**
	 * Merged einen finalen (bereits über Quellen konsolidierten) Spielerstatus in
	 * die persistente Injury-Map inklusive History/News-Handling.
	 */
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
			// No status change: origin bleibt, aber fehlende Infos werden ergänzt.
			enrichMissingFields(oldData, newData);
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
