package comunio.nas.objects.helper;

import java.lang.reflect.Method;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonHelper {

	/**
	 * Rekonstruiert ein JSONArray im Originalformat. Jede Struktur wie im Start:
	 * {"punkte":..., "formation":..., "user":{...}, "punkteGeldKorrekturen":[]} ->
	 * Das "user" Feld wird dabei aus userMap (ID) aktualisiert, alles andere bleibt
	 * erhalten.
	 */
	public static JSONArray userMapToJsonArrayWithOriginalStructure(JSONArray originalUserDB, Map<String, JSONObject> userMap) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < originalUserDB.length(); i++) {
			JSONObject origContainer = originalUserDB.getJSONObject(i);

			// Kopiere das Container-Objekt
			JSONObject containerCopy = new JSONObject(origContainer.toString());

			// Ersetze NUR das "user"-Objekt, falls Aktualisierung vorhanden!
			JSONObject origUser = origContainer.getJSONObject("user");
			String userId = origUser.getString("id");
			if (userMap.containsKey(userId)) {
				containerCopy.put("user", userMap.get(userId));
			}
			// Ansonsten bleibt alles (punkte, formation, etc.) wie im Ursprungsfile!
			result.put(containerCopy);
		}
		return result;
	}

	/**
	 * Konvertiert eine Map<String, ?> in ein JSONObject.
	 * <p>
	 * Prüft für jeden Wert in der Map dynamisch via Reflection, ob eine
	 * {@code toJSON()}-Methode vorhanden ist. Falls ja, wird deren Rückgabewert als
	 * JSON-Wert verwendet. Falls nicht, wird der Wert direkt in das JSONObject
	 * übernommen.
	 * </p>
	 *
	 * @param inputMap Die Quell-Map mit String-Schlüsseln. Darf {@code null} oder
	 *                 leer sein.
	 * @return Ein {@link JSONObject}. Wenn {@code inputMap} null oder leer ist,
	 *         wird ein leeres JSONObject zurückgegeben.
	 */
	public static JSONObject mapToJSONObject(Map<String, ?> inputMap) {
		JSONObject jsonObject = new JSONObject();

		if (inputMap == null || inputMap.isEmpty()) {
			return jsonObject;
		}

		for (Map.Entry<String, ?> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value == null) {
				jsonObject.put(key, JSONObject.NULL);
				continue;
			}

			jsonObject.put(key, extractJsonValue(value));
		}

		return jsonObject;
	}

	/**
	 * Extrahiert die Werte einer Map<String, ?> und gibt sie als JSONArray zurück
	 * (Schlüssel werden ignoriert).
	 * <p>
	 * Prüft für jeden Wert in der Map dynamisch via Reflection, ob eine
	 * {@code toJSON()}-Methode vorhanden ist. Falls ja, wird deren Rückgabewert als
	 * JSON-Wert verwendet.
	 * </p>
	 * 
	 * @param inputMap Die Quell-Map mit String-Schlüsseln. Darf {@code null} oder
	 *                 leer sein.
	 * @return Ein {@link JSONArray}. Wenn {@code inputMap} null oder leer ist, wird
	 *         ein leeres JSONArray zurückgegeben.
	 */
	public static JSONArray mapValuesToJSONArray(Map<String, ?> inputMap) {
		JSONArray jsonArray = new JSONArray();

		if (inputMap == null || inputMap.isEmpty()) {
			return jsonArray;
		}

		for (Object value : inputMap.values()) {
			if (value == null) {
				jsonArray.put(JSONObject.NULL);
				continue;
			}

			jsonArray.put(extractJsonValue(value));
		}

		return jsonArray;
	}

	/**
	 * Hilfsmethode: Prüft via Reflection auf eine toJSON()-Methode und verarbeitet
	 * das Ergebnis.
	 */
	private static Object extractJsonValue(Object value) {
		try {
			// Prüfen, ob das Objekt eine toJSON()-Methode besitzt
			Method toJsonMethod = value.getClass().getMethod("toJSON");
			Object result = toJsonMethod.invoke(value);

			// Falls toJSON() einen String liefert, in ein org.json-Objekt umwandeln
			if (result instanceof String stringResult) {
				String trimmed = stringResult.trim();
				if (trimmed.startsWith("{")) {
					return new JSONObject(trimmed);
				} else if (trimmed.startsWith("[")) {
					return new JSONArray(trimmed);
				}
			}
			return result;

		} catch (NoSuchMethodException e) {
			// Keine toJSON()-Methode vorhanden -> Standardwert (String, Integer, Boolean,
			// etc.) nutzen
			return value;
		} catch (Exception e) {
			throw new RuntimeException("Fehler beim Aufrufen von toJSON() auf " + value.getClass().getName(), e);
		}
	}
}
