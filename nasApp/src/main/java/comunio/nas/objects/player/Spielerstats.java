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
