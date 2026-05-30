package comunio.nas.objects.user;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class User {

    private String id;
    private String name;
    private String firstName;
    private String lastName;
    private int teamCount;
    private String teamType;
    private int points; // entspricht "punkte" im JSON
    private String tactic;
    private int teamCountLinedup;
    private long guthaben;
    private long teamValue;

    // Zusätzliche Felder aus JSON
    private Map<Integer, Integer> punkteHistorie; // z.B. "punkteHistorie":{"1":30,"2":32}
    private String mitgliedSeit; // entspricht "mitgliedSeit"
    private int rank;
    private String lastPoints; // entspricht "lastPoints"

    // komplexes Feld formation (einfache Repräsentation)
    private Formation formation;

    // Untergeordnetes Objekt "user"
    private UserInfo userInfo;

    // Weitere optionale Felder aus ursprünglicher Klasse
    private boolean isGuest;
    private boolean moderator;
    private String registered;
    private String lastAction;
    private String email;
    private String becamePremium;
    private boolean isLeader;
    private boolean accountCancelled;

    // Konstruktor
    public User() {
        this.punkteHistorie = new HashMap<>();
    }

    // --- Getter und Setter (gekürzt für Übersicht, erweitern wie nötig) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getTactic() { return tactic; }
    public void setTactic(String tactic) { this.tactic = tactic; }

    public int getTeamCountLinedup() { return teamCountLinedup; }
    public void setTeamCountLinedup(int teamCountLinedup) { this.teamCountLinedup = teamCountLinedup; }

    public long getGuthaben() { return guthaben; }
    public void setGuthaben(long guthaben) { this.guthaben = guthaben; }

    public long getTeamValue() { return teamValue; }
    public void setTeamValue(long teamValue) { this.teamValue = teamValue; }

    public Map<Integer, Integer> getPunkteHistorie() { return punkteHistorie; }
    public void setPunkteHistorie(Map<Integer, Integer> punkteHistorie) { this.punkteHistorie = punkteHistorie; }

    public String getMitgliedSeit() { return mitgliedSeit; }
    public void setMitgliedSeit(String mitgliedSeit) { this.mitgliedSeit = mitgliedSeit; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getLastPoints() { return lastPoints; }
    public void setLastPoints(String lastPoints) { this.lastPoints = lastPoints; }

    public Formation getFormation() { return formation; }
    public void setFormation(Formation formation) { this.formation = formation; }

    public UserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

    // optionale Felder
    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }

    public boolean isModerator() { return moderator; }
    public void setModerator(boolean moderator) { this.moderator = moderator; }

    public String getRegistered() { return registered; }
    public void setRegistered(String registered) { this.registered = registered; }

    public String getLastAction() { return lastAction; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBecamePremium() { return becamePremium; }
    public void setBecamePremium(String becamePremium) { this.becamePremium = becamePremium; }

    public boolean isLeader() { return isLeader; }
    public void setLeader(boolean leader) { isLeader = leader; }

    public boolean isAccountCancelled() { return accountCancelled; }
    public void setAccountCancelled(boolean accountCancelled) { this.accountCancelled = accountCancelled; }

    /**
     * Updates die User-Instanz anhand eines JSONObjects.
     * Nutzt opt* Methoden mit Defaultwerten, wandelt Strings wo nötig.
     */
    public void updateFromJson(JSONObject obj) {
        if (obj == null) return;

        // Direkt aus oberstem Objekt lesen
        String idFromRoot = obj.optString("id", null);
        String nameFromRoot = obj.optString("name", null);
        String firstNameFromRoot = obj.optString("firstName", null);
        String lastNameFromRoot = obj.optString("lastName", null);

        // Nested user-Objekt (optional)
        JSONObject userObj = obj.has("user") ? obj.optJSONObject("user") : null;

        // Aus user Objekt fallback holen, falls root Werte fehlen
        String idFromUser = userObj != null ? userObj.optString("id", null) : null;
        String nameFromUser = userObj != null ? userObj.optString("name", null) : null;
        String firstNameFromUser = userObj != null ? userObj.optString("firstName", null) : null;
        String lastNameFromUser = userObj != null ? userObj.optString("lastName", null) : null;
        String emailFromUser = userObj != null ? userObj.optString("email", null) : null;

        // Setze Werte, priorisiere Root-Objekt, sonst user-Objekt
        this.setId(idFromRoot != null && !idFromRoot.isEmpty() ? idFromRoot : idFromUser);
        this.setName(nameFromRoot != null && !nameFromRoot.isEmpty() ? nameFromRoot : nameFromUser);
        this.setFirstName(firstNameFromRoot != null && !firstNameFromRoot.isEmpty() ? firstNameFromRoot : firstNameFromUser);
        this.setLastName(lastNameFromRoot != null && !lastNameFromRoot.isEmpty() ? lastNameFromRoot : lastNameFromUser);
        this.setEmail(emailFromUser != null ? emailFromUser : this.email); // email nur aus userObj wenn vorhanden

        // Andere Felder direkt aus Root-Objekt wie vorher
        this.setTeamCount(parseIntSafe(obj.optString("teamCount", String.valueOf(this.teamCount))));
        this.setTeamType(obj.optString("teamType", this.teamType));
        this.setPoints(parseIntSafe(obj.optString("punkte", String.valueOf(this.points))));
        this.setTactic(obj.optString("tactic", this.tactic));
        this.setTeamCountLinedup(parseIntSafe(obj.optString("teamCountLinedup", String.valueOf(this.teamCountLinedup))));
        this.setGuthaben(parseLongSafe(obj.optString("guthaben", String.valueOf(this.guthaben))));
        this.setTeamValue(parseLongSafe(obj.optString("teamValue", String.valueOf(this.teamValue))));

        this.setGuest(obj.optBoolean("isGuest", this.isGuest));
        this.setModerator(obj.optBoolean("moderator", this.moderator));
        this.setRegistered(obj.optString("registered", this.registered));
        this.setLastAction(obj.optString("lastAction", this.lastAction));
        this.setBecamePremium(obj.optString("becamePremium", this.becamePremium));
        this.setLeader(obj.optBoolean("isLeader", this.isLeader));
        this.setAccountCancelled(obj.optBoolean("accountCancelled", this.accountCancelled));

        this.setMitgliedSeit(obj.optString("mitgliedSeit", this.mitgliedSeit));
        this.setRank(parseIntSafe(obj.optString("rank", String.valueOf(this.rank))));
        this.setLastPoints(obj.optString("lastPoints", this.lastPoints));

        // punkteHistorie parsen
        if (obj.has("punkteHistorie")) {
            JSONObject punkteHistObj = obj.optJSONObject("punkteHistorie");
            if (punkteHistObj != null) {
                Map<Integer, Integer> tempMap = new HashMap<>();
                Iterator<String> keys = punkteHistObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    int runde = parseIntSafe(key);
                    int wert = punkteHistObj.optInt(key, 0);
                    tempMap.put(runde, wert);
                }
                this.setPunkteHistorie(tempMap);
            }
        }

        // Formation parsen
        if (obj.has("formation")) {
            JSONObject formObj = obj.optJSONObject("formation");
            if (formObj != null) {
                Formation formation = new Formation();
                formation.updateFromJson(formObj);
                this.setFormation(formation);
            }
        }

        // UserInfo nur als Hilfsobjekt mit ganzen Daten erzeugen, falls benötigt
        if (userObj != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.updateFromJson(userObj);
            this.setUserInfo(userInfo);
        }
    }


    /**
     * Hilfsmethode zum sicheren Parsen von int aus String mit Default 0.
     */
    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Hilfsmethode zum sicheren Parsen von long aus String mit Default 0.
     */
    private static long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0L;
        }
    }

    /**
     * Serialisiert die User-Instanz als JSONObject.
     */
    /**
     * Serialisiert die User-Instanz als JSONObject im gewünschten verschachtelten Format.
     */
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        // Erste Ebene mit den zusätzlichen Feldern laut Beispiel
        obj.put("punkte", points);

        if (punkteHistorie != null) {
            JSONObject punkteHistObj = new JSONObject();
            for (Map.Entry<Integer, Integer> entry : punkteHistorie.entrySet()) {
                punkteHistObj.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            obj.put("punkteHistorie", punkteHistObj);
        }

        obj.put("mitgliedSeit", mitgliedSeit);
        obj.put("guthaben", guthaben);
        obj.put("rank", rank);

        if (formation != null) {
            obj.put("formation", formation.toJson());
        }

        obj.put("teamValue", teamValue);
        obj.put("lastPoints", lastPoints);
        obj.put("tactic", tactic);

        // Verschachteltes User-Objekt
        if (userInfo != null) {
            obj.put("user", userInfo.toJson());
        } else {
            // Falls userInfo nicht gesetzt ist, können wir ein minimales User-Objekt aus den Feldern oben bauen
            JSONObject userObj = new JSONObject();
            userObj.put("id", id);
            userObj.put("name", name);
            userObj.put("firstName", firstName);
            userObj.put("lastName", lastName);
            userObj.put("email", email != null ? email : "");
            // Optional weitere Felder leer oder mit Default?
            obj.put("user", userObj);
        }

        // PunkteGeldKorrekturen als leeres Array mitliefern (optional)
        obj.put("punkteGeldKorrekturen", new JSONArray());

        return obj;
    }


    /**
     * Erzeugt eine User-Instanz aus einem JSONObject.
     */
    public static User fromJson(JSONObject obj) {
        User user = new User();
        user.updateFromJson(obj);
        return user;
    }


    /**
     * Beispiel-URL zum Benutzerprofil
     */
    public String getUrl() {
        return "https://www.comunio.de/api/users/" + this.id;
    }

}
