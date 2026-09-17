package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PlayerInfoValue {
	private Integer numberValue;
	private String key;
	private Object fallback; // Nimmt Strings oder verschachtelte Objekte auf (z.B. Contract End Date)
	private String dateValue;
	private JSONObject options;

	public static PlayerInfoValue fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PlayerInfoValue obj = new PlayerInfoValue();
		obj.setNumberValue(FotmobPlayerDataObject.optInteger(json, "numberValue"));
		obj.setKey(FotmobPlayerDataObject.optString(json, "key"));
		obj.setDateValue(FotmobPlayerDataObject.optString(json, "dateValue"));

		if (json.has("fallback") && !json.isNull("fallback")) {
			obj.setFallback(json.get("fallback"));
		}

		if (json.has("options") && !json.isNull("options")) {
			obj.setOptions(json.getJSONObject("options"));
		}

		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (numberValue != null)
			json.put("numberValue", numberValue);
		if (key != null)
			json.put("key", key);
		if (fallback != null)
			json.put("fallback", fallback);
		if (dateValue != null)
			json.put("dateValue", dateValue);
		if (options != null)
			json.put("options", options);
		return json;
	}

	public Integer getNumberValue() {
		return numberValue;
	}

	public void setNumberValue(Integer numberValue) {
		this.numberValue = numberValue;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getFallback() {
		return fallback;
	}

	public void setFallback(Object fallback) {
		this.fallback = fallback;
	}

	public String getDateValue() {
		return dateValue;
	}

	public void setDateValue(String dateValue) {
		this.dateValue = dateValue;
	}

	public JSONObject getOptions() {
		return options;
	}

	public void setOptions(JSONObject options) {
		this.options = options;
	}
}