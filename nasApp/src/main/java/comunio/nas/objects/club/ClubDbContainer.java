package comunio.nas.objects.club;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClubDbContainer {

	private Map<String, ClubObject> clubDb;

	private Instant lastUpdate;

	public static ClubDbContainer fromJSON(JSONArray json) {
		ClubDbContainer result = new ClubDbContainer();
		result.clubDb = new HashMap<>();
		if (json != null && !json.isEmpty()) {
			for (Object club : json) {
				JSONObject clubObj = (JSONObject) club;
				ClubObject clubObject = ClubObject.fromJSON(clubObj);
				result.clubDb.put(clubObject.getId(), clubObject);
			}
		}
		return result;
	}

	/**
	 * Serialisiert diesen Container in ein JSONObject.
	 *
	 * @return JSONObject mit clubDb und lastUpdate
	 */
	public JSONArray toJSON() {
		JSONArray jsonClub = new JSONArray();
		if (clubDb != null && !clubDb.isEmpty()) {

			for (Map.Entry<String, ClubObject> entry : clubDb.entrySet()) {
				jsonClub.put(entry.getValue().toJSON());
			}
		}
		return jsonClub;
	}

	public Map<String, ClubObject> getClubDb() {
		return clubDb;
	}

	public void setClubDb(Map<String, ClubObject> clubDb) {
		this.clubDb = clubDb;
	}

}
