package comunio.nas.objects.player;

import org.json.JSONObject;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.orga.ComunioDate;

/**
 * Represents the current status of a player, including reason, details, start
 * date, last update timestamp, history of previous status changes, and the list
 * of data sources.
 */
public class Status {
	private SpielerStatus status;
	private String grund;
	private String details;
	private String seit;
	private ComunioDate lastUpdate;
	private String bis;
	private String quelle; 


	/**
	 * Default constructor. Initialises the status to AKTIV with empty fields,
	 * current date, and an empty history list.
	 */
	public Status() {
		this.status = SpielerStatus.AKTIV;
		this.details = "";
		this.grund = "";
		this.seit = new ComunioDate().toString();
		this.lastUpdate = new ComunioDate();
		this.setQuelle(""); 
		this.bis = "";

	}

	public Status(ComunioDate date, String status, String details, String quelle) {
		this.status = SpielerStatus.fromString(status);
		this.details = details;
		this.grund = "";
		this.seit = seit != null ? seit : date.toString();
		this.lastUpdate = new ComunioDate();
		this.setQuelle(quelle != null ? quelle : "");
		this.bis = "";
	}

	public Status(ComunioDate date, String status, String details, String grund, String seit, String bis, String quelle) {
		this.status = SpielerStatus.fromString(status);
		this.details = details;
		this.grund = grund;
		this.seit = seit != null ? seit : date.toString();
		this.bis = bis != null ? bis : "";
		this.lastUpdate = new ComunioDate();
		this.setQuelle(quelle != null ? quelle : "");
	}

	/**
	 * Creates a Status object from a JSONObject.
	 *
	 * @param json The JSONObject containing status data.
	 * @return A new Status instance populated from the JSON data.
	 */
	public static Status fromJSON(JSONObject json) {
		Status cs = new Status();
		cs.grund = json.optString("grund", null);
		String date = json.optString("lastUpdate", null);
		if (date != null) {
			cs.lastUpdate = new ComunioDate(date);
		}

		cs.seit = json.optString("seit", null);
		cs.bis = json.optString("bis", null);
		cs.details = json.optString("details", null);


		String sta = json.optString("status", null);
		if (sta != null) {
			cs.status = SpielerStatus.fromString(sta);
		}
		
		cs.setQuelle(json.optString("quelle", null));

		return cs;
	}


	public void setNichtInLiga() {
		this.status = SpielerStatus.NICHT_IN_LIGA;
		this.details = "Spieler ist nicht mehr in der Bundesliga";
		this.grund = "Vereinswechsel / Karriereende";
		this.lastUpdate = new ComunioDate();
		this.seit = this.lastUpdate.toString();
		this.setQuelle("comunio"); 
	}

	public void setWiederGesund(String quelle) {
		this.status = SpielerStatus.AKTIV;
		this.details = "Spieler ist fit";
		this.grund = "";
		this.lastUpdate = new ComunioDate();
		this.seit = this.lastUpdate.toString();
		this.setQuelle(quelle != null ? quelle : "Unbekannt");
	}

	

	// ======================= GETTER/SETTER ===============================

	public SpielerStatus getStatus() {
		return status;
	}

	public void setStatus(SpielerStatus status) {
		this.status = status;
	}

	public String getGrund() {
		return grund;
	}

	public void setGrund(String grund) {
		this.grund = grund;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getSeit() {
		return seit;
	}

	public void setSeit(String seit) {
		this.seit = seit;
	}

	public String getBis() {
		return bis;
	}

	public void setBis(String bis) {
		this.bis = bis;
	}

	public ComunioDate getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(ComunioDate lastUpdate) {
		this.lastUpdate = lastUpdate;
	}


	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (grund != null)
			json.put("grund", grund);
		if (lastUpdate != null)
			json.put("lastUpdate", lastUpdate.toString());
		if (seit != null)
			json.put("seit", seit);
		if (bis != null)
			json.put("bis", bis);
		if (details != null)
			json.put("details", details);
		if (status != null)
			json.put("status", status.toString());
		if (getQuelle() != null)
			json.put("quelle", getQuelle());
		return json;
	}

	public String getQuelle() {
		return quelle;
	}

	public void setQuelle(String quelle) {
		this.quelle = quelle;
	}
}