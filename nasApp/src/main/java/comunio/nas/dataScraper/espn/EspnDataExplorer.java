package comunio.nas.dataScraper.espn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Explorer fÃƒÂ¼r die ESPN-Soccer-API (1. Bundesliga, Slug {@code ger.1}).
 * <p>
 * Zweck: herausfinden, welche Daten ESPN liefert Ã¢â‚¬â€œ sowohl
 * <b>Matchdaten</b> (Scoreboard/Summary) als auch <b>Spielerdaten</b>
 * (Roster/Athlete).
 * <p>
 * Der Explorer lÃƒÂ¤dt die Rohdaten, speichert sie unter
 * {@code espn_export.json} und gibt eine aufbereitete Struktur-ÃƒÅ“bersicht auf
 * der Konsole aus.
 */
public class EspnDataExplorer {

	private static final String EXPORT_FILE = "espn_export.json";

	public static void main(String[] args) {
		JSONObject export = new JSONObject();
		List<String> report = new ArrayList<>();

		try {
			System.out.println("=== ESPN Explorer: 1. Bundesliga (ger.1) ===\n");

// 1. Teams
			System.out.println("[1/6] Lade Teams ...");
			JSONObject teams = EspnApiClient.getTeams();
			export.put("teams", teams);
			reportTeams(teams, report);

//// 2. Standings (Tabelle)
//			System.out.println("[2/6] Lade Standings ...");
//			JSONObject standings = EspnApiClient.getStandings();
//			export.put("standings", standings);
//			reportStandings(standings, report);
//
//// 3. Scoreboard – Zeitraum um "heute" (7 Tage, damit beendete + kommende Spiele)
//			
//			// NICHT INTERESSANT! 
//			String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
//			String range = today + "-" + LocalDate.now().plusDays(6).format(DateTimeFormatter.BASIC_ISO_DATE);
//			System.out.println("[3/6] Lade Scoreboard (Zeitraum " + range + ") ...");
//			JSONObject scoreboard = EspnApiClient.getScoreboard(range);
//			export.put("scoreboard", scoreboard);
//			reportScoreboard(scoreboard, report);
//
//			// Aktuellen Spieltag aus dem Scoreboard ableiten (Saison-Start 2026-08 → Spieltag 1)
//			int currentMatchday = deriveMatchday(scoreboard);
//
//// 4. Einzelspiel-Summary des ersten gefundenen Events (falls vorhanden)
//			System.out.println("[4/6] Lade Event-Summary (Beispiel) ...");
//			String eventId = firstEventId(scoreboard);
//			if (eventId != null) {
//				JSONObject summary = EspnApiClient.getEventSummary(eventId);
//				export.put("summary_event_" + eventId, summary);
//				reportSummary(summary, report);
//			} else {
//				report.add("--> Kein Event im aktuellen Scoreboard-Zeitraum gefunden.");
//			}

// 5. Roster von ALLEN Teams (Spielerdaten mit Saison-Statistiken)
			System.out.println("[5/6] Lade Roster (alle Teams) ...");
			List<String> teamIds = allTeamIds(teams);
			for (String tid : teamIds) {
				JSONObject roster = EspnApiClient.getTeamRoster(tid);
				Files.write(Paths.get("teamRoaster_" + tid + ".json"), roster.toString(2).getBytes(StandardCharsets.UTF_8));
				export.put("roster_team_" + tid, roster);
				reportRoster(roster, report);
			}
			if (teamIds.isEmpty()) {
				report.add("--> Keine Teams gefunden.");
			}
//
//			// 5b. Matchday-Stats aller Spieler der geladenen Rosters als JSON speichern
//			System.out.println("[5b] Sammle Matchday-Stats (Snapshot Spieltag " + currentMatchday + ") ...");
//			JSONObject matchdayStatsExport = collectMatchdayStats(export, teamIds, currentMatchday);
//			Files.write(Paths.get("espnMatchdayStats.json"), matchdayStatsExport.toString(2).getBytes(StandardCharsets.UTF_8));
//			report.add("Matchday-Stats gespeichert in: espnMatchdayStats.json (" + matchdayStatsExport.optInt("playerCount", 0) + " Spieler, Spieltag " + currentMatchday + ")");
//
//// 6. Athlete-Profil des ersten Spielers aus dem ersten Roster
//// HINWEIS: Der /athletes/{id}-Endpunkt existiert fÃƒÂ¼r Soccer nicht (HTTP 404).
//// Die Profil-Daten (Geburtsdatum, GrÃƒÂ¶ÃƒÅ¸e, Gewicht, ...) sind bereits im Roster enthalten.
//			System.out.println("[6/6] Lade Athlete-Profil (Beispiel) ...");
//			if (!teamIds.isEmpty()) {
//				JSONObject roster = export.getJSONObject("roster_team_" + teamIds.get(0));
//				String athleteId = firstAthleteId(roster);
//				if (athleteId != null) {
//					try {
//						JSONObject athlete = EspnApiClient.getAthlete(athleteId);
//						export.put("athlete_" + athleteId, athlete);
//						reportAthlete(athlete, report);
//					} catch (IOException e) {
//						report.add("--> Athlete-Endpunkt nicht verfÃƒÂ¼gbar (HTTP 404) Ã¢â‚¬â€œ Profil-Daten stecken bereits im Roster.");
//						report.add("    Beispiel-Profil aus Roster:");
//						reportAthleteFromRoster(roster.getJSONArray("athletes").getJSONObject(0), report);
//					}
//				} else {
//					report.add("--> Kein Athlet im Roster gefunden.");
//				}
//			}
//
//// Export-Datei schreiben
//			Files.write(Paths.get(EXPORT_FILE), export.toString(2).getBytes(StandardCharsets.UTF_8));
//			report.add("");
//			report.add("VollstÃƒÂ¤ndige Rohdaten gespeichert in: " + EXPORT_FILE);

		} catch (IOException | InterruptedException e) {
			report.add("FEHLER: " + e.getMessage());
			e.printStackTrace();
		}

// Ausgabe
		System.out.println("\n================ AUFBEREITETE ÃƒÅ“BERSICHT ================\n");
		for (String line : report) {
			System.out.println(line);
		}
		System.out.println("\n=========================================================\n");
	}

// =========================================================================
// REPORT-METHODEN (aufbereitete, gekÃƒÂ¼rzte Darstellung)
// =========================================================================

