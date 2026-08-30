package comunio.nas.error;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.ComunioDataUpdater;
import comunio.nas.dataScraper.comunio.LineupParser;
import comunio.nas.dataVariable.Urls;
import comunio.nas.git.GitHubUploader;
import comunio.nas.objects.helper.LogManager;

/**
 * Fehlercontainer, der eine (String‑)Karte mit Zeitstempeln in einer einzigen,
 * für Menschen lesbaren Form verwendet – *JJJJ‑MM‑TT*.
 *
 * <p>
 * Der JSON‑String kann zwei Schlüsselformen enthalten:
 *
 * <ul>
 * <li>alte, vollständige Formatierung wie
 * {@code Mon Aug 10 14:14:08 CEST 2026}</li>
 * <li>neue, kompakte Form wie {@code 2026-08-10}</li>
 * </ul>
 *
 * <p>
 * Der Container wandelt die alte Form automatisch in die neue um, sodass alle
 * internen Schlüssel einheitlich das Format {@code yyyy‑MM‑dd} besitzen.
 * </p>
 */
public class ErrorsContainer {
	
	private static final Logger LOGGER = LogManager.getLogger(ErrorsContainer.class);

	/*
	 * ------------------------------------------------------------------ Formate
	 * ------------------------------------------------------------------
	 */
	private static final SimpleDateFormat OLD_DATE_FORMAT = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy"); 
	private static final SimpleDateFormat NEW_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); // Zielformat

	/*
	 * ------------------------------------------------------------------ Interne
	 * Datenstruktur
	 * ------------------------------------------------------------------
	 */
	private final Map<String, Set<Error>> errors = new HashMap<>();

	/*
	 * ------------------------------------------------------------------
	 * Öffentliche API
	 * ------------------------------------------------------------------
	 */

	public ErrorsContainer() {
		super();
	}

	/** Fügt einen Fehler für den aktuellen Tag hinzu. */
	public void addError(Error error) {
		for (Set<Error> set : this.errors.values()) {
		    if (set.contains(error)) {   // `contains` nutzt `Error.equals` → nicht gewünscht!
		        return;                  // schon vorhanden → nichts tun
		    }
		}
		String key = NEW_DATE_FORMAT.format(new Date());
		this.errors.computeIfAbsent(key, k -> new HashSet<>()).add(error);
		
		LOGGER.info("Lade aktualisierte ErrorDb.json auf GitHub hoch");
		GitHubUploader.uploadToGitHub(Urls.ERROR_DB_URL, this.toJson());
	}

	/**
	 * Erstellt ein JSON‑Objekt, dessen Schlüssel das neue „JJJJ‑MM‑TT“-Format
	 * besitzen.
	 */
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		for (Map.Entry<String, Set<Error>> e : errors.entrySet()) {
			JSONArray arr = new JSONArray();
			for (Error err : e.getValue()) {
				arr.put(err.toJSON());
			}
			json.put(e.getKey(), arr);
		}
		return json;
	}

	/**
	 * Befüllt den Container aus einem JSON‑Objekt.
	 *
	 * <p>
	 * Der JSON‑String kann zwei Schlüsselformen enthalten:
	 * <ul>
	 * <li>alte, vollständige Formatierung wie
	 * {@code Mon Aug 10 14:14:08 CEST 2026}</li>
	 * <li>neue, kompakte Form wie {@code 2026-08-10}</li>
	 * </ul>
	 * In beiden Fällen wird der Schlüssel in die neue Form überführt, sodass die
	 * interne Struktur konsistent bleibt.
	 * </p>
	 */
	public void fromJson(JSONObject json) {
		for (String rawKey : json.keySet()) {

			// ----- 1. Fehler‑Objekte aus dem JSON extrahieren -----
			Set<Error> errorSet = new HashSet<>();
			Object value = json.get(rawKey);

			if (value instanceof JSONArray) {
				JSONArray arr = (JSONArray) value;
				for (int i = 0; i < arr.length(); i++) {
					Error err = new Error();
					err.fromJSON(arr.getJSONObject(i));
					errorSet.add(err);
				}
			} else if (value instanceof JSONObject) {
				Error err = new Error();
				err.fromJSON((JSONObject) value);
				errorSet.add(err);
			} // sonst: kein Fehler – wird ignoriert

			// ----- 2. Schlüssel normalisieren -----
			String normalizedKey;
			if (rawKey.matches(OLD_DATE_FORMAT.toPattern())) {
				// Altes Format → in neues umwandeln
				try {
					Date d = OLD_DATE_FORMAT.parse(rawKey);
					normalizedKey = NEW_DATE_FORMAT.format(d);
				} catch (Exception ex) {
					// Parsing‑Fehler → Schlüssel unverändert lassen (robust)
					normalizedKey = rawKey;
				}
			} else {
				// Bereits im neuen Format (oder völlig unbekannt) → unverändert
				normalizedKey = rawKey;
			}

			// ----- 3. In die Map eintragen -----
			this.errors.put(normalizedKey, errorSet);
		}
	}

	/** Liefert eine unveränderbare Sicht auf die interne Map. */
	public Map<String, Set<Error>> getErrors() {
		Map<String, Set<Error>> copy = new HashMap<>();
		for (Map.Entry<String, Set<Error>> e : errors.entrySet()) {
			copy.put(e.getKey(), Collections.unmodifiableSet(e.getValue()));
		}
		return Collections.unmodifiableMap(copy);
	}

	/** Ersetzt die komplette Fehler‑Map. */
	public void setErrors(Map<String, Set<Error>> errors) {
		this.errors.clear();
		for (Map.Entry<String, Set<Error>> e : errors.entrySet()) {
			this.errors.put(e.getKey(), new HashSet<>(e.getValue()));
		}
	}
}
