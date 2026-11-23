package comunio.nas.objects.wettbasis;

import org.json.JSONObject;

public class WettbasisClubObject {

	private String name;
	private String id;
	private String link;

	public WettbasisClubObject() {
	}

	public WettbasisClubObject(String name, String id, String link) {
		this.name = name;
		this.id = id;
		this.link = link;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Erstellt ein Wettbasis-Clubobjekt aus JSON.
	 */
	public static WettbasisClubObject fromJSON(JSONObject json) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		return new WettbasisClubObject(json.optString("name"), json.optString("id"), json.optString("link"));
	}

	/**
	 * Serialisiert dieses Wettbasis-Clubobjekt in JSON.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("id", id);
		json.put("link", link);
		return json;
	}
}
