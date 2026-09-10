package comunio.nas.dataScraper.espn;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.player.Spielerstats;

/**
 * Integriert die ESPN-Saison-Statistiken in ein {@link Spielerstats}-Objekt.
 * <p>
 * Die ESPN-Rohdaten liegen pro Athlet unter
 * {@code statistics.splits.categories[].stats[]} – jede Kategorie (General,
 * Offensive, Goal Keeping) enthält Einzelwerte mit einem {@code name}-Feld
 * (z. B. {@code appearances}, {@code saves}, {@code totalGoals}).
 * <p>
 * <b>Wichtig:</b> Diese Klasse ist bewusst NICHT in den bestehenden
 * Update-Ablauf eingebaut – sie wird nur aufgerufen, wenn die ESPN-Stats
 * integriert werden sollen.
 */
public class EspnStatsIntegrator {

	private static final Logger LOGGER = LogManager.getLogger(EspnStatsIntegrator.class);

	/** Konstruktor privat – statische Nutzung. */
	private EspnStatsIntegrator() {
	}

	/**
	 * Integriert die ESPN-Saison-Statistiken eines Athleten in ein
	 * {@link Spielerstats}-Objekt.
	 *
	 * @param espnAthlete Das ESPN-Athleten-JSON (aus Roster), enthält den
	 *                    {@code statistics}-Block
	 * @param stats       Das Ziel-{@link Spielerstats}-Objekt (wird befüllt)
	 * @return true, wenn mindestens ein Feld gesetzt wurde
	 */
	public static boolean integrateStats(JSONObject espnAthlete, Spielerstats stats) {
		if (espnAthlete == null || stats == null) {
			LOGGER.warning("EspnStatsIntegrator: espnAthlete oder stats ist null!");
			return false;
		}

		JSONObject statistics = espnAthlete.optJSONObject("statistics");
		if (statistics == null) {
			return false;
		}
		JSONObject splits = statistics.optJSONObject("splits");
		if (splits == null) {
			return false;
		}
		JSONArray categories = splits.optJSONArray("categories");
		if (categories == null) {
			return false;
		}

		boolean changed = false;

		// Alle Kategorien durchgehen (General, Offensive, Goal Keeping)
		for (int c = 0; c < categories.length(); c++) {
			JSONObject category = categories.getJSONObject(c);
			JSONArray statList = category.optJSONArray("stats");
			if (statList == null) {
				continue;
			}
			for (int s = 0; s < statList.length(); s++) {
				JSONObject stat = statList.getJSONObject(s);
				String name = stat.optString("name", "");
				int value = stat.optInt("value", 0);
				changed |= applyStat(stats, name, value);
			}
		}

		return changed;
	}

	/**
	 * Mappt einen ESPN-Statistik-Namen auf das passende Feld in
	 * {@link Spielerstats}.
	 */
	private static boolean applyStat(Spielerstats stats, String name, int value) {
		switch (name) {
		// Kategorie "General"
		case "foulsCommitted":
			stats.setFoulsCommitted(value);
			return true;
		case "foulsSuffered":
			stats.setFoulsSuffered(value);
			return true;
		case "ownGoals":
			stats.setOwnGoals(value);
			return true;
		case "appearances":
			stats.setAppearances(value);
			return true;
		case "subIns":
			stats.setSubIns(value);
			return true;

		// Kategorie "Offensive"
		case "goalAssists":
			stats.setGoalAssists(value);
			return true;
		case "offsides":
			stats.setOffsides(value);
			return true;
		case "shotsOnTarget":
			stats.setShotsOnTarget(value);
			return true;
		case "totalShots":
			stats.setTotalShots(value);
			return true;

		// Kategorie "Goal Keeping"
		case "saves":
			stats.setSaves(value);
			return true;
		case "shotsFaced":
			stats.setShotsFaced(value);
			return true;
		case "goalsConceded":
			stats.setGoalsConceded(value);
			return true;

		// Bekannte ESPN-Namen, die auf bestehende Spielerstats-Felder passen
		case "totalGoals":
			stats.setTotalGoals(value);
			return true;
		case "yellowCards":
			stats.setGelbekarten(value);
			return true;
		case "redCards":
			stats.setRotekarten(value);
			return true;

		default:
			// Unbekannte Statistik ignorieren (z. B. "redCards" → rotekarten ist oben,
			// aber andere ESPN-interne Namen)
			LOGGER.fine("ESPN-Statistik ignoriert: " + name + " = " + value);
			return false;
		}
	}
}