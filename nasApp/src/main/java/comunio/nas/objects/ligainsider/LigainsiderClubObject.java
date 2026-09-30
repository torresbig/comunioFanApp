package comunio.nas.objects.ligainsider;

import org.json.JSONObject;

public class LigainsiderClubObject {

	private String name;
	private String id;
	private String link;

	public LigainsiderClubObject(String name, String id, String link) {
		this.setName(name);
		this.setId(id);
		this.setLink(link);
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

	public static LigainsiderClubObject fromJSON(JSONObject json) {
		return new LigainsiderClubObject(json.getString("name"), json.getString("id"), json.getString("link"));
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("id", id);
		json.put("link", link);
		return json;
	}

}
