package comunio.nas.objects.user;

import org.json.JSONObject;

// === Innere Klasse zur User-Unterobjektrepräsentation ===
public class UserInfo {
    private String date;
    private String firstName;
    private String lastName;
    private String loginName;
    private String name;
    private String id;
    private int position;
    private String type;
    private String email;
    private boolean negativesBudget;

    // Getter und Setter
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isNegativesBudget() { return negativesBudget; }
    public void setNegativesBudget(boolean negativesBudget) { this.negativesBudget = negativesBudget; }

    public void updateFromJson(JSONObject obj) {
        if (obj == null) return;

        this.setDate(obj.optString("date", this.date));
        this.setFirstName(obj.optString("firstName", this.firstName));
        this.setLastName(obj.optString("lastName", this.lastName));
        this.setLoginName(obj.optString("loginName", this.loginName));
        this.setName(obj.optString("name", this.name));
        this.setId(obj.optString("id", this.id));
        this.setPosition(parseIntSafe(obj.optString("position", String.valueOf(this.position))));
        this.setType(obj.optString("type", this.type));
        this.setEmail(obj.optString("email", this.email));
        this.setNegativesBudget(obj.optBoolean("negativesBudget", this.negativesBudget));
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.put("date", date);
        obj.put("firstName", firstName);
        obj.put("lastName", lastName);
        obj.put("loginName", loginName);
        obj.put("name", name);
        obj.put("id", id);
        obj.put("position", position);
        obj.put("type", type);
        obj.put("email", email);
        obj.put("negativesBudget", negativesBudget);

        return obj;
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }
}