	private static void reportTeams(JSONObject teams, List<String> out) {
		JSONArray arr = teams.optJSONArray("sports");
		if (arr == null || arr.isEmpty()) {
			out.add("Teams: keine 'sports'-Struktur. Top-Keys: " + topKeys(teams));
			return;
		}
		JSONObject sport = arr.getJSONObject(0);
		JSONObject league = sport.optJSONArray("leagues").getJSONObject(0);
		JSONArray teamArr = league.optJSONArray("teams");
		out.add("TEAMS (" + teamArr.length() + "):");
		for (int i = 0; i < teamArr.length(); i++) {
			JSONObject t = teamArr.getJSONObject(i).getJSONObject("team");
			out.add("  - id=" + t.optString("id") + " | " + t.optString("displayName") + " | " + t.optString("shortName"));
		}
	}

	private static void reportStandings(JSONObject standings, List<String> out) {
		JSONArray children = standings.optJSONArray("children");
		if (children == null || children.isEmpty()) {
			out.add("Standings: keine 'children'-Struktur. Top-Keys: " + topKeys(standings));
			return;
		}
		JSONObject group = children.getJSONObject(0);
		JSONArray standingsArr = group.optJSONArray("standings");
		if (standingsArr == null) {
			out.add("Standings: kein 'standings'-Array. Top-Keys: " + topKeys(group));
			return;
		}
		out.add("STANDINGS (Tabelle, " + standingsArr.length() + " EintrÃƒÂ¤ge):");
		for (int i = 0; i < standingsArr.length(); i++) {
			JSONObject entry = standingsArr.getJSONObject(i);
			JSONObject team = entry.optJSONObject("team");
			String name = team != null ? team.optString("displayName", "?") : "?";
			JSONArray stats = entry.optJSONArray("stats");
			JSONObject firstStat = stats != null && stats.length() > 0 ? stats.getJSONObject(0) : null;
			out.add("  - " + name + " | Stat-Beispiel: " + (firstStat != null ? firstStat.optString("name") + "=" + firstStat.opt("value") : "keine"));
		}
	}

