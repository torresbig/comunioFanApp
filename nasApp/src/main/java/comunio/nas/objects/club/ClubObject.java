package comunio.nas.objects.club;

import org.json.JSONObject;

import comunio.nas.objects.ligainsider.LigainsiderClubObject;

public class ClubObject {

	/**
	 * { "ligainsider": { "name": "Hamburger SV", "link":
	 * "https://www.ligainsider.de/hamburger-sv/9/", "id": "9" }, "inLiga": true,
	 * "name": "Hamburger SV", "id": "4", "transfermarktDoDe": { "name": "Hamburger
	 * SV", "link":
	 * "https://www.transfermarkt.de/hamburger-sv/startseite/verein/41", "id": 41 }
	 * }
	 */

	private String name;
	private String id;
	private boolean inLiga;
	private LigainsiderClubObject ligainsiderClub;
	private JSONObject transfermarktClub;

	public ClubObject() {

	}

	public ClubObject(String name, String id, boolean inLiga, LigainsiderClubObject ligainsiderClub, JSONObject transfermarktClub) {
		this.setName(name);
		this.setId(id);
		this.setInLiga(inLiga);
		this.setLigainsiderClub(ligainsiderClub);
		this.setTransfermarktClub(transfermarktClub);
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

	public boolean isInLiga() {
		return inLiga;
	}

	public void setInLiga(boolean inLiga) {
		this.inLiga = inLiga;
	}

	public LigainsiderClubObject getLigainsiderClub() {
		return ligainsiderClub;
	}

	public void setLigainsiderClub(LigainsiderClubObject ligainsiderClub) {
		this.ligainsiderClub = ligainsiderClub;
	}

	public JSONObject getTransfermarktClub() {
		return transfermarktClub;
	}

	public void setTransfermarktClub(JSONObject transfermarktClub) {
		this.transfermarktClub = transfermarktClub;
	}

	public static ClubObject fromJSON(JSONObject json) {
		ClubObject result = new ClubObject();
		if (json != null && !json.isEmpty()) {
			if (json.has("id")) {
				result.setId(json.getString("id"));
			}
			if (json.has("name")) {
				result.setName(json.getString("name"));
			}
			if (json.has("inLiga")) {
				result.setInLiga(json.getBoolean("inLiga"));
			}
			if (json.has("ligainsider")) {
				result.setLigainsiderClub(LigainsiderClubObject.fromJSON(json.getJSONObject("ligainsider")));
			}
			if (json.has("transfermarktClub")) {
				result.setTransfermarktClub(json.getJSONObject("transfermarktClub"));
			}
		}
		return result;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("inLiga", inLiga);
		if (ligainsiderClub != null) {
			json.put("ligainsider", ligainsiderClub.toJSON());
		}
		if (transfermarktClub != null) {
			json.put("transfermarktClub", transfermarktClub);
		}
		return json;
	}
	
	public String toString() {
		return this.name + ", " + this.id + ", " + this.inLiga;
	}

}
