package comunio.nas.objects.comunioTransfermarkt;

import org.json.JSONObject;

public class PlayerWasOnMarket {

	private String id;
	private String name;
	private String date;
	private int preis;
	private int wert;

	public PlayerWasOnMarket(String id, String name, String date, int preis, int wert) {
		super();
		this.setId(id);
		this.setName(name);
		this.setDate(date);
		this.setPreis(preis);
		this.setWert(wert);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getPreis() {
		return preis;
	}

	public void setPreis(int preis) {
		this.preis = preis;
	}

	public int getWert() {
		return wert;
	}

	public void setWert(int wert) {
		this.wert = wert;
	}

	/**
	 * Erstellt ein Objekt null-sicher aus JSON.
	 */
	public static PlayerWasOnMarket fromJSON(JSONObject json) {
		if (json == null) {
			return null;
		}

		return new PlayerWasOnMarket(optNullableString(json, "id"), optNullableString(json, "name"), optNullableString(json, "date"), json.optInt("preis"), json.optInt("wert"));
	}

	/**
	 * Wandelt das Objekt in JSON um und erhält explizite Null-Werte.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id == null ? JSONObject.NULL : id);
		json.put("name", name == null ? JSONObject.NULL : name);
		json.put("date", date == null ? JSONObject.NULL : date);
		json.put("preis", preis);
		json.put("wert", wert);
		return json;
	}

	/**
	 * Liest einen String und behandelt fehlende sowie explizite Null-Werte.
	 */
	private static String optNullableString(JSONObject json, String key) {
		Object value = json.opt(key);
		return value == null || value == JSONObject.NULL ? null : value.toString();
	}

}
