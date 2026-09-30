package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PositionDetail {
	private String strPos;
	private String strPosShort;
	private Integer occurences;
	private Boolean isMainPosition;

	public static PositionDetail fromJSON(JSONObject json) {
		if (json == null) {
			return null;
		}
		PositionDetail obj = new PositionDetail();

		if (json.has("strPos") && !json.isNull("strPos")) {
			obj.setStrPos(json.get("strPos"));
		}
		if (json.has("strPosShort") && !json.isNull("strPosShort")) {
			obj.setStrPosShort(json.get("strPosShort"));
		}
		obj.setOccurences(FotmobPlayerDataObject.optInteger(json, "occurences"));
		obj.setIsMainPosition(FotmobPlayerDataObject.optBoolean(json, "isMainPosition"));
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (strPos != null)
			json.put("strPos", strPos);
		if (strPosShort != null)
			json.put("strPosShort", strPosShort);
		if (occurences != null)
			json.put("occurences", occurences);
		if (isMainPosition != null)
			json.put("isMainPosition", isMainPosition);
		return json;
	}

	public String getStrPos() {
		return strPos;
	}

	public void setStrPos(Object strPos) {
		if (strPos != null) {
			if (strPos instanceof JSONObject) {
				JSONObject strP = (JSONObject) strPos;
				this.strPos = strP.has("label") && !strP.isNull("label") ? strP.getString("label") : "unbekannt";
			} else if (strPos instanceof String) {
				this.strPos = (String) strPos;
			} else {
				System.err.println("Short Position (strPosShort) ist kein String oder JSONObject: " + strPos.toString());
				this.strPos = "unbekannt";
			}
		}
	}

	public String getStrPosShort() {
		return strPosShort;
	}

	public void setStrPosShort(Object strPosShort) {
		if (strPosShort != null) {
			if (strPosShort instanceof JSONObject) {
				JSONObject strPS = (JSONObject) strPosShort;
				this.strPosShort = strPS.has("label") && !strPS.isNull("label") ? strPS.getString("label") : "";
			} else if (strPosShort instanceof String) {
				this.strPosShort = (String) strPosShort;
			} else {
				System.err.println("Short Position (strPosShort) ist kein String oder JSONObject: " + strPosShort.toString());
				this.strPosShort = "unbekannt";
			}
		}
	}

	public Integer getOccurences() {
		return occurences;
	}

	public void setOccurences(Integer occurences) {
		this.occurences = occurences;
	}

	public Boolean getIsMainPosition() {
		return isMainPosition;
	}

	public void setIsMainPosition(Boolean mainPosition) {
		isMainPosition = mainPosition;
	}

}