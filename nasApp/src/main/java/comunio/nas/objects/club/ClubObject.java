package comunio.nas.objects.club;

import org.json.JSONObject;

import comunio.nas.objects.ligainsider.LigainsiderClubObject;
import comunio.nas.objects.wettbasis.WettbasisClubObject;

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
	/** Optionales Mapping zu Wettbasis (Name/ID/Link). */
	private WettbasisClubObject wettbasisClub;
	private JSONObject transfermarktClub;

	public ClubObject() {

	}

	public ClubObject(String name, String id, boolean inLiga, LigainsiderClubObject ligainsiderClub, JSONObject transfermarktClub) {
		this(name, id, inLiga, ligainsiderClub, null, transfermarktClub);
	}

	public ClubObject(String name, String id, boolean inLiga, LigainsiderClubObject ligainsiderClub, WettbasisClubObject wettbasisClub, JSONObject transfermarktClub) {
		this.setName(name);
		this.setId(id);
		this.setInLiga(inLiga);
		this.setLigainsiderClub(ligainsiderClub);
		this.setWettbasisClub(wettbasisClub);
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

	public WettbasisClubObject getWettbasisClub() {
		return wettbasisClub;
	}

	public void setWettbasisClub(WettbasisClubObject wettbasisClub) {
		this.wettbasisClub = wettbasisClub;
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
			if (json.has("wettbasis")) {
				result.setWettbasisClub(WettbasisClubObject.fromJSON(json.getJSONObject("wettbasis")));
			}
			if (json.has("transfermarktClub")) {
				result.setTransfermarktClub(json.getJSONObject("transfermarktClub"));
			}
		}
		return result;
	}

	/**
	 * Serialisiert Clubdaten inklusive Ligainsider- und Wettbasis-Mapping.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("inLiga", inLiga);
		if (ligainsiderClub != null) {
			json.put("ligainsider", ligainsiderClub.toJSON());
		}
		if (wettbasisClub != null) {
			json.put("wettbasis", wettbasisClub.toJSON());
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