package comunio.nas.objects.fotmob;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FotmobPlayerDataObject {

	private Integer id;
	private String name;
	private Boolean isCaptain;
	private String status;
	private InjuryInformation injuryInformation;
	private PositionDescription positionDescription;
	private List<PlayerInformation> playerInformation;

	public FotmobPlayerDataObject() {
	}

	// --- Helper for Null-Safe JSON Extraction ---
	static String optString(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optString(key) : null;
	}

	static Integer optInteger(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optInt(key) : null;
	}

	static Double optDouble(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optDouble(key) : null;
	}

	static Boolean optBoolean(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optBoolean(key) : null;
	}

	// --- From JSON ---
	public static FotmobPlayerDataObject fromJSON(JSONObject json) {
		if (json == null)
			return null;

		FotmobPlayerDataObject player = new FotmobPlayerDataObject();
		player.setId(optInteger(json, "id"));
		player.setName(optString(json, "name"));
		player.setIsCaptain(optBoolean(json, "isCaptain"));
		player.setStatus(optString(json, "status"));

		if (json.has("injuryInformation") && !json.isNull("injuryInformation")) {
			player.setInjuryInformation(InjuryInformation.fromJSON(json.getJSONObject("injuryInformation")));
		}

		if (json.has("positionDescription") && !json.isNull("positionDescription")) {
			player.setPositionDescription(PositionDescription.fromJSON(json.getJSONObject("positionDescription")));
		}

		if (json.has("playerInformation") && !json.isNull("playerInformation")) {
			JSONArray arr = json.getJSONArray("playerInformation");
			List<PlayerInformation> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i)) {
					list.add(PlayerInformation.fromJSON(arr.getJSONObject(i)));
				}
			}
			player.setPlayerInformation(list);
		}

		return player;
	}

	// --- To JSON ---
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (id != null)
			json.put("id", id);
		if (name != null)
			json.put("name", name);
		if (isCaptain != null)
			json.put("isCaptain", isCaptain);
		if (status != null)
			json.put("status", status);
		if (injuryInformation != null)
			json.put("injuryInformation", injuryInformation.toJSON());
		if (positionDescription != null)
			json.put("positionDescription", positionDescription.toJSON());

		if (playerInformation != null) {
			JSONArray arr = new JSONArray();
			for (PlayerInformation info : playerInformation) {
				if (info != null)
					arr.put(info.toJSON());
			}
			json.put("playerInformation", arr);
		}

		return json;
	}

	// --- Sinnvolle Utility-Methoden ---

	public boolean isInjured() {
		return injuryInformation != null && injuryInformation.getName() != null;
	}

	public Optional<PositionDetail> getMainPositionDetail() {
		if (positionDescription == null || positionDescription.getPositions() == null) {
			return Optional.empty();
		}
		return positionDescription.getPositions().stream().filter(p -> Boolean.TRUE.equals(p.getIsMainPosition())).findFirst();
	}

	public Optional<PlayerInformation> getInfoByTitle(String title) {
		if (playerInformation == null)
			return Optional.empty();
		return playerInformation.stream().filter(info -> info.getTitle() != null && info.getTitle().equalsIgnoreCase(title)).findFirst();
	}

	// --- Getters & Setters ---
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsCaptain() {
		return isCaptain;
	}

	public void setIsCaptain(Boolean captain) {
		isCaptain = captain;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public InjuryInformation getInjuryInformation() {
		return injuryInformation;
	}

	public void setInjuryInformation(InjuryInformation injuryInformation) {
		this.injuryInformation = injuryInformation;
	}

	public PositionDescription getPositionDescription() {
		return positionDescription;
	}

	public void setPositionDescription(PositionDescription positionDescription) {
		this.positionDescription = positionDescription;
	}

	public List<PlayerInformation> getPlayerInformation() {
		return playerInformation;
	}

	public void setPlayerInformation(List<PlayerInformation> playerInformation) {
		this.playerInformation = playerInformation;
	}

	// =========================================================================
	// Nested Classes
	// =========================================================================

	public static class ExpectedReturn {
		private String expectedReturnKey;
		private String expectedReturnDateParam;
		private String expectedReturnFallback;

		public static ExpectedReturn fromJSON(JSONObject json) {
			if (json == null)
				return null;
			ExpectedReturn obj = new ExpectedReturn();
			obj.setExpectedReturnKey(optString(json, "expectedReturnKey"));
			obj.setExpectedReturnDateParam(optString(json, "expectedReturnDateParam"));
			obj.setExpectedReturnFallback(optString(json, "expectedReturnFallback"));
			return obj;
		}

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			if (expectedReturnKey != null)
				json.put("expectedReturnKey", expectedReturnKey);
			if (expectedReturnDateParam != null)
				json.put("expectedReturnDateParam", expectedReturnDateParam);
			if (expectedReturnFallback != null)
				json.put("expectedReturnFallback", expectedReturnFallback);
			return json;
		}

		public String getExpectedReturnKey() {
			return expectedReturnKey;
		}

		public void setExpectedReturnKey(String expectedReturnKey) {
			this.expectedReturnKey = expectedReturnKey;
		}

		public String getExpectedReturnDateParam() {
			return expectedReturnDateParam;
		}

		public void setExpectedReturnDateParam(String expectedReturnDateParam) {
			this.expectedReturnDateParam = expectedReturnDateParam;
		}

		public String getExpectedReturnFallback() {
			return expectedReturnFallback;
		}

		public void setExpectedReturnFallback(String expectedReturnFallback) {
			this.expectedReturnFallback = expectedReturnFallback;
		}
	}

	public static class LastUpdated {
		private String utcTime;
		private String timezone;

		public static LastUpdated fromJSON(JSONObject json) {
			if (json == null)
				return null;
			LastUpdated obj = new LastUpdated();
			obj.setUtcTime(optString(json, "utcTime"));
			obj.setTimezone(optString(json, "timezone"));
			return obj;
		}

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			if (utcTime != null)
				json.put("utcTime", utcTime);
			if (timezone != null)
				json.put("timezone", timezone);
			return json;
		}

		public String getUtcTime() {
			return utcTime;
		}

		public void setUtcTime(String utcTime) {
			this.utcTime = utcTime;
		}

		public String getTimezone() {
			return timezone;
		}

		public void setTimezone(String timezone) {
			this.timezone = timezone;
		}
	}


}
