package comunio.nas.objects.player;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import comunio.nas.objects.helper.LogManager;

/**
 * Hilfsklasse für Operationen auf JSON-Objekten von Spielern.
 * 
 * Unterstützt dynamisches Löschen von Keys und Array-Elementen über einen
 * Pfadstring wie z. B.:
 * 
 * <ul>
 * <li>"data.attribute.SonstigeInfos" → löscht ein Feld im JSONObject</li>
 * <li>"data.possibleNames.1" → löscht Element mit Index 1 aus einem Array</li>
 * <li>"data.historicalPoints.0.2023" → löscht den Key "2023" in einem Objekt im
 * Array</li>
 * </ul>
 * 
 * Pfadangabe: - Trenner = Punkt `.` - Bei Arrays muss der Index als Zahl
 * angegeben werden.
 */

public class PlayerJSONHelper {

	private static final Logger LOGGER = LogManager.getLogger(PlayerJSONHelper.class);

	/**
	 * Löscht dynamisch ein Feld oder Array-Element aus einem JSON-Objekt anhand
	 * eines Pfades.
	 *
	 * @param player JSON-Objekt des Spielers
	 * @param path   Pfadangabe (z. B. "data.attribute.SonstigeInfos")
	 */
	public static void deleteJsonObjectFromPlayer(JSONObject player, String path) {
		if (player == null || path == null || path.isBlank()) {
			LOGGER.warning("Ungültiger Aufruf: Parameter player oder path ist null/leer!");
			return;
		}

		String[] keys = path.split("\\.");
		Object current = player;

		// Durchlaufe alle Keys bis zum vorletzten
		for (int i = 0; i < keys.length - 1; i++) {
			String key = keys[i];
			if (current instanceof JSONObject obj) {
				current = obj.opt(key);
			} else if (current instanceof JSONArray arr) {
				try {
					int idx = Integer.parseInt(key);
					current = arr.opt(idx);
				} catch (NumberFormatException e) {
					LOGGER.log(Level.SEVERE, () -> String.format("Pfad ungültig: Key '%s' ist kein gültiger Array-Index im Pfad '%s'", key, path));
					return;
				}
			}

			if (current == null) {
				LOGGER.warning(String.format("Pfad '%s' konnte nicht komplett aufgelöst werden. Key '%s' nicht gefunden.", path, key));
				return;
			}
		}

		// Letztes Element löschen
		String lastKey = keys[keys.length - 1];
		if (current instanceof JSONObject obj) {
			if (obj.has(lastKey)) {
				obj.remove(lastKey);
				// LOGGER.info(() -> String.format("Key '%s' erfolgreich in Pfad '%s' gelöscht.", lastKey, path));
			} else {
				LOGGER.fine(() -> String.format("Key '%s' in Pfad '%s' existiert nicht. Keine Änderung.", lastKey, path));
			}
		} else if (current instanceof JSONArray arr) {
			try {
				int idx = Integer.parseInt(lastKey);
				if (idx >= 0 && idx < arr.length()) {
					arr.remove(idx);
					LOGGER.info(() -> String.format("Element an Index [%d] erfolgreich in Pfad '%s' gelöscht.", idx, path));
				} else {
					LOGGER.warning(() -> String.format("Index [%d] außerhalb des gültigen Bereichs für Pfad '%s'.", idx, path));
				}
			} catch (NumberFormatException e) {
				LOGGER.log(Level.SEVERE, () -> String.format("Letzter Key '%s' ist kein gültiger Array-Index für Pfad '%s'.", lastKey, path));
			}
		} else {
			LOGGER.severe(() -> String.format("Letztes Element in Pfad '%s' ist weder JSONObject noch JSONArray. Keine Löschung möglich.", path));
		}
	}

	/**
	 * Prüft, ob ein bestimmter Pfad in einem JSONObject existiert.
	 *
	 * @param player JSON-Objekt des Spielers
	 * @param path   Pfadangabe (z. B. "data.attribute.SonstigeInfos")
	 * @return true, wenn der Pfad existiert, false sonst
	 */
	public static boolean hasPath(JSONObject player, String path) {
		if (player == null || path == null || path.isBlank()) {
			return false;
		}

		String[] keys = path.split("\\.");
		Object current = player;

		for (String key : keys) {
			if (current instanceof JSONObject obj) {
				if (!obj.has(key)) {
					return false;
				}
				current = obj.get(key);
			} else if (current instanceof JSONArray arr) {
				try {
					int idx = Integer.parseInt(key);
					if (idx < 0 || idx >= arr.length()) {
						return false;
					}
					current = arr.get(idx);
				} catch (NumberFormatException e) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Holt einen Wert aus einem JSONObject anhand eines Pfades.
	 *
	 * @param player JSON-Objekt des Spielers
	 * @param path   Pfadangabe (z. B. "data.wert")
	 * @return Der Wert am angegebenen Pfad oder null, wenn nicht gefunden
	 */
	public static Object getValueByPath(JSONObject player, String path) {
		if (player == null || path == null || path.isBlank()) {
			return null;
		}

		String[] keys = path.split("\\.");
		Object current = player;

		for (String key : keys) {
			if (current instanceof JSONObject obj) {
				current = obj.opt(key);
			} else if (current instanceof JSONArray arr) {
				try {
					int idx = Integer.parseInt(key);
					current = arr.opt(idx);
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}

			if (current == null) {
				return null;
			}
		}
		return current;
	}

	/**
	 * Setzt einen Wert in einem JSONObject anhand eines Pfades. Erstellt fehlende
	 * Zwischenobjekte automatisch.
	 *
	 * @param player JSON-Objekt des Spielers
	 * @param path   Pfadangabe (z. B. "data.attribute.newAttribute")
	 * @param value  Der zu setzende Wert
	 */
	public static void setValueByPath(JSONObject player, String path, Object value) {
		if (player == null || path == null || path.isBlank()) {
			LOGGER.warning("Ungültiger Aufruf: Parameter player oder path ist null/leer!");
			return;
		}

		String[] keys = path.split("\\.");
		JSONObject current = player;

		// Navigiere bis zum vorletzten Key und erstelle fehlende Objekte
		for (int i = 0; i < keys.length - 1; i++) {
			String key = keys[i];

			if (!current.has(key)) {
				current.put(key, new JSONObject());
			}

			Object next = current.get(key);
			if (next instanceof JSONObject) {
				current = (JSONObject) next;
			} else {
				LOGGER.warning(() -> String.format("Pfad '%s' kann nicht gesetzt werden. Key '%s' ist kein JSONObject.", path, key));
				return;
			}
		}

		// Setze den finalen Wert
		String finalKey = keys[keys.length - 1];
		current.put(finalKey, value);

		LOGGER.fine(() -> String.format("Wert an Pfad '%s' erfolgreich gesetzt.", path));
	}
	
	/**
	 * Hilfsmethode: Holt einen Integer-Wert aus einem Spieler-Feld.
	 */
	public static int getPlayerFieldValue(JSONObject player, String fieldPath) {
		try {
			JSONObject data = player.getJSONObject("data");
			return data.optInt(fieldPath, 0);
		} catch (Exception e) {
			return 0;
		}
	}
}