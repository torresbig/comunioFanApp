package comunio.nas.objects.espn;

import org.json.JSONObject;

public class EspnPlayerMapObject {

	private String espnName;
	private String espnId;
	private String comunioId;
	private String comunioName;

	private String lastName;
	private String firstName;
	private String weight;
	private String dateOfBirth;
	private String nationality;
	private String jersey;
	private EspnClubMapObject club;
	private String position;
	private String shortName;
	private String height;
	private String status;
	private boolean mappedOnlyLastName;

	public EspnPlayerMapObject() {

	}

	/**
	 * Erstellt ein EspnPlayerMapObject aus einem JSON-Objekt.
	 *
	 * @param json Das JSON-Objekt, das die Spielerinformationen enthält.
	 * @return Ein EspnPlayerMapObject, das die Informationen aus dem JSON-Objekt
	 *         enthält.
	 */
	public static EspnPlayerMapObject fromJson(JSONObject json) {
		EspnPlayerMapObject player = new EspnPlayerMapObject();
		player.setLastName(json.optString("lastName"));
		player.setEspnId(json.optString("espnId"));
		player.setWeight(json.optString("weight"));
		player.setDateOfBirth(json.optString("dateOfBirth"));
		player.setFirstName(json.optString("firstName"));
		player.setNationality(json.optString("nationality"));
		player.setJersey(json.optString("jersey"));
		player.setName(json.has("name") ? json.optString("name") : json.optString("espnName"));
		player.setClub(EspnClubMapObject.fromJson(json.optJSONObject("club")));
		player.setPosition(json.optString("position"));
		player.setShortName(json.optString("shortName"));
		player.setHeight(json.optString("height"));
		player.setStatus(json.optString("status"));
		player.setComunioId(json.optString("comunioId"));
		player.setComunioName(json.optString("comunioName"));
		player.setMappedOnlyLastName(json.optBoolean("mappedOnlyLastName", false));
		return player;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("lastName", lastName);
		json.put("espnId", espnId);
		json.put("weight", weight);
		json.put("dateOfBirth", dateOfBirth);
		json.put("firstName", firstName);
		json.put("nationality", nationality);
		json.put("jersey", jersey);
		json.put("name", espnName);
		json.put("club", club != null ? club.toJson() : JSONObject.NULL);
		json.put("position", position);
		json.put("shortName", shortName);
		json.put("height", height);
		json.put("status", status);
		json.put("comunioId", comunioId);
		json.put("comunioName", comunioName);
		json.put("mappedOnlyLastName", mappedOnlyLastName);
		return json;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEspnId() {
		return espnId;
	}

	public void setEspnId(String espnId) {
		this.espnId = espnId;
	}

	public String getWeight() {
		return weight;
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getJersey() {
		return jersey;
	}

	public void setJersey(String jersey) {
		this.jersey = jersey;
	}

	public String getName() {
		return espnName;
	}

	public void setName(String name) {
		this.espnName = name;
	}

	public EspnClubMapObject getClub() {
		return club;
	}

	public void setClub(EspnClubMapObject club) {
		this.club = club;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * Berechnet das Alter des Spielers basierend auf dem Geburtsdatum.
	 *
	 * @return Das Alter des Spielers. Wenn das Geburtsdatum nicht verfügbar ist,
	 *         wird 0 zurückgegeben.
	 */

	public int getAge() {
		int age = 0;
		if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
			String[] parts = dateOfBirth.split("\\.");
			if (parts.length == 3) {
				int birthYear = Integer.parseInt(parts[2]);
				int currentYear = java.time.Year.now().getValue();
				age = currentYear - birthYear;
			}
		}
		return age;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getComunioId() {
		return comunioId;
	}

	public void setComunioId(String comunioId) {
		this.comunioId = comunioId;
	}

	public String getComunioName() {
		return comunioName;
	}

	public void setComunioName(String comunioName) {
		this.comunioName = comunioName;
	}

	public boolean isMappedOnlyLastName() {
		return mappedOnlyLastName;
	}

	public void setMappedOnlyLastName(boolean mappedOnlyLastName) {
		this.mappedOnlyLastName = mappedOnlyLastName;
	}

}
