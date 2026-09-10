package comunio.nas.objects.player;

import org.json.JSONObject;
import comunio.nas.objects.orga.ComunioDate;

public class Spielerstats {
	private Integer totalPenalties;
	private Integer ratedGames;
	private Integer playedGames;
	private Integer totalGoals;
	private Integer manOfTheMatchAmount;
	private String punkteDurchschnitt;
	private String notenDurchschnitt;
	private Integer gelbekarten;
	private Integer rotekarten;
	private Integer gelbrotekarten;
	private ComunioDate lastUpdate;

	// ===== ESPN-Saison-Statistiken (Kategorie "General") =====
	private Integer foulsCommitted;
	private Integer foulsSuffered;
	private Integer ownGoals;
	private Integer appearances;
	private Integer subIns;

	// ===== ESPN-Saison-Statistiken (Kategorie "Offensive") =====
	private Integer goalAssists;
	private Integer offsides;
	private Integer shotsOnTarget;
	private Integer totalShots;

	// ===== ESPN-Saison-Statistiken (Kategorie "Goal Keeping") =====
	private Integer saves;
	private Integer shotsFaced;
	private Integer goalsConceded;

	public Spielerstats() {
		totalPenalties = 0;
		ratedGames = 0;
		playedGames = 0;
		totalGoals = 0;
		manOfTheMatchAmount = 0;
		punkteDurchschnitt = "";
		notenDurchschnitt = "";
		gelbekarten = 0;
		rotekarten = 0;
		gelbrotekarten = 0;
		lastUpdate = null;

		foulsCommitted = 0;
		foulsSuffered = 0;
		ownGoals = 0;
		appearances = 0;
		subIns = 0;
		goalAssists = 0;
		offsides = 0;
		shotsOnTarget = 0;
		totalShots = 0;
		saves = 0;
		shotsFaced = 0;
		goalsConceded = 0;
	}
	
