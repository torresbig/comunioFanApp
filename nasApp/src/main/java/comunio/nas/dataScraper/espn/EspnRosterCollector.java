package comunio.nas.dataScraper.espn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;

/**
 * Sammelt die Kader (Roster) aller 18 Bundesliga-Vereine von der ESPN-API,
 * bereitet die Spielerdaten auf und speichert sie als {@code espnSpieler.json}.
 * <p>
 * Zusätzlich wird das Vereins-Mapping (ESPN-ID → Comunio-ID) als
 * {@code espnClubMapping.json} exportiert.
 * <p>
 * <b>Wichtig:</b> Es werden NUR die Profil-Daten (Spielerdaten) aufbereitet –
 * die Saison-Statistiken (Spielerstats) bleiben bewusst als Rohdaten erhalten,
 * damit sie separat ausgewertet werden können.
 */
public class EspnRosterCollector {

	private static final Logger LOGGER = LogManager.getLogger(EspnRosterCollector.class);

	/** Ausgabedatei mit allen aufbereiteten Spielern aller Vereine. */
	public static final String PLAYER_EXPORT_FILE = "espnSpieler.json";

	/** Ausgabedatei mit dem Vereins-Mapping. */
	public static final String MAPPING_EXPORT_FILE = "espnClubMapping.json";

	/** Konstruktor privat – statische Nutzung. */
	private EspnRosterCollector() {
	}

	/**
	 * Hauptmethode: lädt alle Rosters, bereitet sie auf und speichert die JSONs.
	 *
	 * @param clubDB Comunio-Vereinsdatenbank (für das Mapping, kann null sein)
	 * @return JSONObject mit den aufbereiteten Spielern (für Tests/Weiterverarbeitung)
	 */
	public static JSONObject collectAllRosters(JSONArray clubDB) throws IOException, InterruptedException {
		JSONObject result = new JSONObject();
		JSONArray players = new JSONArray();
		JSONArray teams = new JSONArray();

		// 1. Teams laden
		JSONObject espnTeams = EspnApiClient.getTeams();
		JSONArray sports = espnTeams.optJSONArray("sports");
		if (sports == null || sports.isEmpty()) {
			LOGGER.warning("ESPN: keine Teams gefunden!");
			return result;
		}
		JSONArray leagueTeams = sports.getJSONObject(0).optJSONArray("leagues").getJSONObject(0).optJSONArray("teams");

		// 2. Mapping bauen (falls clubDB vorhanden)
		Map<String, String> espnToComunio = null;
		if (clubDB != null) {
			espnToComunio = EspnClubMapper.buildEspnToComunioMap(espnTeams, clubDB);
		}

		// 3. Pro Team Roster laden und Spieler aufbereiten
		for (int i = 0; i < leagueTeams.length(); i++) {
			JSONObject team = leagueTeams.getJSONObject(i).optJSONObject("team");
			if (team == null) {
				continue;
			}
			String teamId = team.optString("id", "");
			String teamName = team.optString("displayName", "");

			LOGGER.info("ESPN-Roster: " + teamName + " (ID " + teamId + ")");
			JSONObject roster = EspnApiClient.getTeamRoster(teamId);
			JSONArray athletes = roster.optJSONArray("athletes");
			if (athletes == null) {
				LOGGER.warning("ESPN: kein 'athletes'-Array für Team " + teamName);
				continue;
			}

			// Team-Info für die Übersicht
			JSONObject teamInfo = new JSONObject();
			teamInfo.put("espnId", teamId);
			teamInfo.put("name", teamName);
			teamInfo.put("abbreviation", team.optString("abbreviation", ""));
			if (espnToComunio != null) {
				String comunioId = espnToComunio.get(teamId);
				teamInfo.put("comunioId", comunioId != null ? comunioId : JSONObject.NULL);
			}
			teamInfo.put("playerCount", athletes.length());
			teams.put(teamInfo);

			// Spieler aufbereiten
			for (int j = 0; j < athletes.length(); j++) {
				JSONObject athlete = athletes.getJSONObject(j);
				JSONObject player = buildPlayerJson(athlete, teamId, teamName);
				players.put(player);
			}
		}

		// 4. Ergebnis zusammenbauen
		result.put("lastUpdate", java.time.LocalDate.now().toString());
		result.put("teamCount", teams.length());
		result.put("playerCount", players.length());
		result.put("teams", teams);
		result.put("players", players);

		// 5. Dateien schreiben
		Files.write(Paths.get(PLAYER_EXPORT_FILE), result.toString(2).getBytes(StandardCharsets.UTF_8));
		LOGGER.info("ESPN-Spieler gespeichert: " + PLAYER_EXPORT_FILE + " (" + players.length() + " Spieler)");

		if (clubDB != null) {
			JSONObject mapping = EspnClubMapper.buildMappingJson(espnTeams, clubDB);
			Files.write(Paths.get(MAPPING_EXPORT_FILE), mapping.toString(2).getBytes(StandardCharsets.UTF_8));
			LOGGER.info("ESPN-Mapping gespeichert: " + MAPPING_EXPORT_FILE);
		}

		return result;
	}