	private static void reportScoreboard(JSONObject sb, List<String> out) {
		JSONArray events = sb.optJSONArray("events");
		if (events == null || events.isEmpty()) {
			out.add("Scoreboard: keine Events. Top-Keys: " + topKeys(sb));
			return;
		}
		out.add("SCOREBOARD (" + events.length() + " Spiele):");
		for (int i = 0; i < events.length(); i++) {
			JSONObject ev = events.getJSONObject(i);
			JSONArray comps = ev.optJSONArray("competitions");
			JSONObject comp = comps != null && comps.length() > 0 ? comps.getJSONObject(0) : null;
			if (comp == null) {
				out.add("  - " + ev.optString("name") + " (id=" + ev.optString("id") + ") [keine competitions]");
				continue;
			}
			JSONArray teamsArr = comp.optJSONArray("competitors");
			String teams = teamsArr != null && teamsArr.length() == 2 ? teamsArr.getJSONObject(0).optJSONObject("team").optString("abbreviation", "?") + " vs " + teamsArr.getJSONObject(1).optJSONObject("team").optString("abbreviation", "?") : "? vs ?";
			JSONObject status = comp.optJSONObject("status");
			String state = "?";
			if (status != null) {
				JSONObject type = status.optJSONObject("type");
				state = type != null ? type.optString("state", "?") : status.optString("type", "?");
			}
// Formkurve + Bilanz der beiden Teams (z. B. "LDWWW" = letzte 5 Spiele)
			String form = "";
			if (teamsArr != null) {
				for (int t = 0; t < teamsArr.length(); t++) {
					JSONObject c = teamsArr.getJSONObject(t);
					String abbr = c.optJSONObject("team").optString("abbreviation", "?");
					String f = c.optString("form", "");
					String rec = "";
					JSONArray records = c.optJSONArray("records");
					if (records != null && records.length() > 0) {
						rec = " (" + records.getJSONObject(0).optString("summary", "") + ")";
					}
					form += " | " + abbr + " Form=" + f + rec;
				}
			}
			out.add("  - id=" + ev.optString("id") + " | " + teams + " | " + state + " | " + ev.optString("date") + form);
		}
	}

	private static void reportSummary(JSONObject summary, List<String> out) {
		out.add("SUMMARY (Einzelspiel) Top-Keys: " + topKeys(summary));
		JSONObject boxscore = summary.optJSONObject("boxscore");
		if (boxscore != null) {
			JSONArray teams = boxscore.optJSONArray("teams");
			out.add("  boxscore.teams: " + (teams != null ? teams.length() : "null") + " Team-EintrÃƒÂ¤ge (Aufstellungen mit Einzelwerten)");
			if (teams != null && teams.length() > 0) {
				JSONObject t0 = teams.getJSONObject(0);
				out.add("  Beispiel-Team-Eintrag Keys: " + topKeys(t0));
				JSONArray statistics = t0.optJSONArray("statistics");
				if (statistics != null && statistics.length() > 0) {
					JSONObject s0 = statistics.getJSONObject(0);
					JSONArray names = s0.optJSONArray("names");
					JSONArray keys = s0.optJSONArray("keys");
					out.add("  VerknÃƒÂ¼pfte Statistik-Namen: " + (names != null ? names.length() : 0) + " | Keys: " + (keys != null ? keys.length() : 0));
				}
			}
		}
	}

	private static void reportRoster(JSONObject roster, List<String> out) {
		JSONArray athletes = roster.optJSONArray("athletes");
		if (athletes == null || athletes.isEmpty()) {
			out.add("Roster: keine 'athletes'. Top-Keys: " + topKeys(roster));
			return;
		}
		JSONObject first = athletes.getJSONObject(0);
		out.add("ROSTER (" + athletes.length() + " Spieler) Ã¢â‚¬â€œ Beispiel-Spieler: " + first.optString("displayName"));
		out.add("  Athlet-Keys: " + topKeys(first));
// Saison-Statistiken liegen unter "statistics" -> "splits" -> "categories" (General/Offensive/Goal Keeping)
		JSONObject statistics = first.optJSONObject("statistics");
		if (statistics != null) {
			JSONObject splits = statistics.optJSONObject("splits");
			JSONArray categories = splits != null ? splits.optJSONArray("categories") : null;
			if (categories != null) {
				out.add("  Statistik-Kategorien: " + categories.length());
				for (int c = 0; c < categories.length(); c++) {
					JSONObject cat = categories.getJSONObject(c);
					JSONArray stats = cat.optJSONArray("stats");
					out.add("    " + cat.optString("displayName", "?") + " (" + (stats != null ? stats.length() : 0) + " Stats):");
					if (stats != null) {
						for (int s = 0; s < stats.length(); s++) {
							JSONObject st = stats.getJSONObject(s);
							Object val = st.has("displayValue") ? st.opt("displayValue") : st.opt("value");
							out.add("      " + st.optString("displayName", "?") + " = " + val);
						}
					}
				}
			}
		}
	}