	public static Spielerstats fromJSON(JSONObject json) {
	
		Spielerstats stats = new Spielerstats();
		if(json == null) {
			stats.lastUpdate = null;
			return stats;
		}
		stats.notenDurchschnitt = json.optString("notenDurchschnitt", null);
		stats.gelbekarten = json.has("gelbekarten") ? json.optInt("gelbekarten") : null;
		stats.totalPenalties = json.has("totalPenalties") ? json.optInt("totalPenalties") : null;
		stats.ratedGames = json.has("ratedGames") ? json.optInt("ratedGames") : null;
		String date = json.optString("lastUpdate", null);
		if(date != null) {
			stats.lastUpdate = new ComunioDate(date);
		}
		stats.playedGames = json.has("playedGames") ? json.optInt("playedGames") : null;
		stats.totalGoals = json.has("totalGoals") ? json.optInt("totalGoals") : null;
		stats.punkteDurchschnitt = json.optString("punkteDurchschnitt", null);
		stats.gelbrotekarten = json.has("gelbrotekarten") ? json.optInt("gelbrotekarten") : null;
		stats.rotekarten = json.has("rotekarten") ? json.optInt("rotekarten") : null;
		stats.manOfTheMatchAmount = json.has("manOfTheMatchAmount") ? json.optInt("manOfTheMatchAmount") : null;

		// ESPN-Felder
		stats.foulsCommitted = json.has("foulsCommitted") ? json.optInt("foulsCommitted") : null;
		stats.foulsSuffered = json.has("foulsSuffered") ? json.optInt("foulsSuffered") : null;
		stats.ownGoals = json.has("ownGoals") ? json.optInt("ownGoals") : null;
		stats.appearances = json.has("appearances") ? json.optInt("appearances") : null;
		stats.subIns = json.has("subIns") ? json.optInt("subIns") : null;
		stats.goalAssists = json.has("goalAssists") ? json.optInt("goalAssists") : null;
		stats.offsides = json.has("offsides") ? json.optInt("offsides") : null;
		stats.shotsOnTarget = json.has("shotsOnTarget") ? json.optInt("shotsOnTarget") : null;
		stats.totalShots = json.has("totalShots") ? json.optInt("totalShots") : null;
		stats.saves = json.has("saves") ? json.optInt("saves") : null;
		stats.shotsFaced = json.has("shotsFaced") ? json.optInt("shotsFaced") : null;
		stats.goalsConceded = json.has("goalsConceded") ? json.optInt("goalsConceded") : null;
		return stats;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (notenDurchschnitt != null)
			json.put("notenDurchschnitt", notenDurchschnitt);
		if (gelbekarten != null)
			json.put("gelbekarten", gelbekarten);
		if (totalPenalties != null)
			json.put("totalPenalties", totalPenalties);
		if (ratedGames != null)
			json.put("ratedGames", ratedGames);
		if (lastUpdate != null)
			json.put("lastUpdate", lastUpdate.toString());
		if (playedGames != null)
			json.put("playedGames", playedGames);
		if (totalGoals != null)
			json.put("totalGoals", totalGoals);
		if (punkteDurchschnitt != null)
			json.put("punkteDurchschnitt", punkteDurchschnitt);
		if (gelbrotekarten != null)
			json.put("gelbrotekarten", gelbrotekarten);
		if (rotekarten != null)
			json.put("rotekarten", rotekarten);
		if (manOfTheMatchAmount != null)
			json.put("manOfTheMatchAmount", manOfTheMatchAmount);

		// ESPN-Felder
		if (foulsCommitted != null)
			json.put("foulsCommitted", foulsCommitted);
		if (foulsSuffered != null)
			json.put("foulsSuffered", foulsSuffered);
		if (ownGoals != null)
			json.put("ownGoals", ownGoals);
		if (appearances != null)
			json.put("appearances", appearances);
		if (subIns != null)
			json.put("subIns", subIns);
		if (goalAssists != null)
			json.put("goalAssists", goalAssists);
		if (offsides != null)
			json.put("offsides", offsides);
		if (shotsOnTarget != null)
			json.put("shotsOnTarget", shotsOnTarget);
		if (totalShots != null)
			json.put("totalShots", totalShots);
		if (saves != null)
			json.put("saves", saves);
		if (shotsFaced != null)
			json.put("shotsFaced", shotsFaced);
		if (goalsConceded != null)
			json.put("goalsConceded", goalsConceded);
		return json;
	}

	public int getTotalPenalties() {
		return totalPenalties;
	}

	public void setTotalPenalties(int totalPenalties) {
		this.totalPenalties = totalPenalties;
		this.lastUpdate = new ComunioDate();
	}

	public int getRatedGames() {
		return ratedGames;
	}

	public void setRatedGames(int ratedGames) {
		this.ratedGames = ratedGames;
		this.lastUpdate = new ComunioDate();
	}

	public int getPlayedGames() {
		return playedGames;
	}

	public void setPlayedGames(int playedGames) {
		this.playedGames = playedGames;
		this.lastUpdate = new ComunioDate();
	}

	public int getTotalGoals() {
		return totalGoals;
	}

	public void setTotalGoals(int totalGoals) {
		this.totalGoals = totalGoals;
		this.lastUpdate = new ComunioDate();
	}

	public int getManOfTheMatchAmount() {
		return manOfTheMatchAmount;
	}

	public void setManOfTheMatchAmount(int manOfTheMatchAmount) {
		this.manOfTheMatchAmount = manOfTheMatchAmount;
		this.lastUpdate = new ComunioDate();
	}
	

	public String getPunkteDurchschnitt() {
		return punkteDurchschnitt;
	}

	public void setPunkteDurchschnitt(String punkteDurchschnitt) {
		this.punkteDurchschnitt = punkteDurchschnitt;
		this.lastUpdate = new ComunioDate();
	}

