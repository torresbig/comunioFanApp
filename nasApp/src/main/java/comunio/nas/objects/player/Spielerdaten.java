package comunio.nas.objects.player;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.enu.PositionDetail;

public class Spielerdaten implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Calendar geburtstag;
	private String nationalitaet;
	private String groesse;
	private String hauptposition;
	private List<String> nebenpositionen;
	private String nationalspieler;
	private Integer laenderspielTore;
	private Integer laenderspiele;
	private String fuss;
	private Integer trikotNummer;
	private ComunioDate lastUpdate;

	public Spielerdaten() {
		this.geburtstag = null;
		this.nationalitaet = "";
		this.groesse = "";
		this.hauptposition = "";
		this.nebenpositionen = new ArrayList<String>();
		this.nationalspieler = "";
		this.setLaenderspielTore(0);
		this.laenderspiele = 0;
		this.lastUpdate = null;
		this.setTrikotNummer(0);
		this.setFuss("");
	}

	public static Spielerdaten fromJSON(JSONObject json) {
		Spielerdaten sd = new Spielerdaten();
		sd.groesse = json.optString("groesse", null);
		String geb = json.optString("geburtstag", null);
		if (geb != null) {
			sd.setGeburtstag(geb);
		}

		sd.laenderspiele = json.has("laenderspiele") ? json.optInt("laenderspiele") : null;
		String lastU = json.optString("lastUpdate", null);
		if (lastU != null && !lastU.isBlank()) {
			sd.lastUpdate = new ComunioDate(lastU);
		}

		JSONArray nebenArray = json.optJSONArray("nebenpositionen");
		if (nebenArray != null) {
			sd.nebenpositionen = new ArrayList<>();
			for (int i = 0; i < nebenArray.length(); i++) {
				sd.nebenpositionen.add(nebenArray.optString(i));
			}
		}

		sd.nationalspieler = json.optString("nationalspieler", null);
		sd.trikotNummer = json.has("trikotNummer") ? json.optInt("trikotNummer") : null;
		sd.nationalitaet = json.optString("nationalitaet", null);
		sd.laenderspielTore = json.has("laenderspielTore") ? json.optInt("laenderspielTore") : null;
		sd.hauptposition = json.optString("hauptposition", null);
		sd.fuss = json.optString("fuss", null);

		return sd;
	}

	public Calendar getGeburtstag() {
		return geburtstag;
	}

	public String getGeburtstagString() {
		String result = "N/A";
		if (this.geburtstag != null) {
			try {
				result = new SimpleDateFormat("dd.MM.yyyy").format(this.geburtstag.getTime());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Format Fehler? " + this.geburtstag);
			}

		}
		return result;
	}

	public int getAlter() {
		int alter = 0;
		if (this.geburtstag != null) {
			Calendar now = Calendar.getInstance();
			// anderes Datum liegt vor Geburtsdatum
			if (now.before(this.getGeburtstagString()))
				return -1;

			// Jahresunterschied berechnen
			alter = now.get(Calendar.YEAR) - this.geburtstag.get(Calendar.YEAR);

			// Prüfen, ob Tag in otherDate vor Tag in birthdate liegt. Wenn ja, Alter um 1
			// Jahr vermindern
			if ((now.get(Calendar.MONTH) < this.geburtstag.get(Calendar.MONTH)) || (now.get(Calendar.MONTH) == this.geburtstag.get(Calendar.MONTH) && now.get(Calendar.DAY_OF_MONTH) < this.geburtstag.get(Calendar.DAY_OF_MONTH))) {
				--alter;
			}
		}
		return alter;

	}

	public void setGeburtstag(Calendar geburtstag) {
		this.geburtstag = geburtstag;
	}

	/**
	 * Geburtstag als String im Format dd.MM.yyyy
	 * 
	 * @param geburtstag
	 */
	public void setGeburtstag(String geburtstag) {
		if (geburtstag != null) {
			if (!geburtstag.isEmpty() && !geburtstag.equals("N/A")) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
				try {
					cal.setTime(sdf.parse(geburtstag));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.setGeburtstag(cal);
			}
		}
	}

	public String getNationalitaet() {
		if (this.nationalitaet == null) {
			return "N/A";
		} else {
			return nationalitaet;
		}
	}

	public void setNationalitaet(String nationalitaet) {
		this.nationalitaet = nationalitaet;
	}

	public String getGroesse() {

		if (this.groesse == null) {
			return "N/A";
		} else {
			return groesse;
		}

	}

	public void setGroesse(String groesse) {
		this.groesse = groesse;
	}

	public String getHauptposition() {
		if (this.hauptposition == null) {
			return "N/A";
		} else {
			return hauptposition;
		}

	}

	public void setHauptposition(String hauptposition) {
		this.hauptposition = hauptposition;
	}

	public List<String> getNebenpositionen() {
		if (this.nebenpositionen == null) {
			this.nebenpositionen = new ArrayList<String>();
		}
		return this.nebenpositionen;
	}

	public void setNebenpositionen(List<?> nebenpositionen) {
		if (nebenpositionen != null) {
			if (!nebenpositionen.isEmpty()) {
				this.nebenpositionen.clear();
				for (Object object : nebenpositionen) {
					if (object instanceof String) {
						String strPos = (String) object;
						this.nebenpositionen.add(strPos);
					}
					if (object instanceof PositionDetail) {
						PositionDetail enuPos = (PositionDetail) object;
						this.nebenpositionen.add(enuPos.toString());
					}
				}
			}
		}
	}

	public boolean isNationalspieler() {
		boolean result = false;
		if (this.nationalspieler == null) {
			result = false;
		} else {
			if (nationalspieler != "") {
				result = true;
			}
		}
		return result;

	}

	public String getNationalspieler() {
		if (this.nationalspieler == null) {
			this.nationalspieler = "";
		}
		return this.nationalspieler;
	}

	public void setNationalspieler(String nationalspieler) {
		this.nationalspieler = nationalspieler;
	}

	public int getLaenderspiele() {
		return laenderspiele;
	}

	public void setLaenderspiele(int laenderspiele) {
		this.laenderspiele = laenderspiele;
	}

	public ComunioDate getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(ComunioDate lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public void setLastUpdate(String lastUpdate) {
		if (lastUpdate != null) {
			if (!lastUpdate.isBlank()) {
				this.setLastUpdate(new ComunioDate(lastUpdate));
			}
		}
	}

	public int getTrikotNummer() {
		return trikotNummer;
	}

	public void setTrikotNummer(int trikotNummer) {
		this.trikotNummer = trikotNummer;
	}

	public int getLaenderspielTore() {
		return laenderspielTore;
	}

	public void setLaenderspielTore(int laenderspielTore) {
		this.laenderspielTore = laenderspielTore;
	}

	public String getFuss() {
		return fuss;
	}

	public void setFuss(String fuss) {
		this.fuss = fuss;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (groesse != null)
			json.put("groesse", groesse);
		if (geburtstag != null)
			json.put("geburtstag", getGeburtstagString());
		if (laenderspiele != null)
			json.put("laenderspiele", laenderspiele);
		if (lastUpdate != null)
			json.put("lastUpdate", lastUpdate.toString());

		if (nebenpositionen != null) {
			JSONArray arr = new JSONArray();
			for (String s : nebenpositionen)
				arr.put(s);
			json.put("nebenpositionen", arr);
		}

		if (nationalspieler != null)
			json.put("nationalspieler", nationalspieler);
		if (trikotNummer != null)
			json.put("trikotNummer", trikotNummer);
		if (nationalitaet != null)
			json.put("nationalitaet", nationalitaet);
		if (laenderspielTore != null)
			json.put("laenderspielTore", laenderspielTore);
		if (hauptposition != null)
			json.put("hauptposition", hauptposition);
		if (fuss != null)
			json.put("fuss", fuss);

		return json;
	}

}
