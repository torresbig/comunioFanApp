package comunio.nas.dataScraper.comunio;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class LineupPlayerData {
	public long id;
	public String name;
	public int kaderPos;
	public boolean wasLiveSubstituted;
	public long quotedprice;
	public boolean substitute;
	public String position;

	public LineupPlayerData() {
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("id", this.id);
		json.put("name", this.name);
		json.put("kaderPos", this.kaderPos);
		json.put("wasLiveSubstituted", this.wasLiveSubstituted);
		json.put("quotedprice", this.quotedprice);
		json.put("substitute", this.substitute);
		json.put("position", this.position);
		return json;
	}

	public static List<LineupPlayerData> fromJSONArray(JSONArray optJSONArray) {
		try {
			List<LineupPlayerData> players = new java.util.ArrayList<>();
			if (optJSONArray != null) {
				for (int i = 0; i < optJSONArray.length(); i++) {
					JSONObject playerJson = optJSONArray.getJSONObject(i);
					LineupPlayerData player = new LineupPlayerData();
					player.id = playerJson.optLong("id", 0L);
					player.name = playerJson.optString("name", "");
					player.kaderPos = playerJson.optInt("kaderPos", 0);
					player.wasLiveSubstituted = playerJson.optBoolean("wasLiveSubstituted", false);
					player.quotedprice = playerJson.optLong("quotedprice", 0L);
					player.substitute = playerJson.optBoolean("substitute", false);
					player.position = playerJson.optString("position", "unbekannt");
					players.add(player);
				}
			}
			return players;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}