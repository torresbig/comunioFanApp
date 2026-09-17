package comunio.nas.objects.fotmob;

import org.json.JSONObject;

public class PitchPositionData {
	private Double right;
	private Double top;
	private Double ratio;

	public static PitchPositionData fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PitchPositionData obj = new PitchPositionData();
		obj.setRight(FotmobPlayerDataObject.optDouble(json, "right"));
		obj.setTop(FotmobPlayerDataObject.optDouble(json, "top"));
		obj.setRatio(FotmobPlayerDataObject.optDouble(json, "ratio"));
		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (right != null)
			json.put("right", right);
		if (top != null)
			json.put("top", top);
		if (ratio != null)
			json.put("ratio", ratio);
		return json;
	}

	public Double getRight() {
		return right;
	}

	public void setRight(Double right) {
		this.right = right;
	}

	public Double getTop() {
		return top;
	}

	public void setTop(Double top) {
		this.top = top;
	}

	public Double getRatio() {
		return ratio;
	}

	public void setRatio(Double ratio) {
		this.ratio = ratio;
	}
}