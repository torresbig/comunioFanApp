package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PlayerInformation {
	private String title;
	private PlayerInfoValue value;

	public static PlayerInformation fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PlayerInformation obj = new PlayerInformation();
		obj.setTitle(FotmobPlayerDataObject.optString(json, "title"));
		if (json.has("value") && !json.isNull("value")) {
			obj.setValue(PlayerInfoValue.fromJSON(json.getJSONObject("value")));
		}
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (title != null)
			json.put("title", title);
		if (value != null)
			json.put("value", value.toJSON());
		return json;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public PlayerInfoValue getValue() {
		return value;
	}

	public void setValue(PlayerInfoValue value) {
		this.value = value;
	}

}