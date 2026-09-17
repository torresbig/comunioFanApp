package comunio.nas.objects.fotmob;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class PositionDescription {
	private List<PositionDetail> positions;
	private PositionKey primaryPosition;
	private List<PositionKey> nonPrimaryPositions;

	public static PositionDescription fromJSON(JSONObject json) {
		if (json == null)
			return null;
		PositionDescription obj = new PositionDescription();

		if (json.has("positions") && !json.isNull("positions")) {
			JSONArray arr = json.getJSONArray("positions");
			List<PositionDetail> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i))
					list.add(PositionDetail.fromJSON(arr.getJSONObject(i)));
			}
			obj.setPositions(list);
		}

		if (json.has("primaryPosition") && !json.isNull("primaryPosition")) {
			obj.setPrimaryPosition(PositionKey.fromJSON(json.getJSONObject("primaryPosition")));
		}

		if (json.has("nonPrimaryPositions") && !json.isNull("nonPrimaryPositions")) {
			JSONArray arr = json.getJSONArray("nonPrimaryPositions");
			List<PositionKey> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i))
					list.add(PositionKey.fromJSON(arr.getJSONObject(i)));
			}
			obj.setNonPrimaryPositions(list);
		}

		return obj;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (positions != null) {
			JSONArray arr = new JSONArray();
			for (PositionDetail pos : positions)
				if (pos != null)
					arr.put(pos.toJSON());
			json.put("positions", arr);
		}
		if (primaryPosition != null)
			json.put("primaryPosition", primaryPosition.toJSON());
		if (nonPrimaryPositions != null) {
			JSONArray arr = new JSONArray();
			for (PositionKey pk : nonPrimaryPositions)
				if (pk != null)
					arr.put(pk.toJSON());
			json.put("nonPrimaryPositions", arr);
		}
		return json;
	}

	public List<PositionDetail> getPositions() {
		return positions;
	}

	public void setPositions(List<PositionDetail> positions) {
		this.positions = positions;
	}

	public PositionKey getPrimaryPosition() {
		return primaryPosition;
	}

	public void setPrimaryPosition(PositionKey primaryPosition) {
		this.primaryPosition = primaryPosition;
	}

	public List<PositionKey> getNonPrimaryPositions() {
		return nonPrimaryPositions;
	}

	public void setNonPrimaryPositions(List<PositionKey> nonPrimaryPositions) {
		this.nonPrimaryPositions = nonPrimaryPositions;
	}
}