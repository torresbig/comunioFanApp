package comunio.nas.objects;

import comunio.nas.enu.NewsArt;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class News {
	private NewsArt art;
	private ComunioDate date;
	private String text;
	private boolean gelesen;
	private String playerId;
	private String id;

	private static final Logger LOGGER = LogManager.getLogger(News.class);

	public News(NewsArt art, String text, String playerId, ComunioDate date, String id) {
		this.art = art;
		this.text = text;
		this.playerId = playerId;
		this.date = date;
		this.gelesen = false;
		this.setId(id);
	}

	public News(NewsArt art, String text, String playerId, String id) {
		this(art, text, playerId, new ComunioDate(), id);
	}
	public News(NewsArt art, String text, String playerId) {
		this(art, text, playerId, new ComunioDate(), "0");
	}

	public News(NewsArt art, String text) {
		this(art, text, "0", "0");
	}

	// JSON-Konvertierung
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("art", art.name());
		json.put("date", date.toString());
		json.put("text", text);
		json.put("gelesen", gelesen);
		json.put("playerId", playerId);
		json.put("id", getId());
		return json;
	}

	public static News fromJson(JSONObject json) {
		NewsArt art;
		if (!json.has("art")) {
			LOGGER.warning("Warnung: News-Objekt ohne Feld 'art': " + json.toString());
			art = NewsArt.UNBESTIMMT;
		} else {
			try {
				art = NewsArt.valueOf(json.getString("art"));
			} catch (Exception e) {
				LOGGER.warning("Warnung: Unbekannter art-Wert: " + json.getString("art"));
				art = NewsArt.UNBESTIMMT;
			}
		}

		String text = json.optString("text", "");
		String playerId = json.optString("playerId", "0");
		ComunioDate date = new ComunioDate(json.optString("date", ""));
		String id = json.optString("id", "0");
		News news = new News(art, text, playerId, date, id);
		news.setGelesen(json.optBoolean("gelesen", false));
		return news;
	}

	// Getter und Setter
	public NewsArt getArt() {
		return art;
	}

	public void setArt(NewsArt art) {
		this.art = art;
	}

	public ComunioDate getDate() {
		return date;
	}

	public void setDate(ComunioDate date) {
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isGelesen() {
		return gelesen;
	}

	public void setGelesen(boolean gelesen) {
		this.gelesen = gelesen;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId != null ? playerId : "0";
	}

	public String toString() {
		return this.art + " | " + this.text + " - " + this.date.toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		News news = (News) obj;

		// Gleiche Art prüfen
		if (art != news.art)
			return false;

		// Nur bei TRANSFER-News spezielle Logik (sonst auf andere Felder prüfen)
		if (art == NewsArt.TRANSFER) {
			JSONObject thisJson = new JSONObject(this.text);
			JSONObject otherJson = new JSONObject(news.text);

			// Vergleiche nur relevante Felder
			return Objects.equals(thisJson.optString("seller"), otherJson.optString("seller")) && Objects.equals(thisJson.optString("buyer"), otherJson.optString("buyer")) && Objects.equals(thisJson.optString("playerName"), otherJson.optString("playerName")) && (thisJson.optInt("price") == otherJson.optInt("price")) && Objects.equals(playerId, news.playerId) && Objects.equals(date, news.date);
			// Preis und playerValue werden ignoriert
		}

		// Für nicht-TRANSFER-News (z.B. Verletzungen)
		return Objects.equals(text, news.text) && Objects.equals(playerId, news.playerId) && Objects.equals(date, news.date);
	}

	@Override
	public int hashCode() {
		if (art == NewsArt.TRANSFER) {
			JSONObject thisJson = new JSONObject(this.text);
			return Objects.hash(art, thisJson.optString("seller"), thisJson.optString("buyer"), thisJson.optString("playerName"), thisJson.optString("playerId"), thisJson.optInt("price"), playerId, date);
		}
		return Objects.hash(art, text, playerId, date);
	}

	public static News getVereinswechsel(String oldClubID, String newClubID, String playerID, String playerName) {
		JSONObject textJ = new JSONObject();
		textJ.put("playerID", playerID);
		textJ.put("playerName", playerName);
		textJ.put("oldClub", oldClubID);
		textJ.put("newClub", newClubID);
		return new News(NewsArt.VEREINSWECHSEL, textJ.toString(), playerID, "0");
	}

	public static News getPositionswechsel(String oldPos, String newPos, String playerID, String playerName) {

//		new News(NewsArt.POSITIONSWECHSEL, "Positionswechsel: " + apiPlayer.getString("name") + " (" + oldPosition + " → " + newPosition.toString() + ")", playerId), true);
		JSONObject textJ = new JSONObject();
		textJ.put("playerID", playerID);
		textJ.put("playerName", playerName);
		textJ.put("newPos", newPos);
		textJ.put("oldPos", oldPos);

		return new News(NewsArt.POSITIONSWECHSEL, textJ.toString(), playerID, "0");
	}

	public static News getTransfer(String sellerId, String sellerName, String buyerId, String buyerName, int price, int wert, String playerId, String playerName, ComunioDate cDate, String id) {

		JSONObject newsText = new JSONObject();
		newsText.put("playerName", playerName);
		newsText.put("playerId", playerId);
		newsText.put("playerValue", wert);
		newsText.put("seller", sellerName);
		newsText.put("buyer", buyerName);
		newsText.put("sellerId", sellerId);
		newsText.put("buyerId", buyerId);
		newsText.put("price", price);

		String text = newsText.toString();
		News news = new News(NewsArt.TRANSFER, text, playerId, cDate, id);

		return news;
	}

	public static News getOwnerwechsel(String oldOwner, String newOwner, int wert, String playerID, String playerName) {
		JSONObject newsText = new JSONObject();
		newsText.put("playerName", playerName);
		newsText.put("playerId", playerID);
		newsText.put("playerValue", wert);
		newsText.put("buyer", newOwner);

		if (oldOwner == null || oldOwner.isBlank() || oldOwner.equals("")) {
			newsText.put("info", "neuer Spieler. war noch nicht in der Liste drin!");
			newsText.put("seller", oldOwner);
		}

		String text = newsText.toString();
		News news = new News(NewsArt.OWNERCHANGE, text, playerID, new ComunioDate(), "0");
		return news;
	}

	public static News getUserpoints(String userName, String userID, int gamedayPoints, int totalPoints) {
		JSONObject textJ = new JSONObject();
		textJ.put("userName", userName);
		textJ.put("userID", userID);
		textJ.put("gamedayPoints", gamedayPoints);
		textJ.put("totalPoints", totalPoints);
		return new News(NewsArt.USERPOINTS, textJ.toString());
	}

	public static News get11desTages(List<JSONObject> playerList, String newsId, ComunioDate newsDate) {
		JSONArray arr = new JSONArray();
		for (JSONObject json : playerList) {
			arr.put(json);
		}
		return new News(NewsArt.ELFDESTAGES, arr.toString(), "0", newsDate, newsId);
	}



}