package comunio.nas.objects.helper;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonHelper {


	 /**
     * Rekonstruiert ein JSONArray im Originalformat.
     * Jede Struktur wie im Start: {"punkte":..., "formation":..., "user":{...}, "punkteGeldKorrekturen":[]}
     * -> Das "user" Feld wird dabei aus userMap (ID) aktualisiert, alles andere bleibt erhalten.
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

}