	private static void reportAthlete(JSONObject athlete, List<String> out) {
		out.add("ATHLETE (Profil): " + athlete.optString("displayName", "?"));
		out.add("  Keys: " + topKeys(athlete));
		JSONArray stats = athlete.optJSONArray("stats");
		if (stats != null && stats.length() > 0) {
			JSONObject s0 = stats.getJSONObject(0);
			out.add("  Profil-Statistik Keys: " + topKeys(s0));
			out.add("  Profil-Stat-Namen: " + s0.optJSONArray("names"));
			out.add("  Profil-Stat-Werte: " + s0.optJSONArray("values"));
		}
	}

	/**
	 * Zeigt die Profil-Daten eines Athleten direkt aus dem Roster-Objekt an (der
	 * separate /athletes/{id}-Endpunkt existiert fÃƒÂ¼r Soccer nicht).
	 */
	private static void reportAthleteFromRoster(JSONObject athlete, List<String> out) {
// Position und NationalitÃƒÂ¤t sind Objekte, keine Strings
		JSONObject pos = athlete.optJSONObject("position");
		String position = pos != null ? pos.optString("displayName", "-") : athlete.optString("position", "-");
		JSONObject nat = athlete.optJSONObject("citizenshipCountry");
		String nationality = nat != null ? nat.optString("abbreviation", "-") : athlete.optString("citizenshipCountry", "-");

		out.add("  - " + athlete.optString("displayName", "?") + " | id=" + athlete.optString("id") + " | Trikot: " + athlete.optString("jersey", "-") + " | Position: " + position + " | Geboren: " + athlete.optString("dateOfBirth", "-") + " | " + athlete.optString("displayHeight", "-") + " | " + athlete.optString("displayWeight", "-") + " | Nationalität: " + nationality);

// Saison-Statistiken liegen unter "statistics" -> "splits" -> "categories" (Kategorien mit Einzelwerten)
		JSONObject statistics = athlete.optJSONObject("statistics");
		if (statistics != null) {
			JSONObject splits = statistics.optJSONObject("splits");
			JSONArray categories = splits != null ? splits.optJSONArray("categories") : null;
			if (categories != null) {
				out.add("  Saison-Statistiken:");
				for (int c = 0; c < categories.length(); c++) {
					JSONObject cat = categories.getJSONObject(c);
					JSONArray stats = cat.optJSONArray("stats");
					out.add("    " + cat.optString("displayName", "?") + ":");
					if (stats != null) {
						for (int s = 0; s < stats.length(); s++) {
							JSONObject st = stats.getJSONObject(s);
							Object val = st.has("displayValue") ? st.opt("displayValue") : st.opt("value");
							out.add("      " + st.optString("displayName", "?") + " = " + val);
						}
					}
				}
			}
		}
	}

// =========================================================================
// HELFER
// =========================================================================

