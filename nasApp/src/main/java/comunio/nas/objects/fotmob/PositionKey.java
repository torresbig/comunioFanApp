package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PositionKey {
	private String label;
	private String key;

	public static PositionKey fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PositionKey obj = new PositionKey();
		obj.setLabel(FotmobPlayerDataObject.optString(json, "label"));
		obj.setKey(FotmobPlayerDataObject.optString(json, "key"));
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (label != null)
			json.put("label", label);
		if (key != null)
			json.put("key", key);
		return json;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}