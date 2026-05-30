package comunio.nas.objects.community;

import org.json.JSONObject;

public class Community {
	
	private String id; 
	private String name;
	private String type; 
	private GroupSettings settings; 
	
	public Community() {
        this(null, null, null, new GroupSettings());
    }

    public Community(String id, String name, String type) {
        this(id, name, type, new GroupSettings());
    }

    public Community(String id, String name, String type, GroupSettings settings) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.settings = settings;
    }

    public Community(String id, String name, String type, JSONObject settings) {
        this(id, name, type, GroupSettings.fromJson(settings));
    }

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the settings
	 */
	public GroupSettings getSettings() {
		return settings;
	}

	/**
	 * @param settings the settings to set
	 */
	public void setSettings(GroupSettings settings) {
		this.settings = settings;
	}
	
	public void setSettings(JSONObject settings) {
		this.settings.updateFromJson(settings);
	}

}
