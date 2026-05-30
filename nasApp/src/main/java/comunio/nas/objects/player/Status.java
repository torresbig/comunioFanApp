package comunio.nas.objects.player;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.enu.SpielerStatus;
import comunio.nas.objects.orga.ComunioDate;

public class Status {
	private SpielerStatus status;
	private String grund;
	private String details;
	private String seit;
	private ComunioDate lastUpdate;

	List<Status> historie;

	public Status() {
		this.status = SpielerStatus.AKTIV;
		this.details = "";
		this.grund = "";
		this.seit = "";
		this.lastUpdate = new ComunioDate();
		this.historie = new ArrayList<Status>();
	}

	public static Status fromJSON(JSONObject json) {
		Status cs = new Status();
		cs.grund = json.optString("grund", null);
		String date = json.optString("lastUpdate", null);
		if (date != null) {
			cs.lastUpdate = new ComunioDate(date);
		}

		cs.seit = json.optString("seit", null);
		cs.details = json.optString("details", null);

		JSONArray histArray = json.optJSONArray("historie");
		if (histArray != null) {
			cs.historie = new ArrayList<>();
			for (int i = 0; i < histArray.length(); i++) {
				JSONObject jo = (JSONObject) histArray.get(i);
				Status statusHist = new Status();
				statusHist.status = SpielerStatus.fromString(jo.getString("status"));
				statusHist.details = jo.getString("details");
				statusHist.grund = jo.getString("grund");
				statusHist.seit = jo.getString("seit");
				statusHist.lastUpdate = new ComunioDate(jo.getString("lastUpdate"));
				cs.historie.add(statusHist);
			}
		}
		String sta = json.optString("status", null);
		if (sta != null) {
			cs.status = SpielerStatus.fromString(sta);
		}

		return cs;
	}

	public void changeStatus(ComunioDate date, String status, String details) {
		setHistorie();
		this.setStatus(SpielerStatus.fromString(status));
		this.details = details == "" ? "unbekannt" : details;
		this.grund = "unbekannt";
		this.seit = "unbekannt";
		this.lastUpdate = date;
	}

	public void setNichtInLiga() {
		setHistorie();
		this.status = SpielerStatus.NICHT_IN_LIGA;
		this.details = "Spieler ist nicht mehr in der Bundesliga";
		this.grund = "Vereinswechsel / Karriereende";
		this.seit = "";
		this.lastUpdate = new ComunioDate();
	}

	public void setWiederGesund() {
		setHistorie();
		this.status = SpielerStatus.AKTIV;
		this.details = "";
		this.grund = "";
		this.seit = "";
		this.lastUpdate = new ComunioDate();
	}

	private void setHistorie() {
		if (!this.status.isAKTIV() && !this.status.isUNBESTIMMT()) {
			if (historie == null) {
				this.historie = new ArrayList<Status>();
			}
			Status adl = new Status();
			adl.setLastUpdate(new ComunioDate());
			adl.setStatus(status);
			adl.setDetails(details);
			adl.setGrund(grund);
			adl.setSeit(seit);
			this.historie.add(adl);
		}
	}

	/**
	 * @return the status
	 */
	public SpielerStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(SpielerStatus status) {
		this.status = status;
	}

	/**
	 * @return the grund
	 */
	public String getGrund() {
		return grund;
	}

	/**
	 * @param grund the grund to set
	 */
	public void setGrund(String grund) {
		this.grund = grund;
	}

	/**
	 * @return the details
	 */
	public String getDetails() {
		return details;
	}

	/**
	 * @param details the details to set
	 */
	public void setDetails(String details) {
		this.details = details;
	}

	/**
	 * @return the seit
	 */
	public String getSeit() {
		return seit;
	}

	/**
	 * @param seit the seit to set
	 */
	public void setSeit(String seit) {
		this.seit = seit;
	}

	/**
	 * @return the lastUpdate
	 */
	public ComunioDate getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate the lastUpdate to set
	 */
	public void setLastUpdate(ComunioDate lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * @return the historie
	 */
	public List<Status> getHistorie() {
		return historie;
	}

	/**
	 * @param historie the historie to set
	 */
	public void setHistorie(ArrayList<Status> historie) {
		this.historie = historie;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (grund != null)
			json.put("grund", grund);
		if (lastUpdate != null)
			json.put("lastUpdate", lastUpdate.toString());
		if (seit != null)
			json.put("seit", seit);
		if (details != null)
			json.put("details", details);
		if (historie != null) {
			JSONArray arr = new JSONArray();
			for (Status o : historie) {
				arr.put(o.toJSON());
			}
			json.put("historie", arr);
		}
		if (status != null)
			json.put("status", status.toString());
		return json;
	}
}
