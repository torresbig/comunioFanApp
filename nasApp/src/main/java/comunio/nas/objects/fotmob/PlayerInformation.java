package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PlayerInformation {
	private String title;
	private String translationKey;
	private String countryCode;
	private PlayerInfoValue value;

	public static PlayerInformation fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PlayerInformation obj = new PlayerInformation();
		obj.setTitle(FotmobPlayerDataObject.optString(json, "title"));
		obj.setTranslationKey(FotmobPlayerDataObject.optString(json, "translationKey"));
		obj.setCountryCode(FotmobPlayerDataObject.optString(json, "countryCode"));

		if (json.has("value") && !json.isNull("value")) {
			obj.setValue(PlayerInfoValue.fromJSON(json.getJSONObject("value")));
		}

		

		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (title != null)
			json.put("title", title);
		if (translationKey != null)
			json.put("translationKey", translationKey);
		if (countryCode != null)
			json.put("countryCode", countryCode);
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

	public String getTranslationKey() {
		return translationKey;
	}

	public void setTranslationKey(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public PlayerInfoValue getValue() {
		return value;
	}

	public void setValue(PlayerInfoValue value) {
		this.value = value;
	}

}