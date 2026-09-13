package comunio.nas.objects.espn;

import org.json.JSONObject;

public class EspnClubMapObject {

	private String espnName;
	private String espnId;
	private String comunioName;
	private String comunioId;

	
	/**
	 * Konstruktor für das Mapping-Objekt.
	 *
	 * @param espnName    Name des Vereins bei ESPN
	 * @param espnId      ID des Vereins bei ESPN
	 * @param comunioName Name des Vereins bei Comunio
	 * @param comunioId   ID des Vereins bei Comunio
	 */
	public EspnClubMapObject(String espnName, String espnId, String comunioName, String comunioId) {
		this.espnName = espnName;
		this.espnId = espnId;
		this.comunioName = comunioName;
		this.comunioId = comunioId;
	}

	public String getEspnName() {
		return espnName;
	}

	public void setEspnName(String espnName) {
		this.espnName = espnName;
	}

	public String getEspnId() {
		return espnId;
	}

	public void setEspnId(String espnId) {
		this.espnId = espnId;
	}

	public String getComunioName() {
		return comunioName;
	}

	public void setComunioName(String comunioName) {
		this.comunioName = comunioName;
	}

	public String getComunioId() {
		return comunioId;
	}

	public void setComunioId(String comunioId) {
		this.comunioId = comunioId;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("espnName", espnName);
		json.put("espnId", espnId);
		json.put("comunioName", comunioName);
		json.put("comunioId", comunioId);
		return json;
	}
	
	public static EspnClubMapObject fromJson(JSONObject json) {
		String espnName = json.optString("espnName", "");
		String espnId = json.optString("espnId", "");
		String comunioName = json.optString("comunioName", "");
		String comunioId = json.optString("comunioId", "");
		return new EspnClubMapObject(espnName, espnId, comunioName, comunioId);
	}

}