	public String getNotenDurchschnitt() {
		return notenDurchschnitt;
	}

	public void setNotenDurchschnitt(String notenDurchschnitt) {
		this.notenDurchschnitt = notenDurchschnitt;
		this.lastUpdate = new ComunioDate();
	}

	public int getGelbekarten() {
		return gelbekarten;
	}

	public void setGelbekarten(int gelbekarten) {
		this.gelbekarten = gelbekarten;
		this.lastUpdate = new ComunioDate();
	}

	public int getRotekarten() {
		return rotekarten;
	}

	public void setRotekarten(int rotekarten) {
		this.rotekarten = rotekarten;
		this.lastUpdate = new ComunioDate();
	}

	public int getGelbrotekarten() {
		return gelbrotekarten;
	}

	public void setGelbrotekarten(int gelbrotekarten) {
		this.gelbrotekarten = gelbrotekarten;
		this.lastUpdate = new ComunioDate();
	}

	// ===== Getter/Setter ESPN-Felder =====

	public int getFoulsCommitted() {
		return foulsCommitted;
	}

	public void setFoulsCommitted(int foulsCommitted) {
		this.foulsCommitted = foulsCommitted;
		this.lastUpdate = new ComunioDate();
	}

	public int getFoulsSuffered() {
		return foulsSuffered;
	}

	public void setFoulsSuffered(int foulsSuffered) {
		this.foulsSuffered = foulsSuffered;
		this.lastUpdate = new ComunioDate();
	}

	public int getOwnGoals() {
		return ownGoals;
	}

	public void setOwnGoals(int ownGoals) {
		this.ownGoals = ownGoals;
		this.lastUpdate = new ComunioDate();
	}

	public int getAppearances() {
		return appearances;
	}

	public void setAppearances(int appearances) {
		this.appearances = appearances;
		this.lastUpdate = new ComunioDate();
	}

	public int getSubIns() {
		return subIns;
	}

	public void setSubIns(int subIns) {
		this.subIns = subIns;
		this.lastUpdate = new ComunioDate();
	}

	public int getGoalAssists() {
		return goalAssists;
	}

	public void setGoalAssists(int goalAssists) {
		this.goalAssists = goalAssists;
		this.lastUpdate = new ComunioDate();
	}

	public int getOffsides() {
		return offsides;
	}

	public void setOffsides(int offsides) {
		this.offsides = offsides;
		this.lastUpdate = new ComunioDate();
	}

	public int getShotsOnTarget() {
		return shotsOnTarget;
	}

	public void setShotsOnTarget(int shotsOnTarget) {
		this.shotsOnTarget = shotsOnTarget;
		this.lastUpdate = new ComunioDate();
	}

	public int getTotalShots() {
		return totalShots;
	}

	public void setTotalShots(int totalShots) {
		this.totalShots = totalShots;
		this.lastUpdate = new ComunioDate();
	}

	public int getSaves() {
		return saves;
	}

	public void setSaves(int saves) {
		this.saves = saves;
		this.lastUpdate = new ComunioDate();
	}

	public int getShotsFaced() {
		return shotsFaced;
	}

	public void setShotsFaced(int shotsFaced) {
		this.shotsFaced = shotsFaced;
		this.lastUpdate = new ComunioDate();
	}

	public int getGoalsConceded() {
		return goalsConceded;
	}

	public void setGoalsConceded(int goalsConceded) {
		this.goalsConceded = goalsConceded;
		this.lastUpdate = new ComunioDate();
	}

	@Override
	public String toString() {
		return "Stats: Played games: " + this.playedGames + ", Gelbekarte/Gelb-Rotekarte/Rotekarte: " + this.gelbekarten + "/" + this.gelbrotekarten + "/" + this.rotekarten + ", Einsätze (gewertet): " + this.ratedGames;
	}

	public ComunioDate getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(ComunioDate lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	
}
