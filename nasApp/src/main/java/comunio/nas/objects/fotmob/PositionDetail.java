package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PositionDetail {
	private PositionKey strPos;
	private PositionKey strPosShort;
	private Integer occurences;
	private String position;
	private Boolean isMainPosition;
	private PitchPositionData pitchPositionData;

	public static PositionDetail fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PositionDetail obj = new PositionDetail();

		if (json.has("strPos") && !json.isNull("strPos"))
			obj.setStrPos(PositionKey.fromJSON(json.getJSONObject("strPos")));
		if (json.has("strPosShort") && !json.isNull("strPosShort"))
			obj.setStrPosShort(PositionKey.fromJSON(json.getJSONObject("strPosShort")));

		obj.setOccurences(FotmobPlayerDataObject.optInteger(json, "occurences"));
		obj.setPosition(FotmobPlayerDataObject.optString(json, "position"));
		obj.setIsMainPosition(FotmobPlayerDataObject.optBoolean(json, "isMainPosition"));

		if (json.has("pitchPositionData") && !json.isNull("pitchPositionData")) {
			obj.setPitchPositionData(PitchPositionData.fromJSON(json.getJSONObject("pitchPositionData")));
		}

		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (strPos != null)
			json.put("strPos", strPos.toJSON());
		if (strPosShort != null)
			json.put("strPosShort", strPosShort.toJSON());
		if (occurences != null)
			json.put("occurences", occurences);
		if (position != null)
			json.put("position", position);
		if (isMainPosition != null)
			json.put("isMainPosition", isMainPosition);
		if (pitchPositionData != null)
			json.put("pitchPositionData", pitchPositionData.toJSON());
		return json;
	}

	public PositionKey getStrPos() {
		return strPos;
	}

	public void setStrPos(PositionKey strPos) {
		this.strPos = strPos;
	}

	public PositionKey getStrPosShort() {
		return strPosShort;
	}

	public void setStrPosShort(PositionKey strPosShort) {
		this.strPosShort = strPosShort;
	}

	public Integer getOccurences() {
		return occurences;
	}

	public void setOccurences(Integer occurences) {
		this.occurences = occurences;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Boolean getIsMainPosition() {
		return isMainPosition;
	}

	public void setIsMainPosition(Boolean mainPosition) {
		isMainPosition = mainPosition;
	}

	public PitchPositionData getPitchPositionData() {
		return pitchPositionData;
	}

	public void setPitchPositionData(PitchPositionData pitchPositionData) {
		this.pitchPositionData = pitchPositionData;
	}
}