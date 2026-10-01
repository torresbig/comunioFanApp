package comunio.nas.objects.comunioTransfermarkt;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Stellt einen Spieler auf dem Transfermarkt dar. Enthält alle relevanten
 * Informationen, die von der {@code Transfermarkt} -Scraper empfangen werden.
 */
public class PlayerOnMarket {
	private String id;
	private String name;
	private String date;
	private int preis;
	private String remainingDate;
	private long remainingSeconds;
	private String setOnMarket;
	private String verein;
	private int punkte;
	private String position;
	private int wert;
	private String status;
	private String ownerId; // Optionales Feld für den Besitzer des Spielers

	/** No‑arg-Konstruktor – erforderlich für JSON‑Deserialisierung. */
	public PlayerOnMarket() {
	}

	/**
	 * Vollständiger Konstruktor – ermöglicht die einfache Erstellung eines
	 * PlayerOnMarket-Objekts aus einzelnen Attributen.
	 */
	public PlayerOnMarket(String playerID, String playerName, String date, int preis, String remainingDate, long remainingSeconds, String setOnMarket, String verein, int punkte, String position, int wert, String status, String ownerId) {
		this.id = playerID;
		this.name = playerName;
		this.date = date;
		this.preis = preis;
		this.remainingDate = remainingDate;
		this.remainingSeconds = remainingSeconds;
		this.setOnMarket = setOnMarket;
		this.verein = verein;
		this.punkte = punkte;
		this.position = position;
		this.wert = wert;
		this.status = status;
		this.ownerId = ownerId;
	}

	// -----------------------------------------------------------------
	// Getter / Setter
	// -----------------------------------------------------------------
	public String getID() {
		return id;
	}

	public void setID(String playerID) {
		this.id = playerID;
	}

	public String getName() {
		return name;
	}

	public void setName(String playerName) {
		this.name = playerName;
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

	public String getRemainingDate() {
		return remainingDate;
	}

	public void setRemainingDate(String remainingDate) {
		this.remainingDate = remainingDate;
	}

	public long getRemainingSeconds() {
		return remainingSeconds;
	}

	public void setRemainingSeconds(long remainingSeconds) {
		this.remainingSeconds = remainingSeconds;
	}

	public String getSetOnMarket() {
		return setOnMarket;
	}

	public void setSetOnMarket(String setOnMarket) {
		this.setOnMarket = setOnMarket;
	}

	public String getVerein() {
		return verein;
	}

	public void setVerein(String verein) {
		this.verein = verein;
	}

	public int getPunkte() {
		return punkte;
	}

	public void setPunkte(int punkte) {
		this.punkte = punkte;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public int getWert() {
		return wert;
	}

	public void setWert(int wert) {
		this.wert = wert;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	// -----------------------------------------------------------------
	// JSON-Serialisierung / -Deserialisierung
	// -----------------------------------------------------------------
	/**
	 * Konvertiert dieses Objekt in ein {@link JSONObject}.
	 *
	 * @return JSON-Darstellung des Spielers.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("playerID", id);
		json.put("playerName", name);
		json.put("date", date);
		json.put("preis", preis);
		json.put("remainingDate", remainingDate);
		json.put("remainingSeconds", remainingSeconds);
		json.put("setOnMarket", setOnMarket);
		json.put("verein", verein);
		json.put("punkte", punkte);
		json.put("position", position);
		json.put("wert", wert);
		json.put("status", status);
		json.put("ownerId", ownerId);
		return json;
	}

	/**
	 * Erstellt ein neues {@code PlayerOnMarket}-Objekt aus einem JSON- Objekt. Alle
	 * Felder werden null-sicher extrahiert.
	 *
	 * @param json JSON, das die Spieler-Daten enthält.
	 * @return PlayerOnMarket-Objekt oder {@code null}, falls die Eingabe
	 *         {@code null} ist.
	 */
	public static PlayerOnMarket fromJSON(JSONObject json) {
		if (json == null) {
			return null;
		}

		PlayerOnMarket player = new PlayerOnMarket();

		player.setID(json.optString("playerID", null));
		player.setName(json.optString("playerName", null));
		player.setDate(json.optString("date", null));
		player.setPreis(json.optInt("preis"));
		player.setRemainingDate(json.optString("remainingDate", null));
		player.setRemainingSeconds(json.optLong("remainingSeconds"));
		player.setSetOnMarket(json.optString("setOnMarket", null));
		player.setVerein(json.optString("verein", null));
		player.setPunkte(json.optInt("punkte"));
		player.setPosition(json.optString("position", null));
		player.setWert(json.optInt("wert"));
		player.setStatus(json.optString("status", null));
		player.setOwnerId(json.optString("ownerId", null));

		return player;
	}

	// -----------------------------------------------------------------
	// Konvertierung in die Map<String, PlayerOnMarket>, die von Transfermarkt
	// erwartet wird (Beispiel ohne die umgebende Methode).
	// -----------------------------------------------------------------
	public static Map<String, PlayerOnMarket> mapFromJSONArray(JSONArray jsonArray) {
		Map<String, PlayerOnMarket> map = new LinkedHashMap<>();
		for (Object o : jsonArray) {
			JSONObject entry = (JSONObject) o;
			PlayerOnMarket player = PlayerOnMarket.fromJSON(entry);
			if (player != null) {
				map.put(player.getID(), player);
			}
		}
		return map;
	}

	/**
	 * Konvertiert eine {@code Map<String, PlayerOnMarket>} in ein
	 * {@link JSONArray}. Die Reihenfolge der Map wird beibehalten (relevant bei
	 * {@link LinkedHashMap}).
	 *
	 * @param map Map mit playerID als Key und PlayerOnMarket als Value.
	 * @return JSONArray mit den JSON-Repräsentationen der Spieler.
	 */
	public static JSONArray jsonArrayFromMap(Map<String, PlayerOnMarket> map) {
		JSONArray jsonArray = new JSONArray();
		if (map == null || map.isEmpty()) {
			return jsonArray;
		}
		for (PlayerOnMarket player : map.values()) {
			if (player != null) {
				jsonArray.put(player.toJSON());
			}
		}
		return jsonArray;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

}
