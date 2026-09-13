package comunio.nas.objects.espn;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class EspnClubContainer {

	private Map<String, EspnClubMapObject> clubMap;
	private int mapped;

	public EspnClubContainer(Map<String, EspnClubMapObject> clubMap) {
		this.clubMap = clubMap;
		this.mapped = clubMap.size();
	}

	public Map<String, EspnClubMapObject> getClubMap() {
		return clubMap;
	}

	public void setClubMap(Map<String, EspnClubMapObject> clubMap) {
		this.clubMap = clubMap;
		this.setMapped(clubMap.size());
	}

	public int getMapped() {
		return mapped;
	}

	private void setMapped(int mapped) {
		this.mapped = mapped;
	}

	/**
	 * Konvertiert das EspnClubContainer-Objekt in ein JSON-Objekt.
	 *
	 * @return Ein JSONObject, das die Daten des EspnClubContainer-Objekts enthält.
	 */
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONArray clubMapJson = new JSONArray();
		for (Map.Entry<String, EspnClubMapObject> entry : this.clubMap.entrySet()) {
			EspnClubMapObject club = entry.getValue();
			clubMapJson.put(club.toJson());
		}
		json.put("mapping", clubMapJson);
		json.put("mapped", this.mapped);
		return json;
	}

	/**
	 * Erstellt ein EspnClubContainer-Objekt aus einem JSON-Objekt.
	 *
	 * @param json Das JSON-Objekt, das die Daten enthält.
	 * @return Ein EspnClubContainer-Objekt, das die Daten aus dem JSON-Objekt
	 *         enthält.
	 */
	public static EspnClubContainer fromJson(JSONObject json) {
		Map<String, EspnClubMapObject> clubMap = new java.util.HashMap<>();
		JSONArray clubMapJson = json.getJSONArray("mapping");
		for (int i = 0; i < clubMapJson.length(); i++) {
			JSONObject clubJson = clubMapJson.getJSONObject(i);
			EspnClubMapObject club = new EspnClubMapObject(clubJson.optString("espnName", ""), clubJson.optString("espnId", ""), clubJson.optString("comunioName", ""), clubJson.optString("comunioId", ""));
			clubMap.put(club.getEspnId(), club);
		}
		return new EspnClubContainer(clubMap);
	}
}
