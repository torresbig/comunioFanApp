package comunio.nas.objects.fotmob;

import org.json.JSONObject;

import comunio.nas.objects.fotmob.FotmobPlayerDataObject.ExpectedReturn;
import comunio.nas.objects.fotmob.FotmobPlayerDataObject.LastUpdated;

public class InjuryInformation {
	private String name;
	private ExpectedReturn expectedReturn;
	private LastUpdated lastUpdated;

	public static InjuryInformation fromJSON(JSONObject json) {
		if (json == null)
			return null;
		InjuryInformation obj = new InjuryInformation();
		obj.setName(FotmobPlayerDataObject.optString(json, "name"));

		if (json.has("expectedReturn") && !json.isNull("expectedReturn")) {
			obj.setExpectedReturn(ExpectedReturn.fromJSON(json.getJSONObject("expectedReturn")));
		}
		if (json.has("lastUpdated") && !json.isNull("lastUpdated")) {
			obj.setLastUpdated(LastUpdated.fromJSON(json.getJSONObject("lastUpdated")));
		}
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (name != null)
			json.put("name", name);
		if (expectedReturn != null)
			json.put("expectedReturn", expectedReturn.toJSON());
		if (lastUpdated != null)
			json.put("lastUpdated", lastUpdated.toJSON());
		return json;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExpectedReturn getExpectedReturn() {
		return expectedReturn;
	}

	public void setExpectedReturn(ExpectedReturn expectedReturn) {
		this.expectedReturn = expectedReturn;
	}

	public LastUpdated getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LastUpdated lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}