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
	private List<PositionDetail> positions;
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
			JSONObject positionD = json.getJSONObject("positionDescription");
			if (positionD.has("positions") && !positionD.isNull("positions")) {
				JSONArray arr = positionD.getJSONArray("positions");
				List<PositionDetail> list = new ArrayList<>();
				for (int i = 0; i < arr.length(); i++) {
					if (!arr.isNull(i))
						list.add(PositionDetail.fromJSON(arr.getJSONObject(i)));
				}
				player.setPositions(list);
			}
		}

		if (json.has("positions") && !json.isNull("poistions")) {
			JSONArray arr = json.getJSONArray("positions");
			List<PositionDetail> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i)) {
					list.add(PositionDetail.fromJSON(arr.getJSONObject(i)));
				}
			}
			player.setPositions(list);
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

		if (positions != null) {
			JSONArray arr = new JSONArray();
			for (PositionDetail pd : positions) {
				if (pd != null)
					arr.put(pd.toJSON());
			}
			json.put("positions", arr);
		}
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
		if (positions == null ) {
			return null;
		}
		return positions.stream().filter(p -> Boolean.TRUE.equals(p.getIsMainPosition())).findFirst();
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

	public List<PlayerInformation> getPlayerInformation() {
		return playerInformation;
	}

	public void setPlayerInformation(List<PlayerInformation> playerInformation) {
		this.playerInformation = playerInformation;
	}

	public List<PositionDetail> getPositions() {
		return positions;
	}

	public void setPositions(List<PositionDetail> positions) {
		this.positions = positions;
	}

}
