package comunio.nas.objects.player;

import org.json.JSONObject;
import comunio.nas.objects.orga.ComunioDate;

public class Spielerstats {
	private Integer totalPenalties;
	private Integer ratedGames;
	private Integer playedGames;
	private Integer tore;
	private Integer manOfTheMatchAmount;
	private String punkteDurchschnitt;
	private String notenDurchschnitt;
	private Integer gelbekarten;
	private Integer rotekarten;
	private Integer gelbrotekarten;
	private ComunioDate lastUpdate;

	// ===== Comstats-Spieltagswerte =====
	private Integer einsatzzeit;
	private String status;
	private Double xgoals;
	private Double rating;
	private Integer pensSaved;
	private Integer pensMissed;
	private Integer points;
	private Integer subOut;
	private Integer cleanSheet;
	private Integer active;

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
		tore = 0;
		manOfTheMatchAmount = 0;
		punkteDurchschnitt = "";
		notenDurchschnitt = "";
		gelbekarten = 0;
		rotekarten = 0;
		gelbrotekarten = 0;
		lastUpdate = null;

		einsatzzeit = 0;
		status = "";
		xgoals = 0.0;
		rating = 0.0;
		pensSaved = 0;
		pensMissed = 0;
		points = 0;
		subOut = 0;
		cleanSheet = 0;
		active = 0;

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
		if (json.has("tore")) {
			stats.tore = json.optInt("tore");
		} else if (json.has("totalGoals")) {
			stats.tore = json.optInt("totalGoals");
		} else {
			stats.tore = null;
		}
		stats.punkteDurchschnitt = json.optString("punkteDurchschnitt", null);
		stats.gelbrotekarten = json.has("gelbrotekarten") ? json.optInt("gelbrotekarten") : null;
		stats.rotekarten = json.has("rotekarten") ? json.optInt("rotekarten") : null;
		stats.manOfTheMatchAmount = json.has("manOfTheMatchAmount") ? json.optInt("manOfTheMatchAmount") : null;

		// Comstats-Felder
		stats.einsatzzeit = json.has("einsatzzeit") ? json.optInt("einsatzzeit") : null;
		stats.status = json.optString("status", null);
		stats.xgoals = json.has("xgoals") ? json.optDouble("xgoals") : null;
		stats.rating = json.has("rating") ? json.optDouble("rating") : null;
		stats.pensSaved = json.has("pensSaved") ? json.optInt("pensSaved") : null;
		stats.pensMissed = json.has("pensMissed") ? json.optInt("pensMissed") : null;
		stats.points = json.has("points") ? json.optInt("points") : null;
		stats.subOut = json.has("subOut") ? json.optInt("subOut") : null;
		stats.cleanSheet = json.has("cleanSheet") ? json.optInt("cleanSheet") : null;
		stats.active = json.has("active") ? json.optInt("active") : null;

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
		if (tore != null)
			json.put("tore", tore);
		if (punkteDurchschnitt != null)
			json.put("punkteDurchschnitt", punkteDurchschnitt);
		if (gelbrotekarten != null)
			json.put("gelbrotekarten", gelbrotekarten);
		if (rotekarten != null)
			json.put("rotekarten", rotekarten);
		if (manOfTheMatchAmount != null)
			json.put("manOfTheMatchAmount", manOfTheMatchAmount);

		// Comstats-Felder
		if (einsatzzeit != null)
			json.put("einsatzzeit", einsatzzeit);
		if (status != null)
			json.put("status", status);
		if (xgoals != null)
			json.put("xgoals", xgoals);
		if (rating != null)
			json.put("rating", rating);
		if (pensSaved != null)
			json.put("pensSaved", pensSaved);
		if (pensMissed != null)
			json.put("pensMissed", pensMissed);
		if (points != null)
			json.put("points", points);
		if (subOut != null)
			json.put("subOut", subOut);
		if (cleanSheet != null)
			json.put("cleanSheet", cleanSheet);
		if (active != null)
			json.put("active", active);

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

	public int getTore() {
		return tore;
	}

	public void setTore(int tore) {
		this.tore = tore;
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

	// ===== Getter/Setter Comstats-Felder =====

	public int getEinsatzzeit() {
		return einsatzzeit;
	}

	public void setEinsatzzeit(int einsatzzeit) {
		this.einsatzzeit = einsatzzeit;
		this.lastUpdate = new ComunioDate();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		this.lastUpdate = new ComunioDate();
	}

	public double getXgoals() {
		return xgoals;
	}

	public void setXgoals(double xgoals) {
		this.xgoals = xgoals;
		this.lastUpdate = new ComunioDate();
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
		this.lastUpdate = new ComunioDate();
	}

	public int getPensSaved() {
		return pensSaved;
	}

	public void setPensSaved(int pensSaved) {
		this.pensSaved = pensSaved;
		this.lastUpdate = new ComunioDate();
	}

	public int getPensMissed() {
		return pensMissed;
	}

	public void setPensMissed(int pensMissed) {
		this.pensMissed = pensMissed;
		this.lastUpdate = new ComunioDate();
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
		this.lastUpdate = new ComunioDate();
	}

	public int getSubOut() {
		return subOut;
	}

	public void setSubOut(int subOut) {
		this.subOut = subOut;
		this.lastUpdate = new ComunioDate();
	}

	public int getCleanSheet() {
		return cleanSheet;
	}

	public void setCleanSheet(int cleanSheet) {
		this.cleanSheet = cleanSheet;
		this.lastUpdate = new ComunioDate();
	}

	public int getActive() {
		return active;
	}

	public void setActive(int active) {
		this.active = active;
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