	/** Kommagetrennte Liste der Top-Level-Keys eines JSON-Objekts. */
	private static String topKeys(JSONObject obj) {
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = obj.keys();
		int count = 0;
		while (it.hasNext() && count < 25) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(it.next());
			count++;
		}
		if (it.hasNext()) {
			sb.append(", ...");
		}
		return sb.toString();
	}

	private static String firstEventId(JSONObject sb) {
		JSONArray events = sb.optJSONArray("events");
		return events != null && events.length() > 0 ? events.getJSONObject(0).optString("id") : null;
	}

	private static List<String> firstTeamIds(JSONObject teams, int max) {
		List<String> ids = new ArrayList<>();
		JSONArray sports = teams.optJSONArray("sports");
		if (sports == null || sports.isEmpty()) {
			return ids;
		}
		JSONArray leagueTeams = sports.getJSONObject(0).optJSONArray("leagues").getJSONObject(0).optJSONArray("teams");
		if (leagueTeams == null) {
			return ids;
		}
		for (int i = 0; i < leagueTeams.length() && ids.size() < max; i++) {
			ids.add(leagueTeams.getJSONObject(i).getJSONObject("team").optString("id"));
		}
		return ids;
	}

	private static List<String> allTeamIds(JSONObject teams) {
		List<String> ids = new ArrayList<>();
		JSONArray sports = teams.optJSONArray("sports");
		if (sports == null || sports.isEmpty()) {
			return ids;
		}
		JSONArray leagueTeams = sports.getJSONObject(0).optJSONArray("leagues").getJSONObject(0).optJSONArray("teams");
		if (leagueTeams == null) {
			return ids;
		}
		for (int i = 0; i < leagueTeams.length(); i++) {
			ids.add(leagueTeams.getJSONObject(i).getJSONObject("team").optString("id"));
		}
		return ids;
	}

	private static String firstAthleteId(JSONObject roster) {
		JSONArray athletes = roster.optJSONArray("athletes");
		return athletes != null && athletes.length() > 0 ? athletes.getJSONObject(0).optString("id") : null;
	}

	private static JSONObject collectMatchdayStats(JSONObject export, List<String> teamIds, int currentMatchday) {
		JSONObject result = new JSONObject();
		JSONArray playerStats = new JSONArray();
		int playerCount = 0;

		// Durchlaufe die Team-IDs und sammele die Matchday-Stats der Spieler
		for (String tid : teamIds) {
			JSONObject roster = export.optJSONObject("roster_team_" + tid);
			if (roster != null) {
				JSONArray athletes = roster.optJSONArray("athletes");
				if (athletes != null) {
					for (int a = 0; a < athletes.length(); a++) {
						JSONObject athlete = athletes.getJSONObject(a);
						String playerId = athlete.optString("id");
						String playerName = athlete.optString("displayName", "");

						// ESPN-Stats aus dem statistics-Block extrahieren
						JSONObject statsJson = extractStatsJson(athlete, currentMatchday);
						if (statsJson != null && statsJson.length() > 0) {
							JSONObject playerStat = new JSONObject();
							playerStat.put("playerId", playerId);
							playerStat.put("playerName", playerName);
							playerStat.put("clubId", tid);
							playerStat.put("stats", statsJson);
							playerStats.put(playerStat);
							playerCount++;
						}
					}
				}
			}
		}
		result.put("playerStats", playerStats);
		result.put("playerCount", playerCount);
		result.put("matchday", currentMatchday);
		result.put("lastUpdate", LocalDate.now().toString());
		return result;
	}

	/**
	 * Extrahiert die ESPN-Saison-Statistiken aus dem statistics-Block eines
	 * Athleten (statistics.splits.categories[].stats[]).
	 */
	private static JSONObject extractStatsJson(JSONObject athlete, int currentMatchday) {
		JSONObject result = new JSONObject();
		JSONObject statistics = athlete.optJSONObject("statistics");
		if (statistics == null) {
			return result;
		}
		JSONObject splits = statistics.optJSONObject("splits");
		if (splits == null) {
			return result;
		}
		JSONArray categories = splits.optJSONArray("categories");
		if (categories == null) {
			return result;
		}
		for (int c = 0; c < categories.length(); c++) {
			JSONObject cat = categories.getJSONObject(c);
			JSONArray stats = cat.optJSONArray("stats");
			if (stats != null) {
				for (int s = 0; s < stats.length(); s++) {
					JSONObject st = stats.getJSONObject(s);
					String name = st.optString("name", "");
					if (!name.isEmpty()) {
						Object val = st.has("value") ? st.opt("value") : 0;
						result.put(name, val);
					}
				}
			}
		}
		return result;
	}

	private static int deriveMatchday(JSONObject scoreboard) {
		JSONArray events = scoreboard.optJSONArray("events");
		if (events == null || events.length() == 0) {
			return 1; // Standardmäßig Spieltag 1, wenn keine Events vorhanden sind
		}
		// Annahme: Der erste Event im Array ist das erste Spiel der Saison
		JSONObject firstEvent = events.getJSONObject(0);
		String startDateStr = firstEvent.optString("date", null);
		if (startDateStr == null) {
			return 1;
		}
		LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
		LocalDate today = LocalDate.now();
		// Berechne die Anzahl der Tage zwischen dem Saisonstart und heute
		long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
		// Jeder Spieltag umfasst etwa 7 Tage (angenommener Abstand zwischen den
		// Spielen)
		return (int) (daysBetween / 7) + 1;
	}
}