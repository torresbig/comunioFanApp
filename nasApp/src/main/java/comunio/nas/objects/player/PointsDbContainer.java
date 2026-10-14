package comunio.nas.objects.player;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class PointsDbContainer {

	private Integer lastProcessedMatchday;
	private Map<String, Map<Integer, Spielerstats>> pointsMap;

	public PointsDbContainer() {
		this.setPointsMap(new HashMap<>());
	}

	public PointsDbContainer(Integer lastProcessedMatchday, Map<String, Map<Integer, Spielerstats>> pointsMap) {
		this.setLastProcessedMatchday(lastProcessedMatchday);
		this.setPointsMap(pointsMap);
	}

	public Integer getLastProcessedMatchday() {
		return lastProcessedMatchday;
	}

	public void setLastProcessedMatchday(Integer lastProcessedMatchday) {
		this.lastProcessedMatchday = lastProcessedMatchday;
	}

	public Map<String, Map<Integer, Spielerstats>> getPointsMap() {
		return pointsMap;
	}

	public void setPointsMap(Map<String, Map<Integer, Spielerstats>> pointsMap) {
		this.pointsMap = pointsMap;
	}

	public void addPlayerStats(String playerId, Integer matchday, Spielerstats stats) {
		if (this.pointsMap == null) {
			this.pointsMap = new HashMap<>();
		}
		if (!pointsMap.containsKey(playerId)) {
			Map<Integer, Spielerstats> matchdaystats = new HashMap<>();
			matchdaystats.put(matchday, stats);
			pointsMap.put(playerId, matchdaystats);
		} else {
			Map<Integer, Spielerstats> matchdaystats = pointsMap.get(playerId);
			Spielerstats statsDb = matchdaystats.get(matchday);
			if (statsDb == null) {
				statsDb = stats;
			} else {
				statsDb.mergeSpielerstats(stats);
			}
			matchdaystats.put(matchday, statsDb);
		}

		if (this.lastProcessedMatchday < matchday) {
			this.lastProcessedMatchday = matchday;
		}
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("lastProcessedMatchday", this.lastProcessedMatchday);

		JSONObject pointsMapJson = new JSONObject();
		for (Map.Entry<String, Map<Integer, Spielerstats>> playerEntry : pointsMap.entrySet()) {
			String playerId = playerEntry.getKey();
			Map<Integer, Spielerstats> matchdayStats = playerEntry.getValue();

			JSONObject matchdayStatsJson = new JSONObject();
			for (Map.Entry<Integer, Spielerstats> matchdayEntry : matchdayStats.entrySet()) {
				Integer matchday = matchdayEntry.getKey();
				Spielerstats stats = matchdayEntry.getValue();
				matchdayStatsJson.put(matchday.toString(), stats.toJSON());
			}

			pointsMapJson.put(playerId, matchdayStatsJson);
		}

		json.put("pointsMap", pointsMapJson);
		return json;

	}

	public static PointsDbContainer fromJson(JSONObject json) {
		Integer lastProcessedMatchday = json.optInt("lastProcessedMatchday", 0);
		Map<String, Map<Integer, Spielerstats>> pointsMap = new HashMap<>();

		JSONObject pointsMapJson = json.optJSONObject("pointsMap");
		if (pointsMapJson != null) {
			for (String playerId : pointsMapJson.keySet()) {
				JSONObject matchdayStatsJson = pointsMapJson.optJSONObject(playerId);
				if (matchdayStatsJson != null) {
					Map<Integer, Spielerstats> matchdayStats = new HashMap<>();
					for (String matchdayStr : matchdayStatsJson.keySet()) {
						Integer matchday = Integer.parseInt(matchdayStr);
						JSONObject statsJson = matchdayStatsJson.optJSONObject(matchdayStr);
						if (statsJson != null) {
							Spielerstats stats = Spielerstats.fromJSON(statsJson);
							matchdayStats.put(matchday, stats);
						}
					}
					pointsMap.put(playerId, matchdayStats);
				}
			}
		}

		return new PointsDbContainer(lastProcessedMatchday, pointsMap);
	}

	public void setNewSeason() {
		this.pointsMap = new HashMap<>();
		this.lastProcessedMatchday = 0;
	}
}
