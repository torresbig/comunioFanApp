package comunio.nas.objects.user;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

// === Innere Klasse zur Repräsentation von Formation ===
public class Formation {
    private String lastSaveFormation;
    private Map<String, JSONArray> lineup = new HashMap<>();
    private String tactic;

    public Formation() {
    }

    public String getLastSaveFormation() { return lastSaveFormation; }
    public void setLastSaveFormation(String lastSaveFormation) { this.lastSaveFormation = lastSaveFormation; }

    public Map<String, JSONArray> getLineup() { return lineup; }
    public void setLineup(Map<String, JSONArray> lineup) { this.lineup = lineup; }

    public String getTactic() { return tactic; }
    public void setTactic(String tactic) { this.tactic = tactic; }

    public void updateFromJson(JSONObject obj) {
        if (obj == null) return;

        this.setLastSaveFormation(obj.optString("lastSaveFormation", this.lastSaveFormation));
        this.setTactic(obj.optString("tactic", this.tactic));

        if (obj.has("lineup")) {
            JSONObject lineupObj = obj.optJSONObject("lineup");
            if (lineupObj != null) {
                Map<String, JSONArray> map = new HashMap<>();
                Iterator<String> keys = lineupObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray arr = lineupObj.optJSONArray(key);
                    if (arr != null) {
                        map.put(key, arr);
                    }
                }
                this.setLineup(map);
            }
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("lastSaveFormation", lastSaveFormation);
        obj.put("tactic", tactic);

        JSONObject lineupObj = new JSONObject();
        if (lineup != null) {
            for (Map.Entry<String, JSONArray> entry : lineup.entrySet()) {
                lineupObj.put(entry.getKey(), entry.getValue());
            }
        }
        obj.put("lineup", lineupObj);

        return obj;
    }
}