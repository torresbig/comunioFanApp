package comunio.nas.dataScraper.ligainsider;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.MatchdayInfo;
import comunio.nas.dataVariable.LastUpdates;
import comunio.nas.objects.LigainsiderRankingEntry;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.player.PlayerHelper;
import comunio.nas.util.player.PlayerMatcher;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extrahiert das Ligainsider-Ranking und speichert es als Attribut in der
 * Spielerdatenbank.
 */
public class LigainsiderRankingUpdater {
	private static final Logger LOGGER = LogManager.getLogger(LigainsiderRankingUpdater.class);

	/**
	 * Aktualisiert das Ligainsider-Ranking für alle Spieler in der Datenbank.
	 *
	 * Diese Methode lädt das aktuelle Ranking von Ligainsider, sucht für jeden
	 * Eintrag den passenden Spieler in der lokalen Spielerdatenbank (playerDB) und
	 * ergänzt dessen Attribute um die Rankingdaten. Das Matching erfolgt robust
	 * mittels Namensvarianten und Vereinsnamen (siehe PlayerMatcher). Das Hochladen
	 * der Datenbank auf GitHub erfolgt bewusst NICHT hier, sondern erst im
	 * Hauptprozess, um Datenkonsistenz mit anderen Prozessen zu gewährleisten.
	 *
	 * @param playerDB Die Spielerdatenbank als JSONArray (wird direkt verändert)
	 */
	public static void updateLigainsiderRanking(JSONObject playerDBObject, JSONArray clubDB, MatchdayInfo matchdayInfo, LastUpdates lastUpdates) {
		try {
			LOGGER.info("Starte Ligainsider-Ranking-Update...");

			JSONArray playerDB = playerDBObject.optJSONArray("playerDB");
			if (playerDB == null) {
				LOGGER.log(Level.WARNING, "KEINE SIELERDATENBANK vorhanden!");
				return;
			}

			// ermitteln, ob es schon ein eintrag gibt und gucken wann das datum war.
			ComunioDate lastUpdate = PlayerHelper.getFirstDateByPath(playerDB, "data.attribute.ligainsiderRanking.lastUpdate");
			int lastProcessedMatchday = playerDBObject.optInt("lastProcessedMatchday", 0);

			if (!matchdayInfo.isNewMatchday(lastProcessedMatchday)) {
				if (lastUpdate != null) {
					lastUpdate.addDays(7);
					if (lastUpdate.after(new ComunioDate()) || lastUpdate.equals(new ComunioDate())) {
						LOGGER.info("Ligainsider-Ranking-Update noch aktuell! LastUpdate: " + lastUpdate);
						return;
					}
				}
			}

			// 1. Ranking-Einträge von Ligainsider laden und parsen
			List<LigainsiderRankingEntry> rankingEntries = LigainsiderRankingParser.parseRanking();
			int matched = 0, notMatched = 0;

			// 2. Für jeden Ranking-Eintrag: Spieler in der Datenbank suchen und Attribute
			// ergänzen
			for (LigainsiderRankingEntry entry : rankingEntries) {
				// --- ROBUSTES MATCHING: Namensvarianten und Verein ---
				Set<String> possibleNames = PlayerMatcher.generatePossibleNames(entry.name);
				JSONObject player = PlayerMatcher.findPlayerByNameAndClub(playerDB, // 1. Die Datenbank (JSONArray)
						clubDB, // 2. Die Verinesdatenbank
						entry.name, // 3. Spielername (String)
						entry.verein, // 4. Vereinsname (String)
						possibleNames // 5. Namensvarianten (Collection<String>)
				);

				if (player != null) {
					// Spieler gefunden: Rankingdaten als Attribut anhängen
					JSONObject data = player.getJSONObject("data");
					JSONObject attribute = data.optJSONObject("attribute");
					if (attribute == null) {
						attribute = new JSONObject();
						data.put("attribute", attribute);
					}

					attribute.put("ligainsiderRanking", entry.toJson());
					matched++;
					// Optional: Debug-Logging für Matches
					// LOGGER.fine("[MATCH] " + entry.name + " (" + entry.verein + ") → Spieler-ID:
					// " + player.optString("id"));
				} else {
					notMatched++;
					LOGGER.warning("[NO MATCH] " + entry.name + " (" + entry.verein + ")");
				}
			}

			// 3. Zusammenfassung im Log
			LOGGER.info("Ligainsider-Ranking-Update abgeschlossen. Gematcht: " + matched + ", Nicht gefunden: " + notMatched);
			lastUpdates.setLigainsider(Instant.now());
			// 4. KEIN Upload der Datenbank an dieser Stelle!
			// Das Hochladen erfolgt erst im Hauptprozess, um Überschreibungen und
			// Inkonsistenzen zu vermeiden.
			// LOGGER.info("Spielerdatenbank mit Ligainsider-Ranking auf GitHub
			// hochgeladen.");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Fehler beim Ligainsider-Ranking-Update: " + e.getMessage(), e);
		}
	}


}