	/**
	 * Baut aus einem ESPN-Athleten ein aufbereitetes Spieler-JSON mit den
	 * Profil-Daten (ohne Saison-Statistiken).
	 */
	private static JSONObject buildPlayerJson(JSONObject athlete, String teamId, String teamName) {
		JSONObject player = new JSONObject();

		// Grunddaten
		player.put("espnId", athlete.optString("id", ""));
		player.put("name", athlete.optString("displayName", ""));
		player.put("firstName", athlete.optString("firstName", ""));
		player.put("lastName", athlete.optString("lastName", ""));
		player.put("shortName", athlete.optString("shortName", ""));

		// Verein
		JSONObject club = new JSONObject();
		club.put("espnId", teamId);
		club.put("name", teamName);
		player.put("club", club);

		// Trikotnummer
		player.put("jersey", athlete.optString("jersey", ""));

		// Position (Objekt → displayName)
		JSONObject pos = athlete.optJSONObject("position");
		player.put("position", pos != null ? pos.optString("displayName", "") : athlete.optString("position", ""));

		// Nationalität (Objekt → abbreviation)
		JSONObject nat = athlete.optJSONObject("citizenshipCountry");
		player.put("nationality", nat != null ? nat.optString("abbreviation", "") : athlete.optString("citizenshipCountry", ""));

		// Geburtsdatum (ISO → dd.MM.yyyy)
		String dob = athlete.optString("dateOfBirth", "");
		player.put("dateOfBirth", formatDate(dob));

		// Größe/Gewicht
		player.put("height", athlete.optString("displayHeight", ""));
		player.put("weight", athlete.optString("displayWeight", ""));

		// Alter
		player.put("age", athlete.optInt("age", 0));

		// Status (aktiv/verletzt etc.)
		JSONObject status = athlete.optJSONObject("status");
		player.put("status", status != null ? status.optString("name", "") : "");

		// Verletzungen (falls vorhanden)
		JSONArray injuries = athlete.optJSONArray("injuries");
		if (injuries != null && injuries.length() > 0) {
			JSONArray injuryList = new JSONArray();
			for (int i = 0; i < injuries.length(); i++) {
				JSONObject inj = injuries.getJSONObject(i);
				JSONObject entry = new JSONObject();
				entry.put("type", inj.optString("type", ""));
				entry.put("status", inj.optString("status", ""));
				entry.put("detail", inj.optString("detail", ""));
				injuryList.put(entry);
			}
			player.put("injuries", injuryList);
		}

		return player;
	}

	/**
	 * Formatiert ein ISO-Datum (z. B. "1992-08-04T07:00Z") zu "dd.MM.yyyy".
	 * Gibt den Original-String zurück, wenn das Format nicht erkannt wird.
	 */
	private static String formatDate(String isoDate) {
		if (isoDate == null || isoDate.isBlank()) {
			return "";
		}
		try {
			java.time.LocalDate date = java.time.LocalDate.parse(isoDate.substring(0, 10));
			return date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		} catch (Exception e) {
			return isoDate;
		}
	}

	/**
	 * main-Methode für den manuellen Aufruf (ohne Comunio-Login).
	 * Erwartet optional den Pfad zur Vereinsdatenbank als Argument.
	 */
	public static void main(String[] args) {
		try {
			JSONArray clubDB = null;
			if (args.length > 0) {
				String content = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
				clubDB = new JSONArray(content);
			}
			JSONObject result = collectAllRosters(clubDB);
			System.out.println("Fertig! " + result.optInt("playerCount", 0) + " Spieler aus " + result.optInt("teamCount", 0) + " Vereinen gespeichert.");
		} catch (Exception e) {
			System.err.println("FEHLER: " + e.getMessage());
			e.printStackTrace();
		}
	}
}