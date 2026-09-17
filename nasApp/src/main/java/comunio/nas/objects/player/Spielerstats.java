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
	private Integer subIn;

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
		stats.subIn = json.has("subIn") ? json.optInt("subIn") : null;
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
		if (subIn != null)
			json.put("subIn", subIn);

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

	public Integer getTotalPenalties() {
	    return totalPenalties;
	}

	public void setTotalPenalties(Integer totalPenalties) {
	    if (totalPenalties != null) {
	        this.totalPenalties = totalPenalties;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getRatedGames() {
	    return ratedGames;
	}

	public void setRatedGames(Integer ratedGames) {
	    if (ratedGames != null) {
	        this.ratedGames = ratedGames;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getPlayedGames() {
	    return playedGames;
	}

	public void setPlayedGames(Integer playedGames) {
	    if (playedGames != null) {
	        this.playedGames = playedGames;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getTore() {
	    return tore;
	}

	public void setTore(Integer tore) {
	    if (tore != null) {
	        this.tore = tore;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getManOfTheMatchAmount() {
	    return manOfTheMatchAmount;
	}

	public void setManOfTheMatchAmount(Integer manOfTheMatchAmount) {
	    if (manOfTheMatchAmount != null) {
	        this.manOfTheMatchAmount = manOfTheMatchAmount;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public String getPunkteDurchschnitt() {
	    return punkteDurchschnitt;
	}

	public void setPunkteDurchschnitt(String punkteDurchschnitt) {
	    if (punkteDurchschnitt != null) {
	        this.punkteDurchschnitt = punkteDurchschnitt;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public String getNotenDurchschnitt() {
	    return notenDurchschnitt;
	}

	public void setNotenDurchschnitt(String notenDurchschnitt) {
	    if (notenDurchschnitt != null) {
	        this.notenDurchschnitt = notenDurchschnitt;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getGelbekarten() {
	    return gelbekarten;
	}

	public void setGelbekarten(Integer gelbekarten) {
	    if (gelbekarten != null) {
	        this.gelbekarten = gelbekarten;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getRotekarten() {
	    return rotekarten;
	}

	public void setRotekarten(Integer rotekarten) {
	    if (rotekarten != null) {
	        this.rotekarten = rotekarten;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getGelbrotekarten() {
	    return gelbrotekarten;
	}

	public void setGelbrotekarten(Integer gelbrotekarten) {
	    if (gelbrotekarten != null) {
	        this.gelbrotekarten = gelbrotekarten;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	// ===== Getter/Setter Comstats-Felder =====

	public Integer getEinsatzzeit() {
	    return einsatzzeit;
	}

	public void setEinsatzzeit(Integer einsatzzeit) {
	    if (einsatzzeit != null) {
	        this.einsatzzeit = einsatzzeit;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public String getStatus() {
	    return status;
	}

	public void setStatus(String status) {
	    if (status != null) {
	        this.status = status;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Double getXgoals() {
	    return xgoals;
	}

	public void setXgoals(Double xgoals) {
	    if (xgoals != null) {
	        this.xgoals = xgoals;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Double getRating() {
	    return rating;
	}

	public void setRating(Double rating) {
	    if (rating != null) {
	        this.rating = rating;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getPensSaved() {
	    return pensSaved;
	}

	public void setPensSaved(Integer pensSaved) {
	    if (pensSaved != null) {
	        this.pensSaved = pensSaved;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getPensMissed() {
	    return pensMissed;
	}

	public void setPensMissed(Integer pensMissed) {
	    if (pensMissed != null) {
	        this.pensMissed = pensMissed;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getPoints() {
	    return points;
	}

	public void setPoints(Integer points) {
	    if (points != null) {
	        this.points = points;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getSubOut() {
	    return subOut;
	}

	public void setSubOut(Integer subOut) {
	    if (subOut != null) {
	        this.subOut = subOut;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getCleanSheet() {
	    return cleanSheet;
	}

	public void setCleanSheet(Integer cleanSheet) {
	    if (cleanSheet != null) {
	        this.cleanSheet = cleanSheet;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getActive() {
	    return active;
	}

	public void setActive(Integer active) {
	    if (active != null) {
	        this.active = active;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	// ===== Getter/Setter ESPN-Felder =====

	public Integer getFoulsCommitted() {
	    return foulsCommitted;
	}

	public void setFoulsCommitted(Integer foulsCommitted) {
	    if (foulsCommitted != null) {
	        this.foulsCommitted = foulsCommitted;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getFoulsSuffered() {
	    return foulsSuffered;
	}

	public void setFoulsSuffered(Integer foulsSuffered) {
	    if (foulsSuffered != null) {
	        this.foulsSuffered = foulsSuffered;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getOwnGoals() {
	    return ownGoals;
	}

	public void setOwnGoals(Integer ownGoals) {
	    if (ownGoals != null) {
	        this.ownGoals = ownGoals;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getAppearances() {
	    return appearances;
	}

	public void setAppearances(Integer appearances) {
	    if (appearances != null) {
	        this.appearances = appearances;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getSubIns() {
	    return subIns;
	}

	public void setSubIns(Integer subIns) {
	    if (subIns != null) {
	        this.subIns = subIns;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getGoalAssists() {
	    return goalAssists;
	}

	public void setGoalAssists(Integer goalAssists) {
	    if (goalAssists != null) {
	        this.goalAssists = goalAssists;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getOffsides() {
	    return offsides;
	}

	public void setOffsides(Integer offsides) {
	    if (offsides != null) {
	        this.offsides = offsides;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getShotsOnTarget() {
	    return shotsOnTarget;
	}

	public void setShotsOnTarget(Integer shotsOnTarget) {
	    if (shotsOnTarget != null) {
	        this.shotsOnTarget = shotsOnTarget;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getTotalShots() {
	    return totalShots;
	}

	public void setTotalShots(Integer totalShots) {
	    if (totalShots != null) {
	        this.totalShots = totalShots;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getSaves() {
	    return saves;
	}

	public void setSaves(Integer saves) {
	    if (saves != null) {
	        this.saves = saves;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getShotsFaced() {
	    return shotsFaced;
	}

	public void setShotsFaced(Integer shotsFaced) {
	    if (shotsFaced != null) {
	        this.shotsFaced = shotsFaced;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public Integer getGoalsConceded() {
	    return goalsConceded;
	}

	public void setGoalsConceded(Integer goalsConceded) {
	    if (goalsConceded != null) {
	        this.goalsConceded = goalsConceded;
	    }
	    this.lastUpdate = new ComunioDate();
	}

	public ComunioDate getLastUpdate() {
	    return lastUpdate;
	}

	public void setLastUpdate(ComunioDate lastUpdate) {
	    this.lastUpdate = lastUpdate;
	}

	public Integer getSubIn() {
	    return subIn;
	}

	public void setSubIn(Integer subIn) {
	    if (subIn != null) {
	        this.subIn = subIn;
	    }
	    this.lastUpdate = new ComunioDate();
	}


	@Override
	public String toString() {
		return "Stats: Played games: " + this.playedGames + ", Gelbekarte/Gelb-Rotekarte/Rotekarte: " + this.gelbekarten + "/" + this.gelbrotekarten + "/" + this.rotekarten + ", Einsätze (gewertet): " + this.ratedGames;
	}



	
}