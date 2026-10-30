package comunio.nas.dataScraper.comunio;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserLineupData {
	public String tactic;
	public List<LineupPlayerData> players;

	public UserLineupData() {
		this.players = new ArrayList<>();
	}
	
	public UserLineupData(String tactic, List<LineupPlayerData> players) {
		this.tactic = tactic;
		this.players = players;
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("tactic", this.tactic);
		JSONArray arr = new JSONArray();
		for (LineupPlayerData player : this.players) {
			arr.put(player.toJson());
		}
		json.put("players", arr);
		return json;
	}
	
	public static UserLineupData fromJSON(JSONObject json) {
		UserLineupData data = new UserLineupData();
		data.tactic = json.optString("tactic");
		data.players = LineupPlayerData.fromJSONArray(json.optJSONArray("players"));
		return data;
	}
}