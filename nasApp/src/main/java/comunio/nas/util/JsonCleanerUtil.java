package comunio.nas.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonCleanerUtil {

    /**
     * Reduziert alle Spieltag-Objekte des übergebenen JSONObject 
     * direkt im Speicher ausschließlich auf die Felder 'key' und 'value'.
     *
     * @param root Das bestehende JSONObject
     * @return Das bereinigte JSONObject
     */
    public static JSONObject reduceToKeyAndValue(JSONObject root) {
        for (String playerId : root.keySet()) {
            JSONArray matchdays = root.getJSONArray(playerId);

            
            for (int i = 0; i < matchdays.length(); i++) {
                JSONObject matchday = matchdays.getJSONObject(i);

                JSONObject cleanMatchday = new JSONObject();
                if (matchday.has("key")) {
                    cleanMatchday.put("key", matchday.get("key"));
                }
                if (matchday.has("value")) {
                    cleanMatchday.put("value", matchday.get("value"));
                }

                matchdays.put(i, cleanMatchday);
            }
        }
        return root;
    }
}
