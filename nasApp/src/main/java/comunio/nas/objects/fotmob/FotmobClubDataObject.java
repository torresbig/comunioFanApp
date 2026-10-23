package comunio.nas.objects.fotmob;

import org.json.JSONObject;

import java.time.Instant;

public class FotmobClubDataObject {
	
	private String id;
	private String comunioId; // comunioId gibt es über die Methode und dem ClubMapper.getComunioIdFromName
	private String name; 
	private String link;  //methode wie z.b. getLink() und da return "https://www.fotmob.com/api/data/teams?id=" + this.id;
	private Instant lastUpdate; 
	
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getComunioId() {
		return comunioId;
	}

	public void setComunioId(String comunioId) {
		this.comunioId = comunioId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public JSONObject toJSON() {
	 JSONObject json = new JSONObject();
	 if (id != null) json.put("id", id);
	 if (comunioId != null) json.put("comunioId", comunioId);
	 if (name != null) json.put("name", name);
	 if (link != null) json.put("link", link);
	 if (lastUpdate != null) json.put("lastUpdate", lastUpdate.toString());
	 return json;
	}

	public static FotmobClubDataObject fromJSON(JSONObject json) {
	 if (json == null) return null;
	 FotmobClubDataObject obj = new FotmobClubDataObject();
	 obj.setId(json.getString("id"));
	 if (json.has("comunioId")) obj.setComunioId(json.getString("comunioId"));
	 if (json.has("name")) obj.setName(json.getString("name"));
	 if (json.has("link")) obj.setLink(json.getString("link"));
	 if (json.has("lastUpdate")) {
	  String ts = json.getString("lastUpdate");
	  obj.setLastUpdate(Instant.parse(ts));
	 }
	 return obj;
	}
}