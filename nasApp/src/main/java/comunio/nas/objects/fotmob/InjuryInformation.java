package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class InjuryInformation {
	private String name;
	private String expectedReturn;
	private String lastUpdated;

	public static InjuryInformation fromJSON(JSONObject json) {
		if (json == null) {
			return null;
		}
		InjuryInformation obj = new InjuryInformation();
		obj.setName(FotmobPlayerDataObject.optString(json, "name"));

		if (json.has("expectedReturn") && !json.isNull("expectedReturn")) {
			obj.setExpectedReturn(json.get("expectedReturn"));
		}
		if (json.has("lastUpdated") && !json.isNull("lastUpdated")) {
			obj.setLastUpdated(json.get("lastUpdated"));
		}
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (name != null)
			json.put("name", name);
		if (expectedReturn != null)
			json.put("expectedReturn", expectedReturn);
		if (lastUpdated != null)
			json.put("lastUpdated", lastUpdated);
		return json;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExpectedReturn() {
		return expectedReturn;
	}

	public void setExpectedReturn(Object expectedReturn) {
		if (expectedReturn != null) {
			if (expectedReturn instanceof JSONObject) {
				JSONObject expR = (JSONObject) expectedReturn;

				this.expectedReturn = expR.has("expectedReturnDateParam") && !expR.isNull("expectedReturnDateParam") ? expR.optString("expectedReturnDateParam") : null;
				if (this.expectedReturn == null) {
					this.expectedReturn = expR.has("expectedReturnFallback") && !expR.isNull("expectedReturnFallback") ? expR.optString("expectedReturnFallback") : "unbekannt";
				}

			} else if (expectedReturn instanceof String) {
				this.expectedReturn = (String) expectedReturn;
			} else {
				System.err.println("Erwartete Rückkehr (expectedReturn) ist kein String oder JSONObject: " + expectedReturn.toString());
				this.expectedReturn = "unbekannt";
			}
		}

	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Object lastUpd) {
		if (lastUpd != null) {
			if (lastUpd instanceof JSONObject) {
				this.lastUpdated = ((JSONObject) lastUpd).optString("utcTime");
			} else if (lastUpd instanceof String) {
				this.lastUpdated = (String) lastUpd;
			} else {
				System.err.println("LastUpdated ist kein String oder JSONObject: " + lastUpd.toString());
				this.lastUpdated = null;
			}
		}
	}